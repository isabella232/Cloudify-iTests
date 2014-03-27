package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import iTests.framework.utils.JGitUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;
import iTests.framework.utils.WebUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.quality.iTests.framework.utils.CommandInvoker;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: yohana
 * Date: 12/8/13
 * Time: 1:44 PM
 */
public class Ec2XAP9xApplicationTest extends NewAbstractCloudTest {
    private static final String LOCAL_GIT_REPO_PATH = ScriptUtils.getBuildPath() + "/git-recipes-" + Ec2XAP9xApplicationTest.class.getSimpleName();
    private static final String XAP9X_MANAGEMENT = "xap-management";
    private static final String SG_VALIDATOR = "sgvalidator";
    private static final String XAP9X_PATH = LOCAL_GIT_REPO_PATH+"/apps/xap9x-tiny/";
    private static final String SG_VALIDATOR_PATH = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/sgvalidator");
    private static final String APP_NAME = "xap9x-tiny";
    private static final String BRANCH_NAME = "master";
    private static final String PUURL = "https://s3-eu-west-1.amazonaws.com/gigaspaces-maven-repository-eu/com/gigaspaces/quality/sgtest/apps/listeners/Space/3.0.0-SNAPSHOT/Space-3.0.0-SNAPSHOT.jar";
    private static final String DEPLOYPU_COMMAND = "deploy-pu pu-using-deploy-pu "+PUURL+" partitioned-sync2backup 1 1 0 0";
    private static final String UNDEPLOYGRID_DEPLOYPU_COMMAND = "undeploy-grid pu-using-deploy-pu";
    private static final String DEPLOYPUBASIC_COMMAND = "deploy-pu-basic "+PUURL;
    private static final String UNDEPLOYGRID_DEPLOYPUBASIC_COMMAND = "undeploy-grid Space-3.0.0-SNAPSHOT.jar";
    private static final String DEPLOYGRID_COMMAND = "deploy-grid pu-using-deploy-grid partitioned-sync2backup 1 1 0 0";
    private static final String UNDEPLOYGRID_DEPLOYGRID_COMMAND = "undeploy-grid pu-using-deploy-grid";
    private static final String DEPLOYGRIDBASIC_COMMAND = "deploy-grid-basic puusing-deploygridbasic";
    private static final String UNDEPLOYGRID_DEPLOYGRIDBASIC_COMMAND = "undeploy-grid puusing-deploygridbasic";

    //This grid is already deployed by the recipe but we need these lines for validateInstance and undeploy command
    private static final String DEPLOYGRID_MYDATAGRID_COMMAND = "deploy-grid-basic myDataGrid"; // partitioned-sync2backup 1 0 1 1
    private static final String UNDEPLOYGRID_MYDATAGRID_COMMAND = "undeploy-grid myDataGrid";

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @BeforeClass(alwaysRun = true)
    protected void prepareTest() throws Exception {
        //Checkout Recipes
        if (!(new File(LOCAL_GIT_REPO_PATH).exists())) {
            String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
            JGitUtils.clone(LOCAL_GIT_REPO_PATH, remotePath, BRANCH_NAME);
        }

        super.bootstrap();
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testXAP9xRecipe() throws Exception {
        InvocationResult result;
        InvokeServiceCommandResponse response;

        //Install and verify xap-management
        String installationLog = installApplicationAndWait(XAP9X_PATH, APP_NAME);//installAndVerify(XAP9X_MANAGEMENT_PATH,XAP9X_MANAGEMENT);
        verifyPUInstallation(APP_NAME);
        Pattern pattern = Pattern.compile("/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}] - xap-management-1 START invoked"); // Regex for ip to match in the log of xap-management installation
        Matcher matcher = pattern.matcher(installationLog);
        if (!matcher.find()) {
            throw new Exception("Unable to find lookuplocator's ip in the log: "+installationLog);
        }
        String managementPrivateIP = matcher.group(0);
        String lookuplocators = managementPrivateIP.substring(managementPrivateIP.indexOf("/")+1, managementPrivateIP.indexOf("]"))+":4242";
        log("Using lookuplocators: "+lookuplocators);
        //Install and verify sgvalidator
        installAndVerify(SG_VALIDATOR_PATH,SG_VALIDATOR);


        // First validate and undeploy myDataGrid that the container-service deploy
        validateInstance(lookuplocators,DEPLOYGRID_MYDATAGRID_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_MYDATAGRID_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        
        //deploy-pu
        response = customCommand(DEPLOYPU_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYPU_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYPU_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        //deploy-pu-basic
        response = customCommand(DEPLOYPUBASIC_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYPUBASIC_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYPUBASIC_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        //deploy-grid
        response = customCommand(DEPLOYGRID_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYGRID_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYGRID_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        //deploy-grid
        response = customCommand(DEPLOYGRIDBASIC_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYGRIDBASIC_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYGRIDBASIC_COMMAND, XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        String uninstallOutput = uninstallAndVerify();
    }

    private void validateInstance(String lookuplocators, String deploypuCommand) throws Exception {
        String gridname;
        String partitions, backups, maxPerVM, maxPerMachine;
        String[] deployCmdArr = deploypuCommand.split(" ");
        if (deployCmdArr[0].equals("deploy-pu")) {
            gridname = deployCmdArr[1];
            partitions = deployCmdArr[4];
            backups = deployCmdArr[5];
            maxPerVM = deployCmdArr[6];
            maxPerMachine = deployCmdArr[7];
        } else if (deployCmdArr[0].equals("deploy-grid")) {
            gridname = deployCmdArr[1];
            partitions = deployCmdArr[3];
            backups = deployCmdArr[4];
            maxPerVM = deployCmdArr[5];
            maxPerMachine = deployCmdArr[6];
        } else if (deployCmdArr[0].equals("deploy-pu-basic")) {
            File file = new File(PUURL);
            gridname = file.getName();
            partitions = "1";
            backups = "0";
            maxPerVM = "1";
            maxPerMachine = "1";
        } else if (deployCmdArr[0].equals("deploy-grid-basic")) {
            gridname = deployCmdArr[1];
            partitions = "1";
            backups = "1";
            maxPerVM = "0";
            maxPerMachine = "0";
        } else {
            throw new Exception("Unknown command. Please recheck the command or add it to validateInstance function.");
        }
        InvokeServiceCommandResponse response;
        InvocationResult result;
        String appName = "default";
        String numOfInstances = String.valueOf(Integer.valueOf(partitions)*(1+Integer.valueOf(backups)));

        response = customCommand("get-datagrid-instances "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),numOfInstances);

        response = customCommand("get-datagrid-deploymentstatus "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(), String.valueOf(DeploymentStatus.INTACT));

        response = customCommand("get-datagrid-partitions "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),partitions);

        response = customCommand("get-datagrid-backups "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),backups);

        response = customCommand("get-datagrid-maxinstancespermachine "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),maxPerMachine);

        response = customCommand("get-datagrid-maxinstancespervm "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),maxPerVM);
    }

    private String uninstallAndVerify() throws Exception {
        uninstallServiceAndWait(SG_VALIDATOR);
        String log = uninstallApplicationAndWait(APP_NAME, false, 25);
        super.scanForLeakedAgentNodes();
        return log;
    }

    private InvokeServiceCommandResponse customCommand(String commandStr, String serviceName, String appName) throws IOException, InterruptedException, RestClientException {
        String[] commandArr = commandStr.split(" ");
        String command=commandStr;
        List<String> params = new ArrayList<String>();
        log("Invoking command: "+command);
        if (commandArr.length > 1) {
            command = commandArr[0];
            for (int i=1 ; i<commandArr.length ; i++) {
                params.add(commandArr[i]);
            }
        }
        CommandInvoker commandInvoker = new CommandInvoker(getRestUrl());
        return commandInvoker.restInvokeServiceCommand(appName, serviceName, command, params);
    }

    private InvocationResult getCustomCommandResult(InvokeServiceCommandResponse response) throws Exception {
        Map<String, InvocationResult> results = parseInvocationResults(response.getInvocationResultPerInstance());
        if (results.values().size() != 1) {
            throw new Exception("Custom command returns more than 1 result");
        }
        return ((InvocationResult)results.values().toArray()[0]);
    }

    private String installAndVerify(String servicePath, String serviceName) throws Exception {
        String result = installServiceAndWait(servicePath, serviceName,25);

        verifyPUInstallation(serviceName);
        return result;
    }

    private void verifyPUInstallation(String appName) throws Exception {
        LogUtils.log("verifying successful installation");
        String restUrl = getRestUrl();

        final URL appURL = new URL(restUrl + "/admin/ProcessingUnits/Names/"+appName+"");
        AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(appURL));
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

    protected void log(String msg) {
        LogUtils.log(msg);
    }
}
