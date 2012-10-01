package test.cli.cloudify.cloud.byon;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.util.LogUtils;


/**
 * this test is exactly like the super test, except that the hosts are specified as host names instead of IP's.
 * @author elip
 *
 */
public class MultipleTemplatesWithNamesAsIPsTest extends MultipleMachineTemplatesTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}

	@Override
	public void customizeCloud() throws Exception {	
		String[] machines = getService().getMachines();
		String[] machinesHostNames = new String[machines.length];
		LogUtils.log("converting every other address to host name");
		for (int i = 0 ; i < machines.length ; i++) {
			String host = machines[i];

			String[] chars = host.split("\\.");

			byte[] add = new byte[chars.length];
			for (int j = 0 ; j < chars.length ; j++) {
				add[j] =(byte) ((int) Integer.valueOf(chars[j]));
			}

			try {
				InetAddress byAddress = InetAddress.getByAddress(add);
				String hostName = byAddress.getHostName();
				LogUtils.log("IP address " + host + " was resolved to " + hostName);
				machinesHostNames[i] = hostName;
			} catch (UnknownHostException e) {
				throw new IllegalStateException("could not resolve host name of ip " + host);
			}

		}
		getService().setMachines(machinesHostNames);
		super.customizeCloud();
	}


	@Override
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, priority = 1)
	public void testPetclinic() throws Exception {
		super.testPetclinic();
	}


	/**
	 * Gets the address of the machine on which the given processing unit is deployed. 
	 * @param puName The name of the processing unit to look for
	 * @return The address of the machine on which the processing unit is deployed.
	 */
	@Override
	protected String getPuHost(final String puName) {
		ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(puName);
		Assert.assertNotNull(pu.getInstances()[0], puName + " processing unit is not found");
		return pu.getInstances()[0].getMachine().getHostName();	
	}

}
