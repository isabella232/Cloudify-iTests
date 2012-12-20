package test.webui.cloud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermittedServicesWrapper {
	
	private final String _userName;
	
	private final Set<String> _applicationNames;
	//key is service name, value is number of instances
	private final Map<String,Integer> _services;
	private final Set<String> _machineAddresses;
	
	private final int _esmCount;
	private final int _gsaCount;
	private final int _gscCount;
	private final int _gsmCount;
	private final int _lusCount;
	private final int _hostsCount;
	
	private final int _statefulPuCount; 
	private final int _statelessPuCount; 
	private final int _webModuleCount;
	private final int _webServerCount;
	
	public PermittedServicesWrapper( String userName, Set<String> applicationNames, 
							Map<String,Integer> services, Set<String> machineAddresses,
							int esmCount,  int gsaCount,  int gscCount,  int gsmCount, 
							int lusCount, int hostsCount, int statefulPuCount, 
							int statelessPuCount, int webModuleCount, int webServerCount ){

		_userName = userName;
		
		_applicationNames = new HashSet<String>( applicationNames );
		_services = new HashMap<String,Integer>( services );
		_machineAddresses = new HashSet<String>( machineAddresses );
		
		_esmCount = esmCount; 
		_gsaCount = gsaCount; 
		_gscCount = gscCount; 
		_gsmCount = gsmCount; 
		_lusCount = lusCount; 
		_hostsCount = hostsCount;
		
		_statefulPuCount = statefulPuCount;
		_statelessPuCount = statelessPuCount;
		_webModuleCount = webModuleCount;  
		_webServerCount = webServerCount;  			
	}

	public Set<String> getApplicationNames() {
		return _applicationNames;
	}

	public Map<String,Integer> getServices() {
		return _services;
	}

	public Set<String> getMachineAddresses() {
		return _machineAddresses;
	}
	
	public int getApplicationsCount() {
		return _applicationNames.size();
	}

	public int getServicesCount() {
		return _services.size();
	}

	public int getMachinesCount() {
		return _machineAddresses.size();
	}

	public int getEsmCount() {
		return _esmCount;
	}

	public int getGsaCount() {
		return _gsaCount;
	}

	public int getGscCount() {
		return _gscCount;
	}

	public int getGsmCount() {
		return _gsmCount;
	}

	public int getLusCount() {
		return _lusCount;
	}

	public int getHostsCount() {
		return _hostsCount;
	}

	public int getStatefulPuCount() {
		return _statefulPuCount;
	}

	public int getStatelessPuCount() {
		return _statelessPuCount;
	}

	public int getWebModuleCount() {
		return _webModuleCount;
	}

	public int getWebServerCount() {
		return _webServerCount;
	}

	public String getUserName() {
		return _userName;
	}
}