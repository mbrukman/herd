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
package org.finra.herd.service.helper;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.finra.herd.core.ApplicationContextHolder;
import org.finra.herd.core.helper.ConfigurationHelper;
import org.finra.herd.dao.helper.JsonHelper;
import org.finra.herd.model.dto.ConfigurationValue;
import org.finra.herd.model.dto.StoragePolicySelection;
import org.finra.herd.service.StoragePolicyProcessorService;

/*
 * The storage policy processor JMS message listener.
 */
@Component
public class StoragePolicyProcessorJmsMessageListener
{
    private static final Logger LOGGER = Logger.getLogger(StoragePolicyProcessorJmsMessageListener.class);

    @Autowired
    private HerdHelper herdHelper;

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private StoragePolicyHelper storagePolicyHelper;

    @Autowired
    private StoragePolicyProcessorService storagePolicyProcessorService;

    @Autowired
    private ConfigurationHelper configurationHelper;

    /**
     * Processes a JMS message.
     *
     * @param payload the message payload
     * @param allHeaders the JMS headers
     */
    @JmsListener(id = HerdJmsDestinationResolver.SQS_DESTINATION_STORAGE_POLICY_SELECTOR_JOB_SQS_QUEUE,
        containerFactory = "storagePolicyProcessorJmsListenerContainerFactory",
        destination = HerdJmsDestinationResolver.SQS_DESTINATION_STORAGE_POLICY_SELECTOR_JOB_SQS_QUEUE)
    public void processMessage(String payload, @Headers Map<Object, Object> allHeaders)
    {
        LOGGER.info(String.format("JMS message received from \"%s\" queue. Headers: \"%s\" Payload: \"%s\"",
            HerdJmsDestinationResolver.SQS_DESTINATION_STORAGE_POLICY_SELECTOR_JOB_SQS_QUEUE, allHeaders, payload));

        // Process the message as storage policy selection message.
        try
        {
            // Process messages coming from the storage policy selector job.
            StoragePolicySelection storagePolicySelection = jsonHelper.unmarshallJsonToObject(StoragePolicySelection.class, payload);

            LOGGER.debug(String.format("Received storage policy selection message: business object data: {%s}, " + "storage policy: {%s}",
                herdHelper.businessObjectDataKeyToString(storagePolicySelection.getBusinessObjectDataKey()),
                storagePolicyHelper.storagePolicyKeyToString(storagePolicySelection.getStoragePolicyKey())));

            // Process the storage policy selection message.
            storagePolicyProcessorService.processStoragePolicySelectionMessage(storagePolicySelection);
        }
        catch (Exception e)
        {
            LOGGER.error(String.format("Failed to process JMS message from \"%s\" queue. Payload: \"%s\"",
                HerdJmsDestinationResolver.SQS_DESTINATION_STORAGE_POLICY_SELECTOR_JOB_SQS_QUEUE, payload), e);
        }
    }

    /**
     * Periodically check the configuration and apply the action to the storage policy processor JMS message listener service, if needed.
     */
    @Scheduled(fixedDelay = 60000)
    public void controlStoragePolicyProcessorJmsMessageListener()
    {
        try
        {
            // Get the configuration setting.
            Boolean jmsMessageListenerEnabled =
                Boolean.valueOf(configurationHelper.getProperty(ConfigurationValue.STORAGE_POLICY_PROCESSOR_JMS_LISTENER_ENABLED));

            // Get the registry bean.
            JmsListenerEndpointRegistry registry = ApplicationContextHolder.getApplicationContext()
                .getBean("org.springframework.jms.config.internalJmsListenerEndpointRegistry", JmsListenerEndpointRegistry.class);

            // Get the storage policy processor JMS message listener container.
            MessageListenerContainer jmsMessageListenerContainer =
                registry.getListenerContainer(HerdJmsDestinationResolver.SQS_DESTINATION_STORAGE_POLICY_SELECTOR_JOB_SQS_QUEUE);

            // Get the current JMS message listener status and the configuration value.
            LOGGER.debug(String.format("controlStoragePolicyProcessorJmsMessageListener(): %s=%s jmsMessageListenerContainer.isRunning()=%b",
                ConfigurationValue.STORAGE_POLICY_PROCESSOR_JMS_LISTENER_ENABLED.getKey(), jmsMessageListenerEnabled, jmsMessageListenerContainer.isRunning()));

            // Apply the relative action if needed.
            if (!jmsMessageListenerEnabled && jmsMessageListenerContainer.isRunning())
            {
                LOGGER.info("controlStoragePolicyProcessorJmsMessageListener(): Stopping the storage policy processor JMS message listener ...");
                jmsMessageListenerContainer.stop();
                LOGGER.info("controlStoragePolicyProcessorJmsMessageListener(): Done");
            }
            else if (jmsMessageListenerEnabled && !jmsMessageListenerContainer.isRunning())
            {
                LOGGER.info("controlStoragePolicyProcessorJmsMessageListener(): Starting the storage policy processor JMS message listener ...");
                jmsMessageListenerContainer.start();
                LOGGER.info("controlStoragePolicyProcessorJmsMessageListener(): Done");
            }
        }
        catch (Exception e)
        {
            LOGGER.error(String
                .format("controlStoragePolicyProcessorJmsMessageListener(): Failed to control the storage policy processor Jms message listener service."), e);
        }
    }
}
