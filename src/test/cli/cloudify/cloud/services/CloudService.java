package test.cli.cloudify.cloud.services;

import java.io.IOException;
import java.util.Map;

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
	
	void teardownCloud(Admin admin) throws IOException, InterruptedException;

	String[] getRestUrls();

	String[] getWebuiUrls();

	String getCloudName();

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
