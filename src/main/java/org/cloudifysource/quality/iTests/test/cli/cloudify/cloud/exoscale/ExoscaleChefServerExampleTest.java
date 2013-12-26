package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.exoscale;

import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;
import iTests.framework.utils.WebUtils;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.quality.iTests.framework.utils.CommandInvoker;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: yohana
 * Date: 11/28/13
 * Time: 3:07 PM
 */
public class ExoscaleChefServerExampleTest extends NewAbstractCloudTest {
    private static final String SERVICE_NAME = "chef-server";
    private static final String APP_NAME = "default";
    private static final String UPDATECOOKBOOKS_COMMAND = "updateCookbooks tar http://repository.cloudifysource.org/chef-cookbooks/mysql_cookbook_with_deps.tgz \"\"";
    private static final String LISTCOOKBOOKS_COMMAND = "listCookbooks";
    private static final String EXPECTED_RESULT_UPDATECOOKBOOKS_COMMAND = null;
    private static final String EXPECTED_LISTCOOKBOOKES_AFTER_UPDATECOOKBOOKS_COMMAND = "\n" +
            "apt            1.4.2\n" +
            "chef_handler   1.0.6\n" +
            "cloudify       0.1.0\n" +
            "mysql          1.2.4\n" +
            "ohai           1.1.8\n" +
            "openssl        1.0.0\n" +
            "windows        1.2.12\n";
    private static final String CLEANUPCOOKBOOKS_COMMAND = "cleanupCookbooks";
    private static final String EXPECTED_RESULT_CLEANUPCOOKBOOKS_COMMAND = "0";
    private static final String CREATENODE_COMMAND = "createNode newNode";
    private static final String EXPECTED_RESULT_CREATENODE_COMMAND = "\n" +
            "Updated Node newNode!\n";
    private static final String KNIFE_COMMAND = "knife node list";
    private static final String EXPECTED_RESULT_KNIFE_COMMAND = "\n" +
            "newNode\n";
    private static final String CLEANUPNODE_COMMAND = "cleanupNode newNode";
    private static final String EXPECTED_RESULT_CLEANUPNODE_COMMAND = "newNode cleaned up";

    @Override
    protected String getCloudName() {
        return "exoscale";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @BeforeClass (alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testChef() throws Exception {
        installAndVerify();

        InvokeServiceCommandResponse response;
        String result;

        response = customCommand(UPDATECOOKBOOKS_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_RESULT_UPDATECOOKBOOKS_COMMAND);

        response = customCommand(LISTCOOKBOOKS_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_LISTCOOKBOOKES_AFTER_UPDATECOOKBOOKS_COMMAND);

        response = customCommand(CLEANUPCOOKBOOKS_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_RESULT_CLEANUPCOOKBOOKS_COMMAND);

        response = customCommand(CREATENODE_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_RESULT_CREATENODE_COMMAND);

        response = customCommand(KNIFE_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_RESULT_KNIFE_COMMAND);

        response = customCommand(CLEANUPNODE_COMMAND);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result, EXPECTED_RESULT_CLEANUPNODE_COMMAND);

        uninstallAndVerify();
    }

    private void uninstallAndVerify() throws Exception {
        uninstallServiceAndWait(SERVICE_NAME);
        super.scanForLeakedAgentNodes();
    }

    private InvokeServiceCommandResponse customCommand(String commandStr) throws IOException, InterruptedException, RestClientException {
        String[] commandArr = commandStr.split(" ");
        String command=commandStr;
        List<String> params = new ArrayList<String>();
        if (commandArr.length > 1) {
            command = commandArr[0];
            LogUtils.log("Invoking command: "+command);
            for (int i=1 ; i<commandArr.length ; i++) {
                params.add(commandArr[i]);
            }
        }
        CommandInvoker commandInvoker = new CommandInvoker(getRestUrl());
        return commandInvoker.restInvokeServiceCommand(APP_NAME, SERVICE_NAME, command, params);
    }

    private String getCustomCommandResult(InvokeServiceCommandResponse response) throws Exception {
        Map<String, InvocationResult> results = parseInvocationResults(response.getInvocationResultPerInstance());
        if (results.values().size() != 1) {
            throw new Exception("Custom command returns more than 1 result");
        }
        return ((InvocationResult)results.values().toArray()[0]).getResult();
    }

    private void installAndVerify() throws Exception {
        installServiceAndWait(ScriptUtils.getBuildRecipesServicesPath()+"/"+SERVICE_NAME,SERVICE_NAME, 25); // timeout in min
        verifyApplicationInstallation(SERVICE_NAME);
    }

    private void verifyApplicationInstallation(String appName) throws Exception {
        LogUtils.log("verifying successful installation");
        String restUrl = getRestUrl();

        final URL chefServerUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/"+appName+"");
        AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(chefServerUrl));
    }



    private Map<String, InvocationResult> parseInvocationResults(
            final Map<String, Map<String, String>> restInvocationResultsPerInstance) {

        final Map<String, InvocationResult> invocationResultsMap = new LinkedHashMap<String, InvocationResult>();

        for (final Map.Entry<String, Map<String, String>> entry : restInvocationResultsPerInstance.entrySet()) {
            final String instanceName = entry.getKey();
            final Map<String, String> restInvocationResult = entry.getValue();
            invocationResultsMap.put(instanceName, parseInvocationResult(restInvocationResult));
        }

        return invocationResultsMap;
    }


    private InvocationResult parseInvocationResult(final Map<String, String> restInvocationResult) {

        InvocationResult invocationResult = null;
        if (restInvocationResult != null) {
            invocationResult = InvocationResult.createInvocationResult(restInvocationResult);
        }

        return invocationResult;
    }
}
