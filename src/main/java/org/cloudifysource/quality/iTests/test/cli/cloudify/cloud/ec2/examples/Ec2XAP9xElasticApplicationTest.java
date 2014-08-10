package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import com.j_spaces.kernel.PlatformVersion;
import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.*;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.quality.iTests.framework.utils.CommandInvoker;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.GSRestClient;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: yohana
 * Date: 12/8/13
 * Time: 1:44 PM
 */
public class Ec2XAP9xElasticApplicationTest extends NewAbstractCloudTest {
    private static final String LOCAL_GIT_REPO_PATH = ScriptUtils.getBuildPath() + "/git-recipes-" + Ec2XAP9xApplicationTest.class.getSimpleName();
    private static final String XAP9X_MANAGEMENT = "xap-management";
    private static final String SG_VALIDATOR = "sg-validator";
    private static final String XAP9X_PATH = LOCAL_GIT_REPO_PATH+"/apps/xap9x-elastic/";
    private static final String SG_VALIDATOR_PATH = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/sgvalidator");
    private static final String APP_NAME = "xap9x-elastic";
    private static final String BRANCH_NAME = "master";

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
        if (!(new File(LOCAL_GIT_REPO_PATH).exists())) {
            String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
            JGitUtils.clone(LOCAL_GIT_REPO_PATH, remotePath, BRANCH_NAME);
        }

        //Get XAP license from properties file
        Properties props;
        try {
                props = IOUtils.readPropertiesFromFile(propsFile);
            } catch (final Exception e) {
                throw new IllegalStateException("Failed reading properties file : " + e.getMessage());
            }
        String xapLicense = props.getProperty("xap10_license");

        //Add license to xap-management and change license in properties to be not null so the recipe will replace it
        IOUtils.replaceTextInFile(LOCAL_GIT_REPO_PATH + "/apps/xap9x-elastic/xap-management/templates/gslicense.xml",
                "\\$\\{license\\}",
                xapLicense);
        IOUtils.replaceTextInFile(LOCAL_GIT_REPO_PATH + "/apps/xap9x-elastic/xap-management/xap-management-service.properties",
                "license=\"\"",
                "license=\"overridden\"");

        //Add license to xap-datagrid and change license in properties to be not null so the recipe will replace it
        IOUtils.replaceTextInFile(LOCAL_GIT_REPO_PATH + "/apps/xap9x-elastic/xap-datagrid/templates/gslicense.xml",
                "\\$\\{license\\}",
                xapLicense);
        IOUtils.replaceTextInFile(LOCAL_GIT_REPO_PATH + "/apps/xap9x-elastic/xap-datagrid/xap-datagrid-service.properties",
                "license=\"\"",
                "license=\"overridden\"");




        super.bootstrap();
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();

        String oldTemplate = "MEDIUM_UBUNTU";
        String newTemplate = "MEDIUM_LINUX";
        getService().getAdditionalPropsToReplace().put(oldTemplate, newTemplate);

        String oldImageId = "imageId ubuntuImageId";
        String newImageId = "imageId linuxImageId";
        getService().getAdditionalPropsToReplace().put(oldImageId, newImageId);

        String oldRemoteDirectory = "remoteDirectory \"/home/ubuntu/gs-files\"";
        String newRemoteDirectory = "remoteDirectory \"/home/ec2-user/gs-files\"";
        getService().getAdditionalPropsToReplace().put(oldRemoteDirectory, newRemoteDirectory);

        String oldUserName = "username \"ubuntu\"";
        String newUserName = "username \"ec2-user\"";
        getService().getAdditionalPropsToReplace().put(oldUserName, newUserName);
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testXAP9xRecipe() throws Exception {
        System.setProperty(CloudifyConstants.SYSTEM_PROPERTY_REST_CLIENT_SOCKET_TIMEOUT_MILLIS, "600000");

        InvocationResult result;
        InvokeServiceCommandResponse response;

        String lookuplocators = installAndReturnLookupLocators();

        log("Using lookuplocators: "+lookuplocators);

        //Validate deployed myDataGrid
        validateInstance(lookuplocators, "myDataGrid", "13", "1", "1", "1");

        testCurrent(lookuplocators);

        scaleOut("myDataGrid", lookuplocators);

        testScaleOut(lookuplocators);


        scaleIn("myDataGrid", lookuplocators);

        testScaleIn();

        response = customCommand("undeploy-grid myDataGrid", XAP9X_MANAGEMENT, APP_NAME);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);


        uninstallAndVerify();
    }

    // Install xap9x-elastic and sg-validator
    private String installAndReturnLookupLocators() throws Exception {

        //Install and verify xap-management
        String installationLog = installApplicationAndWait(XAP9X_PATH, APP_NAME, 25, false);
        verifyInstallation(APP_NAME);
        Pattern pattern = Pattern.compile("/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}] - xap-management-1 START invoked"); // Regex for ip to match in the log of xap-management installation
        Matcher matcher = pattern.matcher(installationLog);
        if (!matcher.find()) {
            throw new Exception("Unable to find lookuplocator's ip in the log: "+installationLog);
        }
        String managementPrivateIP = matcher.group(0);
        String lookuplocators = managementPrivateIP.substring(managementPrivateIP.indexOf("/")+1, managementPrivateIP.indexOf("]"))+":4242";

        //Install and verify sgvalidator
        installAndVerify(SG_VALIDATOR_PATH,SG_VALIDATOR);

        return lookuplocators;
    }

    /*
        1. Check that there are two instances of xap-datagrid service and it's status is INTACT
        2. Validate that myDataGrid is deployed on 2 machines (expectedDataGridMachines), try for maximum 3 minutes while checking every 20 seconds
        3. Validate that each machine contains 13 instances of myDataGrid, try for maximum 3 minutes while checking every 20 seconds
     */
    private void testCurrent(final String lookuplocators) throws Exception {
        final int expectedDataGridMachines = 2;
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());

        log("Checking xap-datagrid instances. Expecting: "+expectedDataGridMachines);
        final Map<String, Object> xap_datagrid = client.getAdmin("ProcessingUnits/Names/"+APP_NAME+".xap-datagrid");
        Assert.assertEquals(xap_datagrid.get("Instances-Size"), expectedDataGridMachines);
        Assert.assertEquals(xap_datagrid.get("Status-Enumerator"), "INTACT");

        log("Checking that myDataGrid is deployed on "+expectedDataGridMachines+" machines");
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    public InvokeServiceCommandResponse resp;
                    public InvocationResult res;

                    @Override
                    public boolean condition() throws Exception {
                        resp = customCommand("getMachinesCount myDataGrid " + lookuplocators, SG_VALIDATOR, "default");
                        res = getCustomCommandResult(resp);
                        if (!(res.getInvocationStatus().equals(CloudifyConstants.InvocationStatus.SUCCESS))) {
                            log("SEVERE: getMachinesCount command failed!");
                            return false;
                        }
                        return Integer.valueOf(res.getResult()).equals(expectedDataGridMachines);
                    }
                },
                TimeUnit.MINUTES.toMillis(3),
                TimeUnit.SECONDS.toMillis(20));

        log("Checking that on each machine that myDataGrid is deployed on there are 13 instances of myDataGrid");
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    @Override
                    public boolean condition() throws Exception {
                        InvokeServiceCommandResponse response = customCommand("getSpaceMachines myDataGrid " + lookuplocators, SG_VALIDATOR, "default");
                        InvocationResult result = getCustomCommandResult(response);
                        if (!(result.getInvocationStatus().equals(CloudifyConstants.InvocationStatus.SUCCESS))) {
                            log("SEVERE: getSpaceMachines command failed!");
                            return false;
                        }

                        String spaceMachinesString = result.getResult();

                        String[] spaceMachines = spaceMachinesString.split(";");
                        Assert.assertEquals(spaceMachines.length, expectedDataGridMachines);

                        int total=0;
                        for (String spaceMachine : spaceMachines) {
                            String[] entry = spaceMachine.split(":");
                            int spaceInstancesForMachine = Integer.valueOf(entry[1]).intValue();
                            if (spaceInstancesForMachine != 13) {
                                return false;
                            }
                            total += spaceInstancesForMachine;
                        }

                        if (total != 26) {
                            return false;
                        }

                        return true;
                    }
                },
                TimeUnit.MINUTES.toMillis(3),
                TimeUnit.SECONDS.toMillis(20));
    }

    // Write 1,600,000 objects to the space causing at least one of the GSCs to have more than 75% memory in use
    private void scaleOut(String gridname, String lookuplocators) throws Exception {
        InvokeServiceCommandResponse response;
        InvocationResult result;
        String appName = "default";

        response = customCommand("write "+gridname+" "+lookuplocators+" 10 130000", SG_VALIDATOR, appName);
        result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);
        log("Finished writing to space");
    }

    /*
        1. Check that there are three instances of xap-datagrid service and it's status is INTACT, try every 30 seconds for 10 minutes
            10 minutes: Waiting for the scaleout to happen
        2. Validate that myDataGrid is deployed on 3 machines (expectedDataGridMachines), try for maximum 5 minutes while checking every 20 seconds
        3. Validate that each machine contains 8 or 9 instances of myDataGrid, try for maximum 5 minutes while checking every 20 seconds
            This is caused by the rebalancing
     */
    private void testScaleOut(final String lookuplocators) throws Exception {
        final int expectedDataGridMachines = 3;
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());

        log("Checking xap-datagrid instances. Expecting: "+expectedDataGridMachines);
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    @Override
                    public boolean condition() throws Exception {
                        Map<String, Object> xap_datagrid = client.getAdmin("ProcessingUnits/Names/"+APP_NAME+".xap-datagrid");
                        return (xap_datagrid.get("Instances-Size").equals(expectedDataGridMachines)) && (xap_datagrid.get("Status-Enumerator").equals("INTACT"));
                    }
                },
                TimeUnit.MINUTES.toMillis(10),
                TimeUnit.SECONDS.toMillis(30));

        log("Checking that myDataGrid is deployed on "+expectedDataGridMachines+" machines");
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    public InvokeServiceCommandResponse resp;
                    public InvocationResult res;

                    @Override
                    public boolean condition() throws Exception {
                        resp = customCommand("getMachinesCount myDataGrid " + lookuplocators, SG_VALIDATOR, "default");
                        res = getCustomCommandResult(resp);
                        if (!(res.getInvocationStatus().equals(CloudifyConstants.InvocationStatus.SUCCESS))) {
                            log("SEVERE: getMachinesCount command failed!");
                            return false;
                        }
                        return Integer.valueOf(res.getResult()).equals(expectedDataGridMachines);
                    }
                },
                TimeUnit.MINUTES.toMillis(5),
                TimeUnit.SECONDS.toMillis(20));


        log("Checking that on each machine that myDataGrid is deployed on there are 8 or 9 instances of myDataGrid");
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    // 26/3 : 8, 9, 9
                    List<Integer> allowedValues= Arrays.asList(8,9);
                    @Override
                    public boolean condition() throws Exception {
                        InvokeServiceCommandResponse response = customCommand("getSpaceMachines myDataGrid " + lookuplocators, SG_VALIDATOR, "default");
                        InvocationResult result = getCustomCommandResult(response);
                        if (!(result.getInvocationStatus().equals(CloudifyConstants.InvocationStatus.SUCCESS))) {
                            log("SEVERE: getSpaceMachines command failed!");
                            return false;
                        }

                        String spaceMachinesString = result.getResult();

                        String[] spaceMachines = spaceMachinesString.split(";");
                        Assert.assertEquals(spaceMachines.length, expectedDataGridMachines);
                        int total=0;
                        for (String spaceMachine : spaceMachines) {
                            String[] entry = spaceMachine.split(":");
                            int spaceInstancesForMachine = Integer.valueOf(entry[1]).intValue();
                            if (! allowedValues.contains(spaceInstancesForMachine)) {
                                return false;
                            }
                            total += spaceInstancesForMachine;
                        }

                        if (total != 26) {
                            return false;
                        }

                        return true;
                    }
                },
                TimeUnit.MINUTES.toMillis(5),
                TimeUnit.SECONDS.toMillis(20));
        // Get the status of each instance and check if it is intact!
    }

    /*
    Clean myDataGrid space causing the memory usage to go down below 35%
     */
    private void scaleIn(String myDataGrid, String lookuplocators) throws Exception {
        InvokeServiceCommandResponse response = customCommand("clearSpace myDataGrid " + lookuplocators, SG_VALIDATOR, "default");
        InvocationResult result = getCustomCommandResult(response);
        Assert.assertEquals(result.getInvocationStatus(), CloudifyConstants.InvocationStatus.SUCCESS);

    }

    private void testScaleIn() throws Exception {
        final int expectedDataGridMachines = 2;
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());

        log("Checking xap-datagrid instances. Expecting: "+expectedDataGridMachines);
        AssertUtils.repetitiveAssertTrue("", new AssertUtils.AssertConditionProvider() {
                    @Override
                    public boolean condition() throws Exception {
                        Map<String, Object> xap_datagrid = client.getAdmin("ProcessingUnits/Names/"+APP_NAME+".xap-datagrid");
                        return (xap_datagrid.get("Instances-Size").equals(expectedDataGridMachines)) && (xap_datagrid.get("Status-Enumerator").equals("INTACT"));
                    }
                },
                TimeUnit.MINUTES.toMillis(10),
                TimeUnit.SECONDS.toMillis(30));
    }

    private void validateInstance(String lookuplocators, String gridname, String partitions, String backups, String maxPerMachine, String maxPerVM) throws Exception {
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

        verifyInstallation(serviceName);
        return result;
    }

    private void verifyInstallation(String appName) throws Exception {
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
