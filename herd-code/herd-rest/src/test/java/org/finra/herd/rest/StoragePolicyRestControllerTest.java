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
package org.finra.herd.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.finra.herd.model.api.xml.StoragePolicy;
import org.finra.herd.model.api.xml.StoragePolicyFilter;
import org.finra.herd.model.api.xml.StoragePolicyKey;
import org.finra.herd.model.api.xml.StoragePolicyRule;
import org.finra.herd.model.api.xml.StoragePolicyTransition;
import org.finra.herd.model.jpa.StoragePolicyEntity;

/**
 * This class tests various functionality within the business object data notification registration REST controller.
 */
public class StoragePolicyRestControllerTest extends AbstractRestTest
{
    @Test
    public void testCreateStoragePolicy()
    {
        StoragePolicyKey storagePolicyKey = new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME);

        // Create and persist the relative database entities.
        createDatabaseEntitiesForStoragePolicyTesting();

        // Create a storage policy.
        StoragePolicy resultStoragePolicy = storagePolicyRestController.createStoragePolicy(
            createStoragePolicyCreateRequest(storagePolicyKey, STORAGE_POLICY_RULE_TYPE, STORAGE_POLICY_RULE_VALUE, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE,
                FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2));

        // Validate the returned object.
        assertEquals(
            new StoragePolicy(resultStoragePolicy.getId(), storagePolicyKey, new StoragePolicyRule(STORAGE_POLICY_RULE_TYPE, STORAGE_POLICY_RULE_VALUE),
                new StoragePolicyFilter(BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, STORAGE_NAME),
                new StoragePolicyTransition(STORAGE_NAME_2)), resultStoragePolicy);
    }

    @Test
    public void testGetStoragePolicy()
    {
        StoragePolicyKey storagePolicyKey = new StoragePolicyKey(STORAGE_POLICY_NAMESPACE_CD, STORAGE_POLICY_NAME);

        // Create and persist a storage policy entity.
        StoragePolicyEntity storagePolicyEntity =
            createStoragePolicyEntity(storagePolicyKey, STORAGE_POLICY_RULE_TYPE, STORAGE_POLICY_RULE_VALUE, BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE,
                FORMAT_FILE_TYPE_CODE, STORAGE_NAME, STORAGE_NAME_2);

        StoragePolicy resultStoragePolicy =
            storagePolicyRestController.getStoragePolicy(storagePolicyKey.getNamespace(), storagePolicyKey.getStoragePolicyName());

        // Validate the returned object.
        assertEquals(
            new StoragePolicy(storagePolicyEntity.getId(), storagePolicyKey, new StoragePolicyRule(STORAGE_POLICY_RULE_TYPE, STORAGE_POLICY_RULE_VALUE),
                new StoragePolicyFilter(BOD_NAMESPACE, BOD_NAME, FORMAT_USAGE_CODE, FORMAT_FILE_TYPE_CODE, STORAGE_NAME),
                new StoragePolicyTransition(STORAGE_NAME_2)), resultStoragePolicy);
    }
}
