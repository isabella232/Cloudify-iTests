package org.cloudifysource.quality.iTests.framework.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.rest.request.InvokeCustomCommandRequest;
import org.cloudifysource.dsl.rest.response.InvokeInstanceCommandResponse;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import com.j_spaces.kernel.PlatformVersion;

public class CommandInvoker {
	
	
	private URL restUrl;
	private String userName = "";
	private String password = "";

	public CommandInvoker(final String restUrl) throws MalformedURLException {
		this.restUrl = new URL(restUrl);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	

	public InvokeServiceCommandResponse restInvokeServiceCommand(final String applicationName, final String serviceName, 
			final String commandName, List<String> parameters) throws RestClientException {
    	
        final String version = PlatformVersion.getVersion();
        final RestClient restClient = new RestClient(restUrl, this.getUserName(), this.getPassword(), version);
        restClient.connect();
        
        if (parameters == null) {
        	parameters = new ArrayList<String>();
        }
        
        InvokeCustomCommandRequest invokeCustomCommandRequest = new InvokeCustomCommandRequest();
        invokeCustomCommandRequest.setCommandName(commandName);
        invokeCustomCommandRequest.setParameters(parameters);
        return restClient.invokeServiceCommand(applicationName, serviceName, invokeCustomCommandRequest);
        
    }
	
	public InvokeInstanceCommandResponse restInvokeInstanceCommand(final String applicationName, final String serviceName, 
			final int instanceId, final String commandName, final List<String> parameters) throws RestClientException {
    	
        final String version = PlatformVersion.getVersion();
        final RestClient restClient = new RestClient(restUrl, this.getUserName(), this.getPassword(), version);
        restClient.connect();
        
        InvokeCustomCommandRequest invokeCustomCommandRequest = new InvokeCustomCommandRequest();
        invokeCustomCommandRequest.setCommandName(commandName);
        invokeCustomCommandRequest.setParameters(parameters);
        return restClient.invokeInstanceCommand(applicationName, serviceName, instanceId, invokeCustomCommandRequest);
        
    }
    
}
