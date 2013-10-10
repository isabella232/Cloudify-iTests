package org.cloudifysource.esc.util

import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver

import java.io.File;
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.cloudifysource.domain.cloud.Cloud
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.openspaces.admin.Admin
import org.cloudifysource.esc.driver.provisioning.BaseComputeDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver

public class ProvisioningDriverClass extends BaseComputeDriver {

	DefaultProvisioningDriver dpd = new DefaultProvisioningDriver();

	/**************
	 * Called once on startup of the cloud driver, passing the cloud configuration to it.
	 *
	 * @param configuration
	 *            the driver configuration.
	 * @throws CloudProvisioningException
	 *             Indicates invalid cloud configuration
	 */
	public void setConfig(final ComputeDriverConfiguration configuration)
	throws CloudProvisioningException {
		dpd.setConfig(configuration);
	}


	/**
	 * A compute driver may choose to give access to the internal 'context' which is used to communicate with the cloud
	 * API. This is an optional interface, and defaults to returning null.
	 *
	 * @return Compute context object or null if not set.
	 */
	public Object getComputeContext() {
		return dpd.getComputeContext();
	}

	/***************
	 * Starts an additional machine on the cloud , on the specific location, to scale out this specific service. In case
	 * of an error while provisioning the machine, any allocated resources should be freed before throwing a
	 * CloudProvisioningException or TimeoutException to the caller.
	 *
	 * @param duration
	 *            Time duration to wait for the instance.
	 * @param unit
	 *            Time unit to wait for the instance.
	 * @param context
	 *            the provisioning context for this machine.
	 * @return The details of the started instance.
	 * @throws TimeoutException
	 *             In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException
	 *             If a problem was encountered while starting the machine.
	 */
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
	throws TimeoutException, CloudProvisioningException {
		return dpd.startMachine(context, duration, unit);
	}

	/******************
	 * Start the management machines for this cluster. This method is called once by the cloud administrator when
	 * bootstrapping a new cluster.
	 *
	 * @param duration
	 *            timeout duration.
	 * @param unit
	 *            timeout unit.
	 * @param context
	 *            the provisioning context for this request.
	 * @return The created machine details.
	 * @throws TimeoutException
	 *             If creating the new machines exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the machines needed for management could not be provisioned.
	 */
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException,
	CloudProvisioningException {
		return dpd.startManagementMachines(context, duration, unit);
	}

	/**************************
	 * Stops a specific machine for scaling in or shutting down a specific service.
	 *
	 * @param machineIp
	 *            host-name/IP of the machine to shut down.
	 * @param duration
	 *            time to wait for the shutdown operation.
	 * @param unit
	 *            time unit for the shutdown operations
	 * @return true if the operation succeeded, false otherwise.
	 *
	 * @throws InterruptedException
	 *             If the operation was interrupted.
	 * @throws TimeoutException
	 *             If the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the operation encountered an error.
	 */
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
	TimeoutException, CloudProvisioningException {
		return dpd.stopMachine(machineIp, duration, unit);
	}

	/*************
	 * Stops the management machines.
	 *
	 * @throws TimeoutException
	 *             in case the stop operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the stop operation failed.
	 */
	public void stopManagementMachines()
	throws TimeoutException, CloudProvisioningException {
		dpd.stopManagementMachines();
	}

	/************
	 * Returns the name of this cloud.
	 *
	 * @return the name of the cloud.
	 */
	public String getCloudName() {

		return dpd.getCloudName();
	}

	/*************
	 * Called when this bean is no longer needed. Close any internal bean resources.
	 *
	 * @see cleanupCloud() - for cleaning up cloud resources.
	 */
	public void close() {
		dpd.close();
	}

	/**************
	 * Called after service has uninstalled. Used to implement cloud resource cleanup for this service.
	 *
	 * @param duration
	 *            time to wait for the shutdown operation.
	 * @param unit
	 *            time unit for the shutdown operations
	 *
	 * @throws InterruptedException
	 *             If the operation was interrupted.
	 * @throws TimeoutException
	 *             If the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the operation encountered an error.
	 */
	public void onServiceUninstalled(final long duration, final TimeUnit unit)
	throws InterruptedException, TimeoutException, CloudProvisioningException {
		dpd.onServiceUninstalled(duration, unit);
	}


	/*********
	 * Sets the custom data file for the cloud driver instance of a specific service.
	 *
	 * @param customDataFile
	 *            the custom data file (may be a folder).
	 */
	public void setCustomDataFile(final File customDataFile) {
		dpd.setCustomDataFile(customDataFile);
	}

	/**********
	 * Return existing management servers.
	 *
	 * @return the existing management servers, or a 0-length array if non exist.
	 * @throws CloudProvisioningException
	 *             if failed to load servers list from cloud.
	 */
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		return dpd.getExistingManagementServers();
	}

	/**********
	 * Return existing management servers based on controller information saved previously.
	 *
	 * @param controllers
	 *            the controller information used to locate the machine details.
	 * @return the existing management servers, or a 0-length array if non exist.
	 * @throws CloudProvisioningException
	 *             if failed to load servers list from cloud.
	 * @throws UnsupportedOperationException
	 *             if the cloud driver does not support this operation.
	 */
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
	throws CloudProvisioningException, UnsupportedOperationException {
		return dpd.getExistingManagementServers(controllers);
	}

	/**
	 * Cloud-specific validations called after setConfig and before machines are allocated.
	 *
	 * @param validationContext
	 *            The object through which writing of validation messages is done
	 * @throws CloudProvisioningException
	 *             Indicates invalid configuration
	 */
	public void validateCloudConfiguration(final ValidationContext validationContext)
	throws CloudProvisioningException {
		dpd.validateCloudConfiguration(validationContext);
	}



	public void addListener(ProvisioningDriverListener listener){
		dpd.addListener(listener);
	}
}