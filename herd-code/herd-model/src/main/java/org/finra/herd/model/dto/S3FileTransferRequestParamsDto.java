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
package org.finra.herd.model.dto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A DTO that holds various parameters for making an S3 file/directory transfer request.
 * <p/>
 * Consider using the builder to make constructing this class easier. For example:
 * <p/>
 * <pre>
 * S3FileTransferRequestParamsDto params = S3FileTransferRequestParamsDto
 *     .builder().s3BucketName(&quot;myBucket&quot;).s3KeyPrefix(&quot;myS3KeyPrefix&quot;).build();
 * </pre>
 */
public class S3FileTransferRequestParamsDto extends AwsParamsDto
{
    /**
     * The {@link #signerOverride} value to enable S3 Signature Version 4.
     */
    public static final String SIGNER_OVERRIDE_V4 = "AWSS3V4SignerType";

    /**
     * The optional S3 endpoint to use when making S3 service calls.
     */
    private String s3Endpoint;

    /**
     * The S3 bucket name.
     */
    private String s3BucketName;

    /**
     * The S3 key prefix for copying to or from.
     */
    private String s3KeyPrefix;

    /**
     * The local file path (file or directory as appropriate).
     */
    private String localPath;

    /**
     * A list of files to upload relative to the local path for upload or S3 key prefix for download. In any case, when we specify the file list, the local path
     * should be a directory.
     */
    private List<File> files;

    /**
     * For directory copies, this determines if the copy will recurse into subdirectories.
     */
    private Boolean isRecursive;

    /**
     * This flag determines if S3 reduced redundancy storage will be used when copying to S3 (when supported).
     */
    private Boolean useRrs;

    /**
     * The S3 access key used for S3 authentication.
     */
    private String s3AccessKey;

    /**
     * The S3 secret key used for S3 authentication.
     */
    private String s3SecretKey;

    /**
     * The maximum number of threads to use for file copying.
     */
    private Integer maxThreads;
    
    /**
     * The KMS id to use for server side encryption.
     */
    private String kmsKeyId;

    /**
     * The S3 signer override type.
     * This should be used to enable SigV4 ({@link #SIGNER_OVERRIDE_V4}).
     * This should ONLY be set when generating pre-signed URL for KMS encrypted objects, and SHOULD NOT be used for any other S3 requests.
     * Overriding this value to anything other that specified may adversely affect the S3 operation.
     */
    private String signerOverride;

    /**
     * Any additional AWS credentials providers the S3 operation should use to get credentials.
     */
    private List<HerdAWSCredentialsProvider> additionalAwsCredentialsProviders = new ArrayList<>();

    public String getS3Endpoint()
    {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint)
    {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3BucketName()
    {
        return s3BucketName;
    }

    public void setS3BucketName(String s3BucketName)
    {
        this.s3BucketName = s3BucketName;
    }

    public String getS3KeyPrefix()
    {
        return s3KeyPrefix;
    }

    public void setS3KeyPrefix(String s3KeyPrefix)
    {
        this.s3KeyPrefix = s3KeyPrefix;
    }

    public String getLocalPath()
    {
        return localPath;
    }

    public void setLocalPath(String localPath)
    {
        this.localPath = localPath;
    }

    public List<File> getFiles()
    {
        return this.files;
    }

    public void setFiles(List<File> files)
    {
        this.files = files;
    }

    public Boolean getRecursive()
    {
        return isRecursive;
    }

    public void setRecursive(Boolean recursive)
    {
        isRecursive = recursive;
    }

    public Boolean getUseRrs()
    {
        return useRrs;
    }

    public void setUseRrs(Boolean useRrs)
    {
        this.useRrs = useRrs;
    }

    public String getS3AccessKey()
    {
        return s3AccessKey;
    }

    public void setS3AccessKey(String s3AccessKey)
    {
        this.s3AccessKey = s3AccessKey;
    }

    public String getS3SecretKey()
    {
        return s3SecretKey;
    }

    public void setS3SecretKey(String s3SecretKey)
    {
        this.s3SecretKey = s3SecretKey;
    }

    public Integer getMaxThreads()
    {
        return maxThreads;
    }

    public void setMaxThreads(Integer maxThreads)
    {
        this.maxThreads = maxThreads;
    }

    public String getKmsKeyId()
    {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId)
    {
        this.kmsKeyId = kmsKeyId;
    }

    public String getSignerOverride()
    {
        return signerOverride;
    }

    public void setSignerOverride(String signerOverride)
    {
        this.signerOverride = signerOverride;
    }

    public List<HerdAWSCredentialsProvider> getAdditionalAwsCredentialsProviders()
    {
        return additionalAwsCredentialsProviders;
    }

    public void setAdditionalAwsCredentialsProviders(List<HerdAWSCredentialsProvider> additionalAwsCredentialsProviders)
    {
        this.additionalAwsCredentialsProviders = additionalAwsCredentialsProviders;
    }

    /**
     * Returns a builder that can easily build this DTO.
     *
     * @return the builder.
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * A builder that makes it easier to construct this DTO.
     */
    public static class Builder
    {
        private S3FileTransferRequestParamsDto params = new S3FileTransferRequestParamsDto();

        public Builder s3Endpoint(String s3Endpoint)
        {
            params.setS3Endpoint(s3Endpoint);
            return this;
        }

        public Builder s3BucketName(String s3BucketName)
        {
            params.setS3BucketName(s3BucketName);
            return this;
        }

        public Builder s3KeyPrefix(String s3KeyPrefix)
        {
            params.setS3KeyPrefix(s3KeyPrefix);
            return this;
        }

        public Builder localPath(String localPath)
        {
            params.setLocalPath(localPath);
            return this;
        }

        public Builder files(List<File> files)
        {
            params.setFiles(files);
            return this;
        }

        public Builder recursive(Boolean recursive)
        {
            params.setRecursive(recursive);
            return this;
        }

        public Builder useRrs(Boolean useRrs)
        {
            params.setUseRrs(useRrs);
            return this;
        }

        public Builder s3AccessKey(String s3AccessKey)
        {
            params.setS3AccessKey(s3AccessKey);
            return this;
        }

        public Builder s3SecretKey(String s3SecretKey)
        {
            params.setS3SecretKey(s3SecretKey);
            return this;
        }

        public Builder maxThreads(Integer maxThreads)
        {
            params.setMaxThreads(maxThreads);
            return this;
        }

        public Builder httpProxyHost(String httpProxyHost)
        {
            params.setHttpProxyHost(httpProxyHost);
            return this;
        }

        public Builder httpProxyPort(Integer httpProxyPort)
        {
            params.setHttpProxyPort(httpProxyPort);
            return this;
        }

        public Builder kmsKeyId(String kmsKeyId)
        {
            params.setKmsKeyId(kmsKeyId);
            return this;
        }

        public Builder signerOverride(String signerOverride)
        {
            params.setSignerOverride(signerOverride);
            return this;
        }

        public Builder additionalAwsCredentialsProviders(List<HerdAWSCredentialsProvider> additionalAwsCredentialsProviders)
        {
            params.setAdditionalAwsCredentialsProviders(additionalAwsCredentialsProviders);
            return this;
        }

        public S3FileTransferRequestParamsDto build()
        {
            return params;
        }
    }
}
