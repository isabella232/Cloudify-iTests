package test.cli.cloudify;

import java.io.IOException;

import org.openspaces.admin.machine.Machine;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import framework.tools.SGTestHelper;

public class BadUSMServiceDownAfterUninstallApplicationTest extends AbstractCommandTest {
	
	@SuppressWarnings("unused")
	private Machine machineA;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void badUsmServiceDownTest() throws IOException, InterruptedException {
		
		String serviceDir = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/badUsmServices/simpleApplication";
		String command = "connect " + restUrl + ";install-application --verbose " + serviceDir + ";exit";
		try {
			runCommand(command);
			machineA = admin.getMachines().getMachines()[0];
			
			command = "connect " + restUrl + ";" + "uninstall-application simple;exit";
			runCommand(command);
			
			RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
				@Override
				public boolean getCondition() {
					return admin.getProcessingUnits().getProcessingUnit("simple") == null;
				}
			};
			AssertUtils.repetitiveAssertTrue(null, condition, 10000);
		} catch (IOException e) {
			e.printStackTrace();
			super.afterTest();
		} catch (InterruptedException e) {
			e.printStackTrace();
			super.afterTest();
		}
	
	}

}
