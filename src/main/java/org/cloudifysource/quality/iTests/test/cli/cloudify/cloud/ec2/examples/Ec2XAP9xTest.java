package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.*;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: yohana
 * Date: 12/8/13
 * Time: 1:44 PM
 */
public class Ec2XAP9xTest extends NewAbstractCloudTest {
    private static final String LOCAL_GIT_REPO_PATH = ScriptUtils.getBuildPath() + "/git-recipes-" + Ec2XAP9xTest.class.getSimpleName();;
    private static final String XAP9X_MANAGEMENT = "xap-management";
    private static final String XAP9X_CONTAINER = "xap-container";
    private static final String SG_VALIDATOR = "sgvalidator";
    private static final String XAP9X_MANAGEMENT_PATH = LOCAL_GIT_REPO_PATH+"/services/xap9x/xap-management";
    private static final String XAP9X_CONTAINER_PATH = LOCAL_GIT_REPO_PATH+"/services/xap9x/xap-container";
    private static final String SG_VALIDATOR_PATH = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/sgvalidator");
    private static final String APP_NAME = "default";
    private static final String BRANCH_NAME = "master";
    private static final String PUURL = "https://s3-eu-west-1.amazonaws.com/gigaspaces-maven-repository-eu/com/gigaspaces/quality/sgtest/apps/listeners/Space/3.0.0-SNAPSHOT/Space-3.0.0-SNAPSHOT.jar";
    private static final String DEPLOYPU_COMMAND = "deploy-pu pu-using-deploy-pu "+PUURL+" partitioned-sync2backup 2 1 2 2";
    private static final String UNDEPLOYGRID_DEPLOYPU_COMMAND = "undeploy-grid pu-using-deploy-pu";
    private static final String DEPLOYPUBASIC_COMMAND = "deploy-pu-basic "+PUURL;
    private static final String UNDEPLOYGRID_DEPLOYPUBASIC_COMMAND = "undeploy-grid Space-3.0.0-SNAPSHOT.jar";
    private static final String DEPLOYGRID_COMMAND = "deploy-grid pu-using-deploy-grid partitioned-sync2backup 1 0 1 1";
    private static final String UNDEPLOYGRID_DEPLOYGRID_COMMAND = "undeploy-grid pu-using-deploy-grid";

    protected static final String CREDENTIALS_FOLDER = System.getProperty("iTests.credentialsFolder",
            SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
    private static File propsFile = new File(CREDENTIALS_FOLDER + "/xap/xap.properties");

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
        String localGitRepoPath = ScriptUtils.getBuildPath() + "/git-recipes-" + this.getClass().getSimpleName();
        if (!(new File(localGitRepoPath).exists())) {
            String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
            JGitUtils.clone(localGitRepoPath, remotePath, BRANCH_NAME);
        }

        //Get XAP license from properties file
        Properties props;
        try {
            props = IOUtils.readPropertiesFromFile(propsFile);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed reading properties file : " + e.getMessage());
        }
        String xapLicense = props.getProperty("xap_license");

        //Add license to xap-management
        IOUtils.replaceTextInFile(localGitRepoPath+"/services/xap9x/xap-management/xap-management-service.properties",
                "license=\"\"",
                "license=\""+xapLicense+"\"");
        //Add license to xap-container
        IOUtils.replaceTextInFile(localGitRepoPath+"/services/xap9x/xap-container/xap-container-service.properties",
                "license=\"\"",
                "license=\""+xapLicense+"\"");

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
        String installationLog = installAndVerify(XAP9X_MANAGEMENT_PATH,XAP9X_MANAGEMENT);
        Pattern pattern = Pattern.compile("/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}]"); // Regex for ip to match in the log of xap-management installation
        Matcher matcher = pattern.matcher(installationLog);
        if (!matcher.find()) {
            throw new Exception("Unable to find lookuplocator's ip in the log: "+installationLog);
        }
        String lookuplocators = matcher.group(0).replace("/","").replace("]","")+":4242";

        //Install and verify xap-container
        installAndVerify(XAP9X_CONTAINER_PATH,XAP9X_CONTAINER);
        //Install and verify sgvalidator
        installAndVerify(SG_VALIDATOR_PATH,SG_VALIDATOR);

        //deploy-pu
        response = customCommand(DEPLOYPU_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYPU_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYPU_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        //deploy-pu-basic
        response = customCommand(DEPLOYPUBASIC_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYPUBASIC_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYPUBASIC_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        //deploy-grid
        response = customCommand(DEPLOYGRID_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        validateInstance(lookuplocators,DEPLOYGRID_COMMAND);
        //undeploy-grid
        response = customCommand(UNDEPLOYGRID_DEPLOYGRID_COMMAND, XAP9X_MANAGEMENT);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

        uninstallAndVerify();
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
        } else {
            throw new Exception("Unknown command. Please recheck the command or add it to validateInstance function.");
        }
        InvokeServiceCommandResponse response;
        InvocationResult result;

        String numOfInstances = String.valueOf(Integer.valueOf(partitions)*(1+Integer.valueOf(backups)));

        response = customCommand("get-datagrid-instances "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),numOfInstances);

        response = customCommand("get-datagrid-deploymentstatus "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(), String.valueOf(DeploymentStatus.INTACT));

        response = customCommand("get-datagrid-partitions "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),partitions);

        response = customCommand("get-datagrid-backups "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),backups);

        response = customCommand("get-datagrid-maxinstancespermachine "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),maxPerMachine);

        response = customCommand("get-datagrid-maxinstancespervm "+gridname+" "+lookuplocators+" "+numOfInstances, SG_VALIDATOR);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        Assert.assertEquals(result.getResult(),maxPerVM);
    }

    private void uninstallAndVerify() throws Exception {
        uninstallServiceAndWait(SG_VALIDATOR);
        uninstallServiceAndWait(XAP9X_CONTAINER);
        uninstallServiceAndWait(XAP9X_MANAGEMENT);
        super.scanForLeakedAgentNodes();
    }

    private InvokeServiceCommandResponse customCommand(String commandStr, String serviceName) throws IOException, InterruptedException, RestClientException {
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
        return commandInvoker.restInvokeServiceCommand(APP_NAME, serviceName, command, params);
    }

    private InvocationResult getCustomCommandResult(InvokeServiceCommandResponse response) throws Exception {
        Map<String, InvocationResult> results = parseInvocationResults(response.getInvocationResultPerInstance());
        if (results.values().size() != 1) {
            throw new Exception("Custom command returns more than 1 result");
        }
        return ((InvocationResult)results.values().toArray()[0]);
    }

    private String installAndVerify(String servicePath, String serviceName) throws Exception {
        String result = installServiceAndWait(servicePath, serviceName,15);

        verifyPUInstallation(serviceName);
        return result;
    }

    private void verifyPUInstallation(String appName) throws Exception {
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

    protected void log(String msg) {
        LogUtils.log(msg);
    }
}
