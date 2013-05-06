package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.Assert;

import iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.usm.USMTestUtils;

/**
 * 
 * @author yael
 *
 */
public abstract class OverridesTest extends AbstractLocalCloudTest {
	protected static final long PU_TIMEOUT = 5 * 60;
	protected static final long PU_STATE_TIMEOUT = 60;
	protected static final int STATUS_OK = 200;
	
	/**
	 * 
	 * @param url the rest URL.
	 * @param dslTypeName the name of the DSL type (application or service).
	 * @param dslDirPath the path to the DSL directory.
	 * @param overridesFilePath the path to the overrides file.
	 */
	protected void install(final String url, final String dslTypeName,
			final String dslDirPath, final String overridesFilePath) {
		String command = "connect " + url + ";install-" + dslTypeName + " --verbose";
		if (overridesFilePath != null) {
			command += " -overrides " + overridesFilePath;
		} 
		try {
			runCommand(command + " " + dslDirPath);
		} catch (final Exception e) {
			LogUtils.log("Failed to install " + dslTypeName + " " + dslDirPath, e);
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param puName the name of the processing unit.
	 * @return the found processing unit.
	 */
	protected ProcessingUnit getProcessingUnit(final String puName) {
		return admin.getProcessingUnits().waitFor(puName, PU_TIMEOUT, TimeUnit.SECONDS);
	}

	/**
	 * 
	 * @param processingUnit the processing unit.
	 */
	protected void assertProcessingUnit(final ProcessingUnit processingUnit) {
		Assert.assertNotNull(processingUnit);
		String puName = processingUnit.getName();
		Assert.assertTrue(processingUnit.waitFor(1, PU_TIMEOUT, TimeUnit.SECONDS),
				"Instance of '" + puName + "' service was not found");
		Assert.assertTrue(USMTestUtils.waitForPuRunningState(puName,
				PU_STATE_TIMEOUT, TimeUnit.SECONDS, admin));		
	}
	
	/**
	 * 
	 * @param processingUnit the processing unit.
	 */
	protected void assertServiceOverridenFields(final ProcessingUnit processingUnit) {
		ProcessingUnitInstance processingUnitInstance = processingUnit.getInstances()[0];
		final Collection<ServiceDetails> allServiceDetails = processingUnitInstance
				.getServiceDetailsByServiceId().values();
		final Map<String, Object> allDetails = new HashMap<String, Object>();
		for (final ServiceDetails serviceDetails : allServiceDetails) {
			allDetails.putAll(serviceDetails.getAttributes());
		}
		for (final Entry<String, Object> details : getExpectedServiceFields().entrySet()) {
			final String detailKey = details.getKey();
			assertTrue("Missing details entry: " + detailKey,
					allDetails.containsKey(detailKey));
			assertEquals(details.getValue(), allDetails.get(detailKey));
		}		
	}

	/**
	 * 
	 * @return the expected service's fields.
	 */
	protected abstract Map<String, Object> getExpectedServiceFields();
	
	
	protected void assertService(String applicationName, String serviceName) {
		String output = listServices(applicationName, false);
		assertTrue("list-services command output doesn't conatin "
				+ serviceName + ", output: " + output,
				output.contains(serviceName));
	}
	
	protected void assertApplication(String applicationName) {
		String output = listApplications(false);
		assertTrue("list-applications command output doesn't conatin "
				+ applicationName + ", output: " + output,
				output.contains(applicationName));
	}
}
