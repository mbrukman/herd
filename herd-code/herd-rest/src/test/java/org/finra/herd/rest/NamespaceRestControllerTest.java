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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.finra.herd.model.api.xml.Namespace;
import org.finra.herd.model.api.xml.NamespaceKey;
import org.finra.herd.model.api.xml.NamespaceKeys;

/**
 * This class tests various functionality within the namespace REST controller.
 */
public class NamespaceRestControllerTest extends AbstractRestTest
{
    @Test
    public void testCreateNamespace() throws Exception
    {
        // Create a namespace.
        Namespace resultNamespace = namespaceRestController.createNamespace(createNamespaceCreateRequest(NAMESPACE_CD));

        // Validate the returned object.
        validateNamespace(NAMESPACE_CD, resultNamespace);
    }

    @Test
    public void testGetNamespace() throws Exception
    {
        // Create and persist a namespace entity.
        createNamespaceEntity(NAMESPACE_CD);

        // Retrieve the namespace.
        Namespace resultNamespace = namespaceRestController.getNamespace(NAMESPACE_CD);

        // Validate the returned object.
        validateNamespace(NAMESPACE_CD, resultNamespace);
    }

    @Test
    public void testGetNamespaces() throws Exception
    {
        // Create and persist namespace entities.
        for (NamespaceKey key : getTestNamespaceKeys())
        {
            createNamespaceEntity(key.getNamespaceCode());
        }

        // Retrieve a list of namespace keys.
        NamespaceKeys resultNamespaceKeys = namespaceRestController.getNamespaces();

        // Validate the returned object.
        assertNotNull(resultNamespaceKeys);
        assertNotNull(resultNamespaceKeys.getNamespaceKeys());
        assertTrue(resultNamespaceKeys.getNamespaceKeys().size() >= getTestNamespaceKeys().size());
        for (NamespaceKey key : getTestNamespaceKeys())
        {
            assertTrue(resultNamespaceKeys.getNamespaceKeys().contains(key));
        }
    }

    @Test
    public void testDeleteNamespace() throws Exception
    {
        // Create and persist a namespace entity.
        createNamespaceEntity(NAMESPACE_CD);

        // Validate that this namespace exists.
        NamespaceKey namespaceKey = new NamespaceKey(NAMESPACE_CD);
        assertNotNull(herdDao.getNamespaceByKey(namespaceKey));

        // Delete this namespace.
        Namespace deletedNamespace = namespaceRestController.deleteNamespace(NAMESPACE_CD);

        // Validate the returned object.
        validateNamespace(NAMESPACE_CD, deletedNamespace);

        // Ensure that this namespace is no longer there.
        assertNull(herdDao.getNamespaceByKey(namespaceKey));
    }

}
