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
package org.finra.herd.tools.common.databridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

import org.finra.herd.core.ArgumentParser;
import org.finra.herd.dao.S3Operations;
import org.finra.herd.model.api.xml.BuildInformation;
import org.finra.herd.tools.common.databridge.DataBridgeApp.ReturnValue;

/**
 * Tests the DataBridgeApp class.
 */
public class DataBridgeAppTest extends AbstractDataBridgeTest
{
    private PrintStream originalStream = System.out;

    private MockDataBridgeApp dataBridgeApp = new MockDataBridgeApp();

    @Test
    public void testParseShortCommandLineArgumentsSuccess() throws Exception
    {
        String[] arguments =
            {"-a", S3_ACCESS_KEY, "-p", S3_SECRET_KEY, "-e", S3_ENDPOINT_US_STANDARD, "-l", LOCAL_TEMP_PATH_INPUT.toString(), "-m", STRING_VALUE, "-H",
                WEB_SERVICE_HOSTNAME, "-P", WEB_SERVICE_HTTPS_PORT.toString(), "-n", HTTP_PROXY_HOST, "-o", HTTP_PROXY_PORT.toString()};
        assertNull(dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseLongCommandLineArgumentsSuccess() throws Exception
    {
        String[] arguments = {"--s3AccessKey", S3_ACCESS_KEY, "--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath",
            LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath", STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort",
            WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost", HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString()};
        assertNull(dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsMissingUsername() throws Exception
    {
        String[] arguments = {"--s3AccessKey", S3_ACCESS_KEY, "--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath",
            LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath", STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort",
            WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost", HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString(), "-s", "true", "-w",
            WEB_SERVICE_HTTPS_PASSWORD};
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsS3SecretAndAccessKeys() throws Exception
    {
        // Both secret and access keys not specified is valid.
        String[] argumentsNoKeys = {"--s3AccessKey", S3_ACCESS_KEY, "--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath",
            LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath", STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort",
            WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost", HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString()};
        assertNull(dataBridgeApp.parseCommandLineArguments(argumentsNoKeys, applicationContext));

        // Access key is present, but secret key is not which is invalid.
        String[] argumentsMissingSecretKey =
            {"--s3AccessKey", S3_ACCESS_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath", LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath",
                STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort", WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost",
                HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString()};
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(argumentsMissingSecretKey, applicationContext));

        // Secret key is present, but access key is not which is invalid.
        String[] argumentsMissingAccessKeyKey =
            {"--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath", LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath",
                STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort", WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost",
                HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString()};
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(argumentsMissingAccessKeyKey, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsMissingPassword() throws Exception
    {
        String[] arguments = {"--s3AccessKey", S3_ACCESS_KEY, "--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath",
            LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath", STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort",
            WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost", HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString(), "-s", "true", "-u",
            WEB_SERVICE_HTTPS_USERNAME};
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseCommandLineInvalidSslValue() throws Exception
    {
        String[] arguments = {"--s3AccessKey", S3_ACCESS_KEY, "--s3SecretKey", S3_SECRET_KEY, "--s3Endpoint", S3_ENDPOINT_US_STANDARD, "--localPath",
            LOCAL_TEMP_PATH_INPUT.toString(), "--manifestPath", STRING_VALUE, "--regServerHost", WEB_SERVICE_HOSTNAME, "--regServerPort",
            WEB_SERVICE_HTTPS_PORT.toString(), "--httpProxyHost", HTTP_PROXY_HOST, "--httpProxyPort", HTTP_PROXY_PORT.toString(), "-s", "INVALID_BOOLEAN_VALUE",
            "-u", WEB_SERVICE_HTTPS_USERNAME, "-w", WEB_SERVICE_HTTPS_PASSWORD};
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsOptionalParameters() throws Exception
    {
        String[] arguments =
            {"-a", S3_ACCESS_KEY, "-p", S3_SECRET_KEY, "-l", LOCAL_TEMP_PATH_INPUT.toString(), "-m", STRING_VALUE, "-H", WEB_SERVICE_HOSTNAME, "-P",
                WEB_SERVICE_HTTPS_PORT.toString(), "-n", HTTP_PROXY_HOST, "-o", HTTP_PROXY_PORT.toString()};
        assertNull(dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsNone() throws Exception
    {
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.parseCommandLineArguments(new String[] {}, applicationContext));
    }

    @Test
    public void testParseCommandLineArgumentsHelpOpt() throws Exception
    {
        String output = runTestGetSystemOut(new Runnable()
        {
            @Override
            public void run()
            {
                String[] arguments = {"--help"};
                assertEquals(ReturnValue.SUCCESS, dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
            }
        });

        assertTrue("Incorrect usage information returned.", output.startsWith("usage: " + MockDataBridgeApp.TEST_APPLICATION_NAME));
    }

    @Test
    public void testParseCommandLineArgumentsVersionOpt()
    {
        String output = runTestGetSystemOut(new Runnable()
        {
            @Override
            public void run()
            {
                String[] arguments = {"--version"};
                assertEquals(ReturnValue.SUCCESS, dataBridgeApp.parseCommandLineArguments(arguments, applicationContext));
            }
        });

        BuildInformation buildInformation = applicationContext.getBean(BuildInformation.class);

        assertEquals("output", String
            .format(DataBridgeApp.BUILD_INFO_STRING_FORMAT, buildInformation.getBuildDate(), buildInformation.getBuildNumber(), buildInformation.getBuildOs(),
                buildInformation.getBuildUser()), output);
    }

    @Test
    public void testParseCommandLineArgumentsParseException() throws Exception
    {
        String[] arguments = {};
        ReturnValue returnValue = dataBridgeApp.parseCommandLineArguments(arguments, applicationContext);

        assertEquals("returnValue", ReturnValue.FAILURE, returnValue);
    }

    @Test
    public void testReturnValue() throws Exception
    {
        assertEquals(ReturnValue.FAILURE, dataBridgeApp.go(null));
        assertEquals(ReturnValue.FAILURE.getReturnCode(), dataBridgeApp.go(null).getReturnCode());
        assertEquals(ReturnValue.SUCCESS, dataBridgeApp.go(new String[] {}));
        assertEquals(ReturnValue.SUCCESS.getReturnCode(), dataBridgeApp.go(new String[] {}).getReturnCode());
    }

    @Test
    public void testCreateApplicationContext() throws Exception
    {
        ApplicationContext applicationContext = dataBridgeApp.createApplicationContext();
        assertNotNull(applicationContext);
        assertNotNull(applicationContext.getBean(S3Operations.class));
    }

    private String runTestGetSystemOut(Runnable runnable)
    {
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(output);
            System.setOut(printStream);
            runnable.run();

            return output.toString().trim();
        }
        finally
        {
            System.setOut(originalStream);
        }
    }

    /**
     * Extends the abstract DataBridgeApp class so methods in the base class can be tested.
     */
    class MockDataBridgeApp extends DataBridgeApp
    {
        public static final String TEST_APPLICATION_NAME = "testApplicationName";

        @Override
        public ArgumentParser getArgumentParser()
        {
            return new ArgumentParser(TEST_APPLICATION_NAME);
        }

        @Override
        public ReturnValue go(String[] args) throws Exception
        {
            return (args == null ? ReturnValue.FAILURE : ReturnValue.SUCCESS);
        }
    }
}