package org.cloudifysource.quality.iTests.test.esm.stateless.manual.memory;

import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.GsmTestUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.CloudTestUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogEntryMatchers;

public abstract class AbstractStatelessManualByonCleanupTest extends AbstractFromXenToByonGSMTest {

	final private String expectedLogStatement;
	final private String cloudDriverClassName;
	
	protected AbstractStatelessManualByonCleanupTest(String expectedLogStatement, String cloudDriverClassName) {
		this.expectedLogStatement = expectedLogStatement;
		this.cloudDriverClassName = cloudDriverClassName;
	}
	
    @Override
    public void beforeBootstrap() throws IOException {
        CloudTestUtils.replaceCloudDriverImplementation(
        		getService(),
        		ByonProvisioningDriver.class.getName(),	//old class
        		cloudDriverClassName, //new class
        		"location-aware-provisioning-byon", "2.2-SNAPSHOT" //jar
        );
    }
    
	protected void testCloudCleanup() {
		
		File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
		 // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);	    
		final ProcessingUnit pu = super.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	  .memoryCapacity(2, MemoryUnit.GIGABYTES)
                      .create())
	    );
	    
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, 2, OPERATION_TIMEOUT);
		
	    assertUndeployAndWait(pu);
       
        repetitiveAssertOnServiceUninstalledInvoked();
	}
	
	private void repetitiveAssertOnServiceUninstalledInvoked() {
    	
    	final ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        LogUtils.log("Waiting for 1 "+ expectedLogStatement + " log entry before undeploying PU");
        final LogEntryMatcher logMatcher = LogEntryMatchers.containsString(expectedLogStatement);
        repetitiveAssertTrue("Expected " + expectedLogStatement +" log", new RepetitiveConditionProvider() {
            
            @Override
            public boolean getCondition() {
                final LogEntries logEntries = esm.logEntries(logMatcher);
                final int count = logEntries.logEntries().size();
                LogUtils.log("Exepcted at least one "+ expectedLogStatement + " log entries. Actual :" + count);
                return count > 0;
            }
        } , OPERATION_TIMEOUT);
    }
}
