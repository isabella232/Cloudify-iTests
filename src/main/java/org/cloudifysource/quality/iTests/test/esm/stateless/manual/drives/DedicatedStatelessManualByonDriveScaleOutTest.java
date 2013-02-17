package org.cloudifysource.quality.iTests.test.esm.stateless.manual.drives;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

public class DedicatedStatelessManualByonDriveScaleOutTest extends AbstractFromXenToByonGSMTest {
	
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
    private static final String DRIVE = "/";
	private static final int MACHINE_DRIVE_CAPACITY = 32000; // checked using df cmd
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    int drivesCapacityInMB = MACHINE_DRIVE_CAPACITY;
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

                scale(new ManualCapacityScaleConfigurer()
                      .driveCapacity(DRIVE, drivesCapacityInMB, MemoryUnit.MEGABYTES)
                      .create())
        );

	    
	    GsmTestUtils.waitForDrives(admin, DRIVE, drivesCapacityInMB);
	    GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,1,1,OPERATION_TIMEOUT);
				
	    drivesCapacityInMB *= 3;
	    pu.scale(new ManualCapacityScaleConfigurer()
                      .driveCapacity(DRIVE, drivesCapacityInMB, MemoryUnit.MEGABYTES)
                      .create());
        GsmTestUtils.waitForDrives(admin ,DRIVE, drivesCapacityInMB);
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu,3,3,OPERATION_TIMEOUT);
		
        assertUndeployAndWait(pu);
	}

}


