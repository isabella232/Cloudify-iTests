package test.cli.cloudify.cloud.services;

import java.io.IOException;
import java.util.Map;

import org.cloudifysource.dsl.cloud.Cloud;
import org.openspaces.admin.Admin;

/**
 * Every supported cloud must have a service that implements this interface in order to be included in the test cycle.
 * 
 * @author elip
 * 
 */
public interface CloudService {

	void bootstrapCloud() throws Exception;

	void teardownCloud() throws IOException, InterruptedException;
	
	/**
	 * Some clouds need a generic action to be performed before the bootstrapping process.
	 * @throws Exception
	 */
	void beforeBootstrap() throws Exception;
	
	void teardownCloud(Admin admin) throws IOException, InterruptedException;

	String[] getRestUrls();

	String[] getWebuiUrls();

	String getCloudName();
	
	Cloud getCloud();

	void injectCloudAuthenticationDetails() throws IOException;
	
	void init(final String uniqueName) throws Exception;

	String getUser();

	String getApiKey();

	void setMachinePrefix(String machinePrefix);
	
	void setNumberOfManagementMachines(int numberOfManagementMachines);
	
	String getMachinePrefix();

	boolean scanLeakedAgentAndManagementNodes();

	boolean scanLeakedAgentNodes();
		
	String getPathToCloudFolder();
	
	String getPathToCloudGroovy();
	
	Map<String, Object> getProperties();

}
