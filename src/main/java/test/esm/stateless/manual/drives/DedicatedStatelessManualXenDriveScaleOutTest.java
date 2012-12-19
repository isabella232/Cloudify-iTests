package test.gsm.stateless.manual.drives.xen;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import framework.utils.DeploymentUtils;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedStatelessManualXenDriveScaleOutTest extends AbstractXenGSMTest {

    private static final String DRIVE = "/";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	            
	    int drivesCapacityInMB = XENSERVER_ROOT_DRIVE_CAPACITY - RESERVED_DRIVE_CAPACITY_PER_MACHINE_MEGABYTES;
        
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


