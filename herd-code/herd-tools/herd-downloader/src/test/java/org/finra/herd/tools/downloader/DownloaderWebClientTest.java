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
package org.finra.herd.tools.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.finra.herd.core.Command;
import org.finra.herd.model.dto.RegServerAccessParamsDto;
import org.finra.herd.model.dto.DownloaderInputManifestDto;
import org.finra.herd.model.dto.UploaderInputManifestDto;
import org.finra.herd.model.jpa.StorageEntity;
import org.finra.herd.model.api.xml.AwsCredential;
import org.finra.herd.model.api.xml.BusinessObjectData;
import org.finra.herd.model.api.xml.BusinessObjectDataDownloadCredential;
import org.finra.herd.model.api.xml.S3KeyPrefixInformation;
import org.finra.herd.tools.common.databridge.DataBridgeWebClient;

/**
 * Unit tests for DownloaderWebClient class.
 */
public class DownloaderWebClientTest extends AbstractDownloaderTest
{
    @Test
    public void testWebClientRegServerAccessParamsDtoSetterAndGetter()
    {
        // Create and initialize an instance of RegServerAccessParamsDto.
        RegServerAccessParamsDto regServerAccessParamsDto = new RegServerAccessParamsDto();
        regServerAccessParamsDto.setRegServerHost(WEB_SERVICE_HOSTNAME);
        regServerAccessParamsDto.setRegServerPort(WEB_SERVICE_HTTPS_PORT);
        regServerAccessParamsDto.setUseSsl(true);
        regServerAccessParamsDto.setUsername(WEB_SERVICE_HTTPS_USERNAME);
        regServerAccessParamsDto.setPassword(WEB_SERVICE_HTTPS_PASSWORD);

        // Set the DTO.
        downloaderWebClient.setRegServerAccessParamsDto(regServerAccessParamsDto);

        // Retrieve the DTO and validate the results.
        RegServerAccessParamsDto resultRegServerAccessParamsDto = downloaderWebClient.getRegServerAccessParamsDto();

        // validate the results.
        assertEquals(WEB_SERVICE_HOSTNAME, resultRegServerAccessParamsDto.getRegServerHost());
        assertEquals(WEB_SERVICE_HTTPS_PORT, resultRegServerAccessParamsDto.getRegServerPort());
        assertTrue(resultRegServerAccessParamsDto.getUseSsl());
        assertEquals(WEB_SERVICE_HTTPS_USERNAME, resultRegServerAccessParamsDto.getUsername());
        assertEquals(WEB_SERVICE_HTTPS_PASSWORD, resultRegServerAccessParamsDto.getPassword());
    }

    @Test
    public void testGetS3KeyPrefix() throws Exception
    {
        // Upload and register business object data parents.
        uploadAndRegisterTestDataParents(downloaderWebClient);

        // Upload and register the initial version if of the test business object data.
        uploadTestDataFilesToS3(S3_TEST_PATH_V0);
        final UploaderInputManifestDto uploaderInputManifestDto = getTestUploaderInputManifestDto();

        executeWithoutLogging(DataBridgeWebClient.class, new Command()
        {
            @Override
            public void execute() throws Exception
            {
                downloaderWebClient.registerBusinessObjectData(uploaderInputManifestDto, getTestS3FileTransferRequestParamsDto(S3_TEST_PATH_V0 + "/"),
                    StorageEntity.MANAGED_STORAGE, false);
            }
        });

        // Get S3 key prefix.
        BusinessObjectData businessObjectData = toBusinessObjectData(uploaderInputManifestDto);
        S3KeyPrefixInformation resultS3KeyPrefixInformation = downloaderWebClient.getS3KeyPrefix(businessObjectData);

        // Validate the results.
        assertNotNull(resultS3KeyPrefixInformation);
        assertEquals(S3_SIMPLE_TEST_PATH, resultS3KeyPrefixInformation.getS3KeyPrefix());
    }

    @Test
    public void testGetData() throws Exception
    {
        // Upload and register business object data parents.
        uploadAndRegisterTestDataParents(downloaderWebClient);

        // Upload and register the initial version if of the test business object data.
        uploadTestDataFilesToS3(S3_TEST_PATH_V0);
        final UploaderInputManifestDto uploaderInputManifestDto = getTestUploaderInputManifestDto();

        executeWithoutLogging(DataBridgeWebClient.class, new Command()
        {
            @Override
            public void execute() throws Exception
            {
                downloaderWebClient.registerBusinessObjectData(uploaderInputManifestDto, getTestS3FileTransferRequestParamsDto(S3_TEST_PATH_V0 + "/"),
                    StorageEntity.MANAGED_STORAGE, false);
            }
        });

        // Get business object data information.
        DownloaderInputManifestDto downloaderInputManifestDto = getTestDownloaderInputManifestDto();
        BusinessObjectData resultBusinessObjectData = downloaderWebClient.getBusinessObjectData(downloaderInputManifestDto);

        // Validate the results.
        assertNotNull(resultBusinessObjectData);
    }

    @Test
    public void testGetBusinessObjectDataDownloadCredential1() throws Exception
    {
        DownloaderInputManifestDto manifest = new DownloaderInputManifestDto();
        manifest.setNamespace("test1");
        manifest.setBusinessObjectDefinitionName("test2");
        manifest.setBusinessObjectFormatUsage("test3");
        manifest.setBusinessObjectFormatFileType("test4");
        manifest.setBusinessObjectFormatVersion("test5");
        manifest.setPartitionValue("test6");
        manifest.setSubPartitionValues(Arrays.asList("test7", "test8"));
        manifest.setBusinessObjectDataVersion("test9");
        String storageName = "test10";
        BusinessObjectDataDownloadCredential businessObjectDataDownloadCredential = downloaderWebClient.getBusinessObjectDataDownloadCredential(manifest,
            storageName);
        Assert.assertNotNull(businessObjectDataDownloadCredential);
        AwsCredential awsCredential = businessObjectDataDownloadCredential.getAwsCredential();
        Assert.assertNotNull(awsCredential);
        Assert.assertEquals("https://testWebServiceHostname:1234/herd-app/rest/businessObjectData/download/credential/namespaces/test1"
            + "/businessObjectDefinitionNames/test2/businessObjectFormatUsages/test3/businessObjectFormatFileTypes/test4/businessObjectFormatVersions/test5"
            + "/partitionValues/test6/businessObjectDataVersions/test9?storageName=test10&subPartitionValues=test7%7Ctest8", awsCredential.getAwsAccessKey());
    }

    @Test
    public void testGetBusinessObjectDataDownloadCredential2() throws Exception
    {
        DownloaderInputManifestDto manifest = new DownloaderInputManifestDto();
        manifest.setNamespace("test1");
        manifest.setBusinessObjectDefinitionName("test2");
        manifest.setBusinessObjectFormatUsage("test3");
        manifest.setBusinessObjectFormatFileType("test4");
        manifest.setBusinessObjectFormatVersion("test5");
        manifest.setPartitionValue("test6");
        manifest.setBusinessObjectDataVersion("test9");
        String storageName = "test10";
        downloaderWebClient.getRegServerAccessParamsDto().setUseSsl(true);
        BusinessObjectDataDownloadCredential businessObjectDataDownloadCredential = downloaderWebClient.getBusinessObjectDataDownloadCredential(manifest,
            storageName);
        Assert.assertNotNull(businessObjectDataDownloadCredential);
        AwsCredential awsCredential = businessObjectDataDownloadCredential.getAwsCredential();
        Assert.assertNotNull(awsCredential);
        Assert.assertEquals("https://testWebServiceHostname:1234/herd-app/rest/businessObjectData/download/credential/namespaces/test1"
            + "/businessObjectDefinitionNames/test2/businessObjectFormatUsages/test3/businessObjectFormatFileTypes/test4/businessObjectFormatVersions/test5"
            + "/partitionValues/test6/businessObjectDataVersions/test9?storageName=test10", awsCredential.getAwsAccessKey());
    }

    private BusinessObjectData toBusinessObjectData(final UploaderInputManifestDto uploaderInputManifestDto)
    {
        BusinessObjectData businessObjectData = new BusinessObjectData();
        businessObjectData.setNamespace(uploaderInputManifestDto.getNamespace());
        businessObjectData.setBusinessObjectDefinitionName(uploaderInputManifestDto.getBusinessObjectDefinitionName());
        businessObjectData.setBusinessObjectFormatUsage(uploaderInputManifestDto.getBusinessObjectFormatUsage());
        businessObjectData.setBusinessObjectFormatFileType(uploaderInputManifestDto.getBusinessObjectFormatFileType());
        businessObjectData.setBusinessObjectFormatVersion(Integer.valueOf(uploaderInputManifestDto.getBusinessObjectFormatVersion()));
        businessObjectData.setPartitionKey(uploaderInputManifestDto.getPartitionKey());
        businessObjectData.setPartitionValue(uploaderInputManifestDto.getPartitionValue());
        businessObjectData.setSubPartitionValues(uploaderInputManifestDto.getSubPartitionValues());
        businessObjectData.setVersion(TEST_DATA_VERSION_V0);
        return businessObjectData;
    }
}
