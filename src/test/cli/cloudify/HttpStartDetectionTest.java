package test.cli.cloudify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;

public class HttpStartDetectionTest extends AbstractCommandTest {
	
	@SuppressWarnings("unused")
	private Machine machineA;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void badUsmServiceDownTest() throws IOException, InterruptedException {
		
		machineA = admin.getProcessingUnits().getProcessingUnit("rest").getInstances()[0].getMachine();
		
		String serviceDir = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/usm/tomcatHttpStartDetection";
		String command = "connect " + restUrl + ";install-service --verbose " + serviceDir + ";exit";
		try {
			
			LogUtils.log("Installing tomcat when port is free");
			runCommand(command);
			assertTrue(admin.getProcessingUnits().getProcessingUnit("tomcat").getStatus().equals(DeploymentStatus.INTACT));
			LogUtils.log("Success");
			LogUtils.log("Uninstalling tomcat");
			command = "connect " + restUrl + ";uninstall-service tomcat;exit";
			runCommand(command);
			assertTrue(admin.getProcessingUnits().getProcessingUnit("tomcat") == null);
			LogUtils.log("Installing tomcat when port is taken");
			Socket socket = new Socket();
			socket.bind(new InetSocketAddress(machineA.getHostAddress(), 8080));
			command = "connect " + restUrl + ";install-service --verbose " + serviceDir + ";exit";
			runCommand(command);
			assertTrue("tomcat service should not be installed", admin.getProcessingUnits().getProcessingUnit("tomcat").getStatus().equals(DeploymentStatus.BROKEN));
			socket.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
			super.afterTest();
		} catch (InterruptedException e) {
			e.printStackTrace();
			super.afterTest();
		}
	
	}

}
