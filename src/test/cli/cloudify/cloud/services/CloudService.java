package test.cli.cloudify.cloud.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.cloudifysource.shell.commands.CLIException;

/**
 * Every supported cloud must have a service that implements this interface in order to be included in the test cycle.
 * 
 * @author elip
 * 
 */
public interface CloudService {

	/**
	 * performs a bootstrap to a specific cloud. see {@link AbstractCloudService} for generic implementation.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public void bootstrapCloud() throws Exception;

	/**
	 * tears down the specific cloud of all machines. see {@link AbstractCloudService} for generic implementation.
	 * 
	 * @throws CLIException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void teardownCloud()
			throws IOException, InterruptedException;

	/**
	 * @return the rest url cloudify shell can connect to.
	 */
	public String[] getRestUrls();

	/**
	 * 
	 * @return the webui url browsers can connect to.
	 */
	public String[] getWebuiUrls();

	/**
	 * @return the cloud provider name as specified in the jclouds documentation.
	 */
	public String getCloudName();

	/**
	 * replaces the cloud dsl file with SGTest specific details.
	 */
	public void injectAuthenticationDetails() throws IOException;

	public boolean isBootstrapped();

	public String getUser();

	public String getApiKey();

	public String getUniqueName();

	public String getServiceFolder();

	public void setMachinePrefix(String machinePrefix);
	
	public void setNumberOfManagementMachines(int numberOfManagementMachines);
	
	public String getMachinePrefix();

	public void beforeBootstrap() throws Exception;
	
	/********
	 * True if teardown cleanup did not find any leaked nodes, false if there was a leak.
	 * @return .
	 */
	public boolean scanLeakedAgentAndManagementNodes();

	public boolean scanLeakedAgentNodes();
	
	public void addFilesToReplace(Map<File, File> filesToReplace);
	
	public String getPathToCloudFolder();

}
