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
package org.finra.herd.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.finra.herd.dao.impl.MockSqsOperationsImpl;
import org.finra.herd.model.api.xml.BusinessObjectDataKey;
import org.finra.herd.model.api.xml.StoragePolicyKey;
import org.finra.herd.model.dto.StoragePolicySelection;
import org.finra.herd.model.jpa.BusinessObjectDataEntity;
import org.finra.herd.model.jpa.BusinessObjectDataStatusEntity;
import org.finra.herd.model.jpa.StoragePolicyRuleTypeEntity;
import org.finra.herd.model.jpa.StorageUnitEntity;
import org.finra.herd.model.jpa.StorageUnitStatusEntity;

/**
 * This class tests functionality within the StoragePolicySelectorService.
 */
public class StoragePolicySelectorServiceTest extends AbstractServiceTest
{
    @Test
    public void testExecute() throws Exception
    {
        // Create a storage policy key.
        StoragePolicyKey storagePolicyKey = new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME);

        // Create and persist a storage policy entity.
        createStoragePolicyEntity(storagePolicyKey, StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS, BOD_NAMESPACE, BOD_NAME,
            FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Apply the offset in days to business object data "created on" value.
        ageBusinessObjectData(storageUnitEntity.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 1);

        // Execute the storage policy selection.
        List<StoragePolicySelection> resultStoragePolicySelections = storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT);

        // Validate the results.
        assertEquals(Arrays.asList(new StoragePolicySelection(
            new BusinessObjectDataKey(BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE, SUBPARTITION_VALUES,
                DATA_VERSION), storagePolicyKey)), resultStoragePolicySelections);
    }

    @Test
    public void testExecuteWithInvalidSqsQueueName() throws Exception
    {
        // Create and persist a storage policy entity.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE,
            STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Apply the offset in days to business object data "created on" value.
        ageBusinessObjectData(storageUnitEntity.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 1);

        // Try to execute the storage policy selection by passing an invalid SQS queue name.
        try
        {
            storagePolicySelectorService.execute(MockSqsOperationsImpl.MOCK_SQS_QUEUE_NOT_FOUND_NAME, MAX_RESULT);
            fail("Should throw an IllegalStateException when invalid SQS queue name is specified.");
        }
        catch (IllegalStateException e)
        {
            assertEquals(String.format("AWS SQS queue with \"%s\" name not found.", MockSqsOperationsImpl.MOCK_SQS_QUEUE_NOT_FOUND_NAME), e.getMessage());
        }
    }

    @Test
    public void testExecuteWithInvalidStoragePolicyRuleType() throws Exception
    {
        // Create and persist a storage policy entity with a non-supported storage policy type.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME), STORAGE_POLICY_RULE_TYPE, BDATA_AGE_IN_DAYS,
            BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Apply the offset in days to business object data "created on" value.
        ageBusinessObjectData(storageUnitEntity.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 1);

        // Try to retrieve the business object data as matching to the storage policy.
        try
        {
            storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT);
            fail("Should throw an IllegalStateException when a storage policy has an invalid storage policy rule type.");
        }
        catch (IllegalStateException e)
        {
            assertEquals(String.format("Storage policy type \"%s\" is not supported.", STORAGE_POLICY_RULE_TYPE), e.getMessage());
        }
    }

    @Test
    public void testExecuteBusinessObjectDataNotOldEnough()
    {
        // Create and persist a storage policy entity.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE,
            STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, STORAGE_UNIT_STATUS,
                NO_STORAGE_DIRECTORY_PATH);

        // Apply the offset in days to business object data "created on" value.
        ageBusinessObjectData(storageUnitEntity.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 1);

        // Execute the storage policy selection and validate the results. One business object data matching to storage policy should get selected.
        assertEquals(1, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Apply another offset in days to business object data "created on" value to make it one day not old enough for the storage policy.
        ageBusinessObjectData(storageUnitEntity.getBusinessObjectData(), -2);

        // Execute the storage policy selection and validate the results. No business object data matching to storage policy should get selected.
        assertEquals(0, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());
    }

    @Test
    public void testExecuteBusinessObjectDataNotSelectedDueToHigherPriorityLevelStoragePolicy()
    {
        // Storage a storage policy with a filter that has no fields specified.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD_2, STORAGE_POLICY_NAME_2),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS, NO_BOD_NAMESPACE, NO_BOD_NAME, NO_FORMAT_USAGE_CODE,
            NO_FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Get the business object data entity.
        BusinessObjectDataEntity businessObjectDataEntity = storageUnitEntity.getBusinessObjectData();

        // Apply the offset in days to business object data "created on" value, so it would match to the storage policy.
        ageBusinessObjectData(businessObjectDataEntity, BDATA_AGE_IN_DAYS + 1);

        // Execute the storage policy selection and validate the results. The business object data is expected to be selected.
        assertEquals(1, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Storage a storage policy with a filter that has only usage and file type specified
        // and with the age restriction greater than the current business object data entity age.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD_2, STORAGE_POLICY_NAME),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS + 2, NO_BOD_NAMESPACE, NO_BOD_NAME, FORMAT_USAGE_CODE,
            FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Execute the storage policy selection and validate the results. The business object data is not expected to be selected.
        assertEquals(0, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Apply the offset in days to business object data "created on" value, so it would match to the last added storage policy.
        ageBusinessObjectData(businessObjectDataEntity, 2);

        // Execute the storage policy selection and validate the results. The business object data is expected to be selected.
        assertEquals(1, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Storage a storage policy with a filter that has only business object definition
        // specified and with the age restriction greater than the current business object data entity age.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME_2),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS + 4, BOD_NAMESPACE, BOD_NAME, NO_FORMAT_USAGE_CODE,
            NO_FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Execute the storage policy selection and validate the results. The business object data is not expected to be selected.
        assertEquals(0, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Apply the offset in days to business object data "created on" value, so it would match to the last added storage policy.
        ageBusinessObjectData(businessObjectDataEntity, 2);

        // Execute the storage policy selection and validate the results. The business object data is expected to be selected.
        assertEquals(1, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Storage a storage policy with a filter that has business object definition, usage, and file type
        // specified and with the age restriction greater than the current business object data entity age.
        createStoragePolicyEntity(new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME),
            StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS + 6, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE,
            STORAGE_NAME, STORAGE_NAME_2);

        // Execute the storage policy selection and validate the results. The business object data is not expected to be selected.
        assertEquals(0, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());

        // Apply the offset in days to business object data "created on" value, so it would match to the last added storage policy.
        ageBusinessObjectData(businessObjectDataEntity, 2);

        // Execute the storage policy selection and validate the results. The business object data is expected to be selected.
        assertEquals(1, storagePolicySelectorService.execute(SQS_QUEUE_NAME, MAX_RESULT).size());
    }

    @Test
    public void testExecuteTestingMaxResult()
    {
        // Create a storage policy key.
        StoragePolicyKey storagePolicyKey = new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME);

        // Create and persist a storage policy entity.
        createStoragePolicyEntity(storagePolicyKey, StoragePolicyRuleTypeEntity.DAYS_SINCE_BDATA_REGISTERED, BDATA_AGE_IN_DAYS, BOD_NAMESPACE, BOD_NAME,
            FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        // Create and persist a storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity1 =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Apply the offset in days to business object data "created on" value.
        ageBusinessObjectData(storageUnitEntity1.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 1);

        // Create and persist a second storage unit in the storage policy filter storage.
        StorageUnitEntity storageUnitEntity2 =
            createStorageUnitEntity(STORAGE_NAME, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE_2,
                SUBPARTITION_VALUES, DATA_VERSION, LATEST_VERSION_FLAG_SET, BusinessObjectDataStatusEntity.VALID, StorageUnitStatusEntity.ENABLED,
                NO_STORAGE_DIRECTORY_PATH);

        // Also apply an offset to business object data "created on" value, but make this business object data older than the first.
        ageBusinessObjectData(storageUnitEntity2.getBusinessObjectData(), BDATA_AGE_IN_DAYS + 2);

        // Try to retrieve both business object data instances as matching to the storage policy, but with max result limit set to 1.
        List<StoragePolicySelection> resultStoragePolicySelections = storagePolicySelectorService.execute(SQS_QUEUE_NAME, 1);

        // Validate the results. Only the oldest business object data should get selected.
        assertEquals(Arrays.asList(new StoragePolicySelection(
            new BusinessObjectDataKey(BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, FORMAT_VERSION, PARTITION_VALUE_2, SUBPARTITION_VALUES,
                DATA_VERSION), storagePolicyKey)), resultStoragePolicySelections);
    }
}
