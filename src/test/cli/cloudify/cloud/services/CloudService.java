package test.cli.cloudify.cloud.services;

import java.io.IOException;

import org.cloudifysource.shell.commands.CLIException;

/**
 * Every supported cloud must have a service that implements this interface in order
 * to be included in the test cycle.
 * @author elip
 *
 */
public interface CloudService {
	
	/**
	 * performs a bootstrap to a specific cloud.
	 * see {@link AbstractCloudService} for generic implementation.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void bootstrapCloud() throws IOException, InterruptedException;
	

	/**
	 * tears down the specific cloud of all machines.
	 * see {@link AbstractCloudService} for generic implementation.
	 * @throws CLIException 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void teardownCloud() throws IOException, InterruptedException;
	
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
	
}
