package org.cloudifysource.esc.util

import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.cloudifysource.dsl.cloud.Cloud
import org.openspaces.admin.Admin
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException
import org.cloudifysource.esc.driver.provisioning.MachineDetails
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver

public class ProvisioningDriverClass implements ProvisioningDriver {

	DefaultProvisioningDriver dpd = new DefaultProvisioningDriver();
	public void setConfig(Cloud cloud, String cloudTemplate, boolean management, String serviceName) 
			throws CloudProvisioningException {
				dpd.setConfig(cloud, cloudTemplate, management, serviceName);
			}
	

	public Object getComputeContext(){ 
		return dpd.getComputeContext();
	}
	
	public void setAdmin(Admin admin) {
		dpd.setAdmin(admin);
	} 


	public MachineDetails startMachine(String locationId, long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
				return dpd.startMachine(locationId, duration, unit);
	}

	
	public MachineDetails[] startManagementMachines(long duration, TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException{ 
				return dpd.startManagementMachines(duration, unit);
			}
			
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
			TimeoutException, CloudProvisioningException{ 
				return dpd.stopMachine(machineIp, duration, unit);
			}
			
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException{ 
				dpd.stopManagementMachines();
			}

	
	public String getCloudName(){ 
		return dpd.getCloudName();
	}

	
	public void close(){ 
		dpd.close();
	}

	
	public void addListener(ProvisioningDriverListener listener){ 
		dpd.addListener(listener);
	}


	public void onServiceUninstalled(long duration, TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException{
				dpd.onServiceUninstalled(duration, unit);
	}

}