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
package org.finra.herd.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.finra.herd.dao.config.DaoSpringModuleConfig;
import org.finra.herd.model.api.xml.BusinessObjectData;
import org.finra.herd.model.api.xml.BusinessObjectDataAvailability;
import org.finra.herd.model.api.xml.BusinessObjectDataAvailabilityCollectionRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataAvailabilityCollectionResponse;
import org.finra.herd.model.api.xml.BusinessObjectDataAvailabilityRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataCreateRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataDdl;
import org.finra.herd.model.api.xml.BusinessObjectDataDdlCollectionRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataDdlCollectionResponse;
import org.finra.herd.model.api.xml.BusinessObjectDataDdlRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataInvalidateUnregisteredRequest;
import org.finra.herd.model.api.xml.BusinessObjectDataInvalidateUnregisteredResponse;
import org.finra.herd.model.api.xml.BusinessObjectDataKey;
import org.finra.herd.model.api.xml.S3KeyPrefixInformation;
import org.finra.herd.service.BusinessObjectDataService;
import org.finra.herd.service.helper.BusinessObjectDataHelper;

/**
 * This is a Business Object Data service implementation for testing.
 */
@Service
@Transactional(value = DaoSpringModuleConfig.HERD_TRANSACTION_MANAGER_BEAN_NAME)
@Primary
public class TestBusinessObjectDataServiceImpl extends BusinessObjectDataServiceImpl implements BusinessObjectDataService
{
    @Autowired
    private BusinessObjectDataHelper businessObjectDataHelper;

    // Overwrite the base class method to change transactional attributes.
    @Override
    public BusinessObjectDataAvailability checkBusinessObjectDataAvailability(BusinessObjectDataAvailabilityRequest request)
    {
        return checkBusinessObjectDataAvailabilityImpl(request);
    }

    // Overwrite the base class method to change transactional attributes.
    @Override
    public BusinessObjectDataAvailabilityCollectionResponse checkBusinessObjectDataAvailabilityCollection(
        BusinessObjectDataAvailabilityCollectionRequest request)
    {
        return checkBusinessObjectDataAvailabilityCollectionImpl(request);
    }

    @Override
    public BusinessObjectData createBusinessObjectData(BusinessObjectDataCreateRequest request)
    {
        return businessObjectDataHelper.createBusinessObjectData(request);
    }

    @Override
    public BusinessObjectData getBusinessObjectData(BusinessObjectDataKey businessObjectDataKey, String businessObjectFormatPartitionKey)
    {
        return getBusinessObjectDataImpl(businessObjectDataKey, businessObjectFormatPartitionKey);
    }

    @Override
    public S3KeyPrefixInformation getS3KeyPrefix(BusinessObjectDataKey businessObjectDataKey, String businessObjectFormatPartitionKey, Boolean createNewVersion)
    {
        return getS3KeyPrefixImpl(businessObjectDataKey, businessObjectFormatPartitionKey, createNewVersion);
    }

    @Override
    public BusinessObjectDataDdl generateBusinessObjectDataDdl(BusinessObjectDataDdlRequest request)
    {
        return generateBusinessObjectDataDdlImpl(request, false);
    }

    // Overwrite the base class method to change transactional attributes.
    @Override
    public BusinessObjectDataDdlCollectionResponse generateBusinessObjectDataDdlCollection(BusinessObjectDataDdlCollectionRequest request)
    {
        return generateBusinessObjectDataDdlCollectionImpl(request);
    }

    @Override
    public BusinessObjectDataInvalidateUnregisteredResponse invalidateUnregisteredBusinessObjectData(
        BusinessObjectDataInvalidateUnregisteredRequest businessObjectDataInvalidateUnregisteredRequest)
    {
        return invalidateUnregisteredBusinessObjectDataImpl(businessObjectDataInvalidateUnregisteredRequest);
    }
}
