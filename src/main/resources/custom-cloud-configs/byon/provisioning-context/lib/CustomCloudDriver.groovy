import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;

import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver
import org.cloudifysource.esc.driver.provisioning.*
import org.cloudifysource.esc.installer.*;
import org.cloudifysource.esc.driver.provisioning.byon.*

import java.util.concurrent.*

public class CustomCloudDriver extends org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver {

	@Override
	MachineDetails startMachine(String locationId, long duration, TimeUnit unit)
	throws TimeoutException, CloudProvisioningException {
		println("In start machine")
		ProvisioningContext ctx =  new ProvisioningContextAccess().getProvisioiningContext()
		println "Context is: " + ctx
		MachineDetails md = super.startMachine(locationId, duration, unit);
		md.setAgentRunning(true)

		ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName)
		final String script = ctx.createEnvironmentScript(md, template);


		InstallationDetails details = ((ProvisioningContextImpl)ctx).getCreatedDetails().get(0);

		println "*********************************"
		println "script is: "
		println script
		println "*********************************"
		AgentlessInstaller installer = new AgentlessInstaller()
		installer.setEnvironmentFileContents(script)

		installer.installOnMachineWithIP(details, duration, unit)

		return md;
	}



	@Override
	public MachineDetails[] startManagementMachines(final long duration,
			final TimeUnit unit) throws TimeoutException,
	CloudProvisioningException {
		println("In start management")
		ManagementProvisioningContext ctx =  new ProvisioningContextAccess().getManagementProvisioiningContext()
		println "Context is: " + ctx
		MachineDetails[] mds = super.startManagementMachines(duration, unit);
		for (md in mds) {
			md.setAgentRunning(true)
		}

		ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName)
		final String[] scripts = ctx.createManagementEnvironmentScript(mds, template);
		ExecutorService executorService = Executors.newFixedThreadPool(scripts.length);
		
		for(int i=0;i<scripts.length;++i) {

			final String script = scripts[i];
			final InstallationDetails details = ((ProvisioningContextImpl)ctx).getCreatedDetails().get(i);

			println "*********************************"
			println "script is: "
			println script
			println "*********************************"
			
			Runnable task = new Runnable() {
				public void run() {
					AgentlessInstaller installer = new AgentlessInstaller()
					installer.setEnvironmentFileContents(script)
		
					installer.installOnMachineWithIP(details, duration, unit)
				}
			}
			executorService.execute(task)
			
		}
		executorService.shutdown()
		executorService.awaitTermination(duration, unit)

		return mds;
	}
}