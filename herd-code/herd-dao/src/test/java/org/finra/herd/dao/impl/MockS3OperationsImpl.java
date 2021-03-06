/*
* Copyright 2015 herd contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.finra.herd.dao.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.config.model.NoSuchBucketException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.internal.CopyImpl;
import com.amazonaws.services.s3.transfer.internal.DownloadImpl;
import com.amazonaws.services.s3.transfer.internal.MultipleFileDownloadImpl;
import com.amazonaws.services.s3.transfer.internal.MultipleFileUploadImpl;
import com.amazonaws.services.s3.transfer.internal.TransferMonitor;
import com.amazonaws.services.s3.transfer.internal.UploadImpl;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.concurrent.BasicFuture;
import org.apache.log4j.Logger;

import org.finra.herd.core.AbstractCoreTest;
import org.finra.herd.core.HerdDateUtils;
import org.finra.herd.dao.S3Operations;

/**
 * Mock implementation of AWS S3 operations. Simulates S3 by storing objects in memory.
 * <p/>
 * Some operations use a series of predefined prefixes to hint the operation to behave in a certain manner (throwing exceptions, for example).
 * <p/>
 * Some operations which either put or list objects, will NOT throw an exception even when a specified bucket do not exist. This is because some tests are
 * assuming that the bucket already exists and test may not have permissions to create test buckets during unit tests when testing against real S3.
 */
public class MockS3OperationsImpl implements S3Operations
{
    private static final Logger LOGGER = Logger.getLogger(MockS3OperationsImpl.class);

    /**
     * Suffix to hint operation to throw a AmazonServiceException
     */
    public static final String MOCK_S3_FILE_NAME_SERVICE_EXCEPTION = "mock_s3_file_name_service_exception";

    /**
     * Suffix to hint operation to throw a 404 exception.
     */
    public static final String MOCK_S3_FILE_NAME_NOT_FOUND = "mock_s3_file_name_not_found";

    /**
     * Suffix to hint operation to use object content length of 0 bytes.
     */
    public static final String MOCK_S3_FILE_NAME_0_BYTE_SIZE = "mock_s3_file_name_0_byte_size";

    /**
     * A mock KMS ID.
     */
    public static final String MOCK_KMS_ID = "mock_kms_id";

    /**
     * A mock KMS ID that will cause a failed transfer.
     */
    public static final String MOCK_KMS_ID_FAILED_TRANSFER = "mock_kms_id_failed_transfer";

    /**
     * A mock KMS ID that will cause a failed transfer with no underlying exception.
     */
    public static final String MOCK_KMS_ID_FAILED_TRANSFER_NO_EXCEPTION = "mock_kms_id_failed_transfer_no_exception";

    /**
     * A mock KMS ID that will cause a canceled transfer.
     */
    public static final String MOCK_KMS_ID_CANCELED_TRANSFER = "mock_kms_id_canceled_transfer";

    /**
     * Suffix to hint operation to throw a AmazonServiceException
     */
    public static final String MOCK_S3_BUCKET_NAME_SERVICE_EXCEPTION = "mock_s3_bucket_name_service_exception";

    /**
     * Suffix to hint multipart listing to return a truncated list.
     */
    public static final String MOCK_S3_BUCKET_NAME_TRUNCATED_MULTIPART_LISTING = "mock_s3_bucket_name_truncated_multipart_listing";

    /**
     * A bucket name which hints that method should throw a {@link NoSuchBucketException}
     */
    public static final String MOCK_S3_BUCKET_NAME_NO_SUCH_BUCKET_EXCEPTION = "MOCK_S3_BUCKET_NAME_NO_SUCH_BUCKET_EXCEPTION";

    /**
     * The description for a mock transfer.
     */
    public static final String MOCK_TRANSFER_DESCRIPTION = "MockTransfer";

    public static final String MOCK_S3_BUCKET_NAME_ACCESS_DENIED = "MOCK_S3_BUCKET_NAME_ACCESS_DENIED";

    public static final String MOCK_S3_BUCKET_NAME_INTERNAL_ERROR = "MOCK_S3_BUCKET_NAME_INTERNAL_ERROR";

    /**
     * The buckets that are available in-memory.
     */
    private Map<String, MockS3Bucket> mockS3Buckets = new HashMap<>();

    /**
     * <p>
     * Creates and returns a new {@link ObjectMetadata} with the given parameters. Content length is defaulted to 1 bytes unless a hint is provided.
     * </p>
     * <p>
     * Takes the following hints when filePath is suffixed:
     * </p>
     * <dl>
     * <p/>
     * <dt>AMAZON_THROTTLING_EXCEPTION</dt>
     * <dd>Throws AmazonServiceException with the error code "ThrottlingException"</dd>
     * <p/>
     * <dt>MOCK_S3_FILE_NAME_SERVICE_EXCEPTION</dt>
     * <dd>Throws AmazonServiceException</dd>
     * <p/>
     * <dt>MOCK_S3_FILE_NAME_NOT_FOUND</dt>
     * <dd>Throws AmazonServiceException with status code SC_NOT_FOUND</dd>
     * <p/>
     * <dt>MOCK_S3_FILE_NAME_0_BYTE_SIZE</dt>
     * <dd>Sets content length to 0 bytes</dd>
     * <p/>
     * </dl>
     */
    @Override
    public ObjectMetadata getObjectMetadata(String sourceBucketName, String filePath, AmazonS3Client s3Client)
    {
        ObjectMetadata objectMetadata = new ObjectMetadata();

        if (filePath.endsWith(MockAwsOperationsHelper.AMAZON_THROTTLING_EXCEPTION))
        {
            AmazonServiceException throttlingException = new AmazonServiceException("test throttling exception");
            throttlingException.setErrorCode("ThrottlingException");

            throw throttlingException;
        }
        else if (filePath.endsWith(MOCK_S3_FILE_NAME_SERVICE_EXCEPTION))
        {
            throw new AmazonServiceException(null);
        }
        else if (filePath.endsWith(MOCK_S3_FILE_NAME_NOT_FOUND))
        {
            AmazonServiceException exception = new AmazonServiceException(null);
            exception.setStatusCode(HttpStatus.SC_NOT_FOUND);
            throw exception;
        }
        else if (filePath.endsWith(MOCK_S3_FILE_NAME_0_BYTE_SIZE))
        {
            objectMetadata.setContentLength(AbstractCoreTest.FILE_SIZE_0_BYTE);
        }
        else
        {
            objectMetadata.setContentLength(AbstractCoreTest.FILE_SIZE_1_KB);
        }

        return objectMetadata;
    }

    /**
     * <p>
     * Simulates a copyFile operation.
     * </p>
     * <p>
     * This method does not actually copy files in-memory, but always returns a pre-defined result.
     * </p>
     * <p>
     * The result {@link Copy} has the following properties:
     * <dl>
     * <p/>
     * <dt>description</dt>
     * <dd>"MockTransfer"</dd>
     * <p/>
     * <dt>state</dt>
     * <dd>{@link TransferState#Completed}</dd>
     * <p/>
     * <dt>transferProgress.totalBytesToTransfer</dt>
     * <dd>1024</dd>
     * <p/>
     * <dt>transferProgress.updateProgress</dt>
     * <dd>1024</dd>
     * <p/>
     * </dl>
     * <p/>
     * All other properties are set as default.
     * </p>
     * <p>
     * This operation takes the following hints when suffixed in copyObjectRequest.sourceKey:
     * <dl>
     * <p/>
     * <dt>MOCK_S3_FILE_NAME_NOT_FOUND</dt>
     * <dd>Throws a AmazonServiceException</dd>
     * <p/>
     * </dl>
     * </p>
     */
    @Override
    public Copy copyFile(final CopyObjectRequest copyObjectRequest, TransferManager transferManager)
    {
        LOGGER.debug(
            "copyFile(): copyObjectRequest.getSourceBucketName() = " + copyObjectRequest.getSourceBucketName() + ", copyObjectRequest.getSourceKey() = " +
                copyObjectRequest.getSourceKey() + ", copyObjectRequest.getDestinationBucketName() = " + copyObjectRequest.getDestinationBucketName() +
                ", copyObjectRequest.getDestinationKey() = " + copyObjectRequest.getDestinationKey());

        if (copyObjectRequest.getSourceKey().endsWith(MOCK_S3_FILE_NAME_NOT_FOUND))
        {
            throw new AmazonServiceException(null);
        }
        /*
         * Does not actually copy files in memory. There is a test case S3ServiceTest.testCopyFile() which is expecting a result without staging any data.
         * Below is implementation when needed.
         */
        /*
         * String sourceBucketName = copyObjectRequest.getSourceBucketName();
         * String sourceKey = copyObjectRequest.getSourceKey();
         * MockS3Bucket mockSourceS3Bucket = mockS3Buckets.get(sourceBucketName);
         * MockS3Object mockSourceS3Object = mockSourceS3Bucket.getObjects().get(sourceKey);
         * String destinationBucketName = copyObjectRequest.getDestinationBucketName();
         * String destinationKey = copyObjectRequest.getDestinationKey();
         * ObjectMetadata objectMetadata = copyObjectRequest.getNewObjectMetadata();
         * MockS3Object mockDestinationS3Object = new MockS3Object();
         * mockDestinationS3Object.setKey(destinationKey);
         * mockDestinationS3Object.setData(Arrays.copyOf(mockSourceS3Object.getData(), mockSourceS3Object.getData().length));
         * mockDestinationS3Object.setObjectMetadata(objectMetadata);
         * MockS3Bucket mockDestinationS3Bucket = getOrCreateBucket(destinationBucketName);
         * mockDestinationS3Bucket.getObjects().put(destinationKey, mockDestinationS3Object);
         */

        // Set the result CopyImpl and TransferProgress.
        TransferProgress transferProgress = new TransferProgress();
        transferProgress.setTotalBytesToTransfer(AbstractCoreTest.FILE_SIZE_1_KB);
        transferProgress.updateProgress(AbstractCoreTest.FILE_SIZE_1_KB);
        CopyImpl copy = new CopyImpl(MOCK_TRANSFER_DESCRIPTION, transferProgress, null, null);
        copy.setState(TransferState.Completed);

        // If an invalid KMS Id was passed in, mark the transfer as failed and return an exception via the transfer monitor.
        final String kmsId = copyObjectRequest.getSSEAwsKeyManagementParams().getAwsKmsKeyId();
        if (kmsId.startsWith(MOCK_KMS_ID_FAILED_TRANSFER))
        {
            copy.setState(TransferState.Failed);
            copy.setMonitor(new TransferMonitor()
            {
                @Override
                public Future<?> getFuture()
                {
                    if (!kmsId.equals(MOCK_KMS_ID_FAILED_TRANSFER_NO_EXCEPTION))
                    {
                        throw new AmazonServiceException("Key '" + copyObjectRequest.getSSEAwsKeyManagementParams().getAwsKmsKeyId() +
                            "' does not exist (Service: Amazon S3; Status Code: 400; Error Code: KMS.NotFoundException; Request ID: 1234567890123456)");
                    }

                    // We don't want an exception to be thrown so return a basic future that won't throw an exception.
                    BasicFuture<?> future = new BasicFuture<Void>(null);
                    future.completed(null);
                    return future;
                }

                @Override
                public boolean isDone()
                {
                    return true;
                }
            });
        }
        else if (kmsId.startsWith(MOCK_KMS_ID_CANCELED_TRANSFER))
        {
            // If the KMS indicates a cancelled transfer, just update the state to canceled.
            copy.setState(TransferState.Canceled);
        }

        return copy;
    }

    /**
     * <p>
     * Deletes an object specified by the given bucket name and key. This method does nothing if the bucket does not exist.
     * </p>
     * <p>
     * This operation takes the following hints when suffixed in filePath:
     * <dl>
     * <p/>
     * <dt>MOCK_S3_FILE_NAME_NOT_FOUND</dt>
     * <dd>Throws a AmazonServiceException</dd>
     * <p/>
     * </dl>
     * </p>
     */
    @Override
    public void deleteFile(String bucketName, String key, AmazonS3Client s3Client)
    {
        LOGGER.debug("deleteFile(): bucketName = " + bucketName + ", key = " + key);

        if (key.endsWith(MOCK_S3_FILE_NAME_NOT_FOUND))
        {
            throw new AmazonServiceException(null);
        }

        MockS3Bucket mockS3Bucket = mockS3Buckets.get(bucketName);
        if (mockS3Bucket != null)
        {
            mockS3Bucket.getObjects().remove(key);
        }
    }

    /**
     * <p>
     * Returns a mock list of multipart uploads. Since a multipart upload in progress does not exist when in-memory, this method simply returns a preconfigured
     * list.
     * </p>
     * <p>
     * Returns a mock {@link MultipartUploadListing} based on the parameters and hints provided. By default returns a mock listing as defiend by
     * {@link #getMultipartUploadListing()}.
     * </p>
     * <p>
     * This operation takes the following hints when suffixed in listMultipartUploadsRequest.bucketName:
     * <dl>
     * <p/>
     * <dt>MOCK_S3_BUCKET_NAME_SERVICE_EXCEPTION</dt>
     * <dd>Throws a AmazonServiceException</dd>
     * <p/>
     * <dt>MOCK_S3_BUCKET_NAME_TRUNCATED_MULTIPART_LISTING</dt>
     * <dd>Returns the listing as if it is truncated. See below for details.</dd>
     * <p/>
     * </dl>
     * </p>
     */
    @Override
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest listMultipartUploadsRequest, AmazonS3Client s3Client)
    {
        if (listMultipartUploadsRequest.getBucketName().equals(MOCK_S3_BUCKET_NAME_SERVICE_EXCEPTION))
        {
            throw new AmazonServiceException(null);
        }
        else if (listMultipartUploadsRequest.getBucketName().equals(MOCK_S3_BUCKET_NAME_TRUNCATED_MULTIPART_LISTING))
        {
            MultipartUploadListing multipartUploadListing = getMultipartUploadListing();

            // If listing request does not have upload ID marker set, mark the listing as truncated - this is done to truncate the multipart listing just once.
            if (listMultipartUploadsRequest.getUploadIdMarker() == null)
            {
                multipartUploadListing.setUploadIdMarker("TEST_UPLOAD_MARKER_ID");
                multipartUploadListing.setTruncated(true);
            }

            return multipartUploadListing;
        }
        else
        {
            return getMultipartUploadListing();
        }
    }

    /**
     * <p>
     * Returns a mock {@link MultipartUploadListing}.
     * </p>
     * <p>
     * The return object has the following properties.
     * <dl>
     * <dt>multipartUploads</dt>
     * <dd>Length 3 list</dd>
     * <p/>
     * <dt>multipartUploads[0].initiated</dt>
     * <dd>5 minutes prior to the object creation time.</dd>
     * <p/>
     * <dt>multipartUploads[1].initiated</dt>
     * <dd>15 minutes prior to the object creation time.</dd>
     * <p/>
     * <dt>multipartUploads[2].initiated</dt>
     * <dd>20 minutes prior to the object creation time.</dd>
     * </dl>
     * <p/>
     * All other properties as set to default as defined in the by {@link MultipartUploadListing} constructor.
     * </p>
     *
     * @return a mock object
     */
    private MultipartUploadListing getMultipartUploadListing()
    {
        // Return 3 multipart uploads with 2 of them started more than 10 minutes ago.
        MultipartUploadListing multipartUploadListing = new MultipartUploadListing();
        List<MultipartUpload> multipartUploads = new ArrayList<>();
        multipartUploadListing.setMultipartUploads(multipartUploads);
        Date now = new Date();
        multipartUploads.add(getMultipartUpload(HerdDateUtils.addMinutes(now, -5)));
        multipartUploads.add(getMultipartUpload(HerdDateUtils.addMinutes(now, -15)));
        multipartUploads.add(getMultipartUpload(HerdDateUtils.addMinutes(now, -20)));
        return multipartUploadListing;
    }

    /**
     * Creates and returns a mock {@link MultipartUpload} with the given initiated timestamp.
     *
     * @param initiated - Timestamp to set to initiated.
     *
     * @return mock object
     */
    private MultipartUpload getMultipartUpload(Date initiated)
    {
        MultipartUpload multipartUpload = new MultipartUpload();
        multipartUpload.setInitiated(initiated);
        return multipartUpload;
    }

    /**
     * <p>
     * Simulates abort multipart upload operation.
     * </p>
     * <p>
     * This method does nothing.
     * </p>
     */
    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest, AmazonS3Client s3Client)
    {
    }

    /**
     * Deletes a list of objects from a bucket.
     */
    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectRequest, AmazonS3Client s3Client)
    {
        LOGGER.debug("deleteObjects(): deleteObjectRequest.getBucketName() = " + deleteObjectRequest.getBucketName() + ", deleteObjectRequest.getKeys() = " +
            deleteObjectRequest.getKeys());

        List<DeletedObject> deletedObjects = new ArrayList<>();

        MockS3Bucket mockS3Bucket = mockS3Buckets.get(deleteObjectRequest.getBucketName());

        for (KeyVersion keyVersion : deleteObjectRequest.getKeys())
        {
            String s3ObjectKey = keyVersion.getKey();

            if (mockS3Bucket.getObjects().remove(s3ObjectKey) != null)
            {
                DeletedObject deletedObject = new DeletedObject();
                deletedObject.setKey(s3ObjectKey);
                deletedObjects.add(deletedObject);
            }
        }

        return new DeleteObjectsResult(deletedObjects);
    }

    /**
     * Returns a list of objects. If the bucket does not exist, returns a listing with an empty list.
     * If a prefix is specified in listObjectsRequest, only keys starting with the prefix will be returned.
     */
    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest, AmazonS3Client s3Client)
    {
        LOGGER.debug("listObjects(): listObjectsRequest.getBucketName() = " + listObjectsRequest.getBucketName());

        String bucketName = listObjectsRequest.getBucketName();

        if (MOCK_S3_BUCKET_NAME_NO_SUCH_BUCKET_EXCEPTION.equals(bucketName))
        {
            AmazonS3Exception amazonS3Exception = new AmazonS3Exception(MOCK_S3_BUCKET_NAME_NO_SUCH_BUCKET_EXCEPTION);
            amazonS3Exception.setErrorCode("NoSuchBucket");
            throw amazonS3Exception;
        }

        ObjectListing objectListing = new ObjectListing();
        objectListing.setBucketName(bucketName);

        MockS3Bucket mockS3Bucket = mockS3Buckets.get(bucketName);
        if (mockS3Bucket != null)
        {
            for (MockS3Object mockS3Object : mockS3Bucket.getObjects().values())
            {
                String s3ObjectKey = mockS3Object.getKey();
                if (listObjectsRequest.getPrefix() == null || s3ObjectKey.startsWith(listObjectsRequest.getPrefix()))
                {
                    S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
                    s3ObjectSummary.setBucketName(bucketName);
                    s3ObjectSummary.setKey(s3ObjectKey);
                    s3ObjectSummary.setSize(mockS3Object.getData().length);

                    objectListing.getObjectSummaries().add(s3ObjectSummary);
                }
            }
        }

        return objectListing;
    }

    /**
     * Puts an object into a bucket. Creates a new bucket if the bucket does not already exist.
     *
     * @throws IllegalArgumentException when there is an error reading from input stream.
     */
    @Override
    public PutObjectResult putObject(PutObjectRequest putObjectRequest, AmazonS3Client s3Client)
    {
        LOGGER.debug("putObject(): putObjectRequest.getBucketName() = " + putObjectRequest.getBucketName() + ", putObjectRequest.getKey() = " +
            putObjectRequest.getKey());

        String s3BucketName = putObjectRequest.getBucketName();
        InputStream inputStream = putObjectRequest.getInputStream();

        ObjectMetadata metadata = putObjectRequest.getMetadata();
        if (metadata == null)
        {
            metadata = new ObjectMetadata();
        }

        File file = putObjectRequest.getFile();
        if (file != null)
        {
            try
            {
                inputStream = new FileInputStream(file);
                metadata.setContentLength(file.length());
            }
            catch (FileNotFoundException e)
            {
                throw new IllegalArgumentException("File not found " + file, e);
            }
        }
        String s3ObjectKey = putObjectRequest.getKey();

        byte[] s3ObjectData;
        try
        {
            s3ObjectData = IOUtils.toByteArray(inputStream);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Error converting input stream into byte array", e);
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                LOGGER.error("Error closing stream " + inputStream, e);
            }
        }

        MockS3Bucket mockS3Bucket = getOrCreateBucket(s3BucketName);

        MockS3Object mockS3Object = new MockS3Object();
        mockS3Object.setKey(s3ObjectKey);
        mockS3Object.setData(s3ObjectData);
        mockS3Object.setObjectMetadata(metadata);

        mockS3Bucket.getObjects().put(s3ObjectKey, mockS3Object);

        return new PutObjectResult();
    }

    /**
     * Retrieves an existing mock bucket or creates a new one and registers it if it does not exist.
     * <p/>
     * This method should only be used to retrieve mock bucket when a test assumes a bucket already exists.
     *
     * @param s3BucketName - The name of thte bucket
     *
     * @return new or existing bucket
     */
    private MockS3Bucket getOrCreateBucket(String s3BucketName)
    {
        MockS3Bucket mockS3Bucket = mockS3Buckets.get(s3BucketName);

        if (mockS3Bucket == null)
        {
            mockS3Bucket = new MockS3Bucket();
            mockS3Bucket.setName(s3BucketName);
            mockS3Buckets.put(s3BucketName, mockS3Bucket);
        }
        return mockS3Bucket;
    }

    /**
     * Uploads the contents of a directory. Optionally recurses to the sub-directories when includeSubdirectories is true.
     * <p/>
     * Delegates to {@link #uploadFileList(String, String, File, List, ObjectMetadataProvider, TransferManager)}.
     */
    @Override
    public MultipleFileUpload uploadDirectory(String bucketName, String virtualDirectoryKeyPrefix, File directory, boolean includeSubdirectories,
        ObjectMetadataProvider metadataProvider, TransferManager transferManager)
    {
        LOGGER.debug(
            "uploadDirectory(): bucketName = " + bucketName + ", virtualDirectoryKeyPrefix = " + virtualDirectoryKeyPrefix + ", directory = " + directory +
                ", includeSubdirectories = " + includeSubdirectories);

        List<File> files = new ArrayList<>();
        listFiles(directory, files, includeSubdirectories);

        return uploadFileList(bucketName, virtualDirectoryKeyPrefix, directory, files, metadataProvider, transferManager);
    }

    /**
     * Uploads a list of files.
     * <p/>
     * Delegates to {@link #putObject(PutObjectRequest, AmazonS3Client)} for each file.
     */
    @Override
    public MultipleFileUpload uploadFileList(String bucketName, String virtualDirectoryKeyPrefix, File directory, List<File> files,
        ObjectMetadataProvider metadataProvider, TransferManager transferManager)
    {
        LOGGER.debug(
            "uploadFileList(): bucketName = " + bucketName + ", virtualDirectoryKeyPrefix = " + virtualDirectoryKeyPrefix + ", directory = " + directory +
                ", files = " + files);

        String directoryPath = directory.getAbsolutePath();

        long totalFileLength = 0;
        List<Upload> subTransfers = new ArrayList<>();
        for (File file : files)
        {
            // Get path to file relative to the specified directory
            String relativeFilePath = file.getAbsolutePath().substring(directoryPath.length());

            // Replace any backslashes (i.e. Windows separator) with a forward slash.
            relativeFilePath = relativeFilePath.replace("\\", "/");

            // Remove any leading slashes
            relativeFilePath = relativeFilePath.replaceAll("^/+", "");

            long fileLength = file.length();

            // Remove any trailing slashes
            virtualDirectoryKeyPrefix = virtualDirectoryKeyPrefix.replaceAll("/+$", "");

            String s3ObjectKey = virtualDirectoryKeyPrefix + "/" + relativeFilePath;
            totalFileLength += fileLength;

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3ObjectKey, file);

            putObject(putObjectRequest, (AmazonS3Client) transferManager.getAmazonS3Client());

            subTransfers.add(new UploadImpl(null, null, null, null));
        }

        TransferProgress progress = new TransferProgress();
        progress.setTotalBytesToTransfer(totalFileLength);
        progress.updateProgress(totalFileLength);

        MultipleFileUploadImpl multipleFileUpload = new MultipleFileUploadImpl(null, progress, null, virtualDirectoryKeyPrefix, bucketName, subTransfers);
        multipleFileUpload.setState(TransferState.Completed);
        return multipleFileUpload;
    }

    /**
     * Implementation copied from {@link TransferManager#listFiles}.
     */
    private void listFiles(File dir, List<File> results, boolean includeSubDirectories)
    {
        File[] found = dir.listFiles();
        if (found != null)
        {
            for (File f : found)
            {
                if (f.isDirectory())
                {
                    if (includeSubDirectories)
                    {
                        listFiles(f, results, includeSubDirectories);
                    }
                }
                else
                {
                    results.add(f);
                }
            }
        }
    }

    /**
     * Downloads objects with the given prefix into a destination directory.
     * <p/>
     * Creates any directory that does not exist in the path to the destination directory.
     */
    @Override
    public MultipleFileDownload downloadDirectory(String bucketName, String keyPrefix, File destinationDirectory, TransferManager transferManager)
    {
        LOGGER.debug("downloadDirectory(): bucketName = " + bucketName + ", keyPrefix = " + keyPrefix + ", destinationDirectory = " + destinationDirectory);

        MockS3Bucket mockS3Bucket = mockS3Buckets.get(bucketName);

        List<Download> downloads = new ArrayList<>();
        long totalBytes = 0;

        if (mockS3Bucket != null)
        {
            for (MockS3Object mockS3Object : mockS3Bucket.getObjects().values())
            {
                if (mockS3Object.getKey().startsWith(keyPrefix))
                {
                    String filePath = destinationDirectory.getAbsolutePath() + "/" + mockS3Object.getKey();
                    File file = new File(filePath);
                    file.getParentFile().mkdirs(); // Create any directory in the path that does not exist.
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file))
                    {
                        LOGGER.debug("downloadDirectory(): Writing file " + file);
                        fileOutputStream.write(mockS3Object.getData());
                        totalBytes += mockS3Object.getData().length;
                        downloads.add(new DownloadImpl(null, null, null, null, null, new GetObjectRequest(bucketName, mockS3Object.getKey()), file));
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException("Error writing to file " + file, e);
                    }
                }
            }
        }

        TransferProgress progress = new TransferProgress();
        progress.setTotalBytesToTransfer(totalBytes);
        progress.updateProgress(totalBytes);

        MultipleFileDownloadImpl multipleFileDownload = new MultipleFileDownloadImpl(null, progress, null, keyPrefix, bucketName, downloads);
        multipleFileDownload.setState(TransferState.Completed);
        return multipleFileDownload;
    }

    /**
     * Puts an object.
     */
    @Override
    public Upload upload(PutObjectRequest putObjectRequest, TransferManager transferManager) throws AmazonServiceException, AmazonClientException
    {
        LOGGER.debug(
            "upload(): putObjectRequest.getBucketName() = " + putObjectRequest.getBucketName() + ", putObjectRequest.getKey() = " + putObjectRequest.getKey());

        putObject(putObjectRequest, (AmazonS3Client) transferManager.getAmazonS3Client());

        long contentLength = putObjectRequest.getFile().length();
        TransferProgress progress = new TransferProgress();
        progress.setTotalBytesToTransfer(contentLength);
        progress.updateProgress(contentLength);

        UploadImpl upload = new UploadImpl(null, progress, null, null);
        upload.setState(TransferState.Completed);

        return upload;
    }

    /**
     * Downloads an object.
     */
    @Override
    public Download download(String bucket, String key, File file, TransferManager transferManager)
    {
        MockS3Bucket mockS3Bucket = mockS3Buckets.get(bucket);
        MockS3Object mockS3Object = mockS3Bucket.getObjects().get(key);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file))
        {
            fileOutputStream.write(mockS3Object.getData());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error writing to file " + file, e);
        }

        TransferProgress progress = new TransferProgress();
        progress.setTotalBytesToTransfer(mockS3Object.getData().length);
        progress.updateProgress(mockS3Object.getData().length);

        DownloadImpl download = new DownloadImpl(null, progress, null, null, null, new GetObjectRequest(bucket, key), file);
        download.setState(TransferState.Completed);

        return download;
    }

    /**
     * Clears all buckets
     */
    @Override
    public void rollback()
    {
        mockS3Buckets.clear();
    }

    @Override
    public S3Object getS3Object(GetObjectRequest getObjectRequest, AmazonS3 s3)
    {
        String bucketName = getObjectRequest.getBucketName();
        String key = getObjectRequest.getKey();

        if (MOCK_S3_BUCKET_NAME_NO_SUCH_BUCKET_EXCEPTION.equals(bucketName))
        {
            AmazonServiceException amazonServiceException = new AmazonServiceException(S3Operations.ERROR_CODE_NO_SUCH_BUCKET);
            amazonServiceException.setErrorCode(S3Operations.ERROR_CODE_NO_SUCH_BUCKET);
            throw amazonServiceException;
        }

        if (MOCK_S3_BUCKET_NAME_ACCESS_DENIED.equals(bucketName))
        {
            AmazonServiceException amazonServiceException = new AmazonServiceException(S3Operations.ERROR_CODE_ACCESS_DENIED);
            amazonServiceException.setErrorCode(S3Operations.ERROR_CODE_ACCESS_DENIED);
            throw amazonServiceException;
        }

        if (MOCK_S3_BUCKET_NAME_INTERNAL_ERROR.equals(bucketName))
        {
            AmazonServiceException amazonServiceException = new AmazonServiceException(S3Operations.ERROR_CODE_INTERNAL_ERROR);
            amazonServiceException.setErrorCode(S3Operations.ERROR_CODE_INTERNAL_ERROR);
            throw amazonServiceException;
        }

        MockS3Bucket mockS3Bucket = getOrCreateBucket(bucketName);
        MockS3Object mockS3Object = mockS3Bucket.getObjects().get(key);

        if (mockS3Object == null)
        {
            AmazonServiceException amazonServiceException = new AmazonServiceException(S3Operations.ERROR_CODE_NO_SUCH_KEY);
            amazonServiceException.setErrorCode(S3Operations.ERROR_CODE_NO_SUCH_KEY);
            throw amazonServiceException;
        }

        S3Object s3Object = new S3Object();
        s3Object.setBucketName(bucketName);
        s3Object.setKey(key);
        s3Object.setObjectContent(new ByteArrayInputStream(mockS3Object.getData()));
        s3Object.setObjectMetadata(mockS3Object.getObjectMetadata());
        return s3Object;
    }

    /**
     * <p>
     * A mock implementation which generates a URL which reflects the given request.
     * </p>
     * <p>
     * The URL is composed as such:
     * </p>
     * 
     * <pre>
     * https://{s3BucketName}/{s3ObjectKey}?{queryParams}
     * </pre>
     * <p>
     * Where {@code queryParams} is the URL encoded list of parameters given in the request.
     * </p>
     * <p>
     * The query params include:
     * </p>
     * TODO list the query params in the result
     */
    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest, AmazonS3 s3)
    {
        String host = generatePresignedUrlRequest.getBucketName();
        StringBuilder file = new StringBuilder();
        file.append('/').append(generatePresignedUrlRequest.getKey());
        file.append("?method=").append(generatePresignedUrlRequest.getMethod());
        file.append("&expiration=").append(generatePresignedUrlRequest.getExpiration().getTime());
        try
        {
            return new URL("https", host, file.toString());
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
