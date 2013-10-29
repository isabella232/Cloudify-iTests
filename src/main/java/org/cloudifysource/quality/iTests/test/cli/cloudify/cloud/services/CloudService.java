package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services;

import java.io.IOException;
import java.util.Map;

import org.cloudifysource.domain.cloud.Cloud;
import org.openspaces.admin.Admin;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;

/**
 * Every supported cloud must have a service that implements this interface in order to be included in the test cycle.
 * 
 * @author elip
 * 
 */
public interface CloudService {

	String bootstrapCloud() throws Exception;

	void teardownCloud() throws Exception;
	
	/**
	 * Some clouds need a generic action to be performed before the bootstrapping process.
	 * @throws Exception
	 */
	void beforeBootstrap() throws Exception;

    void afterTeardown() throws Exception;
	
	void teardownCloud(Admin admin) throws IOException, InterruptedException;

	String[] getRestUrls();

	String[] getWebuiUrls();

	String getCloudName();
	
	Cloud getCloud();

	void injectCloudAuthenticationDetails() throws IOException;
	
	void init(final String uniqueName) throws Exception;
	
	void setBootstrapper(CloudBootstrapper bootstrapper);
	
	CloudBootstrapper getBootstrapper();

	String getUser();

	String getApiKey();

	void setMachinePrefix(String machinePrefix);

    void setVolumePrefix(String volumePrefix);

	void setNumberOfManagementMachines(int numberOfManagementMachines);

	String getMachinePrefix();

	String getVolumePrefix();

	boolean scanLeakedAgentAndManagementNodes();

	boolean scanLeakedAgentNodes();
	
	boolean scanLeakedManagementNodes();

	String getPathToCloudFolder();

	String getPathToCloudGroovy();

	Map<String, Object> getProperties();

    Map<String, String> getAdditionalPropsToReplace();

    String getRegion();
}
