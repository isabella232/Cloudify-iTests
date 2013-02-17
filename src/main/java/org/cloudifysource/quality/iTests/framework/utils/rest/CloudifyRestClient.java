package org.cloudifysource.quality.iTests.framework.utils.rest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.SetApplicationAttributesRequest;
import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class CloudifyRestClient {

	private WebResource resource;

	private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
	private static final String CONTENT_TYPE_HEADER_VALUE = "application/json";

	public CloudifyRestClient(final String endpoint) {
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client httpClient = Client.create(config);
		httpClient.setConnectTimeout(CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		httpClient.setReadTimeout(CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		resource = httpClient.resource(endpoint);
	}

	public ServiceDetails getServiceDetails(
			final String appName,
			final String serviceName) throws RestClientException, TimeoutException, JsonParseException, JsonMappingException, IOException {
		ClientResponse clientResponse = doGet("/" + appName + "/service/" + serviceName + "/metadata");
		String responseBody = clientResponse.getEntity(String.class);
		Response<ServiceDetails> response = new ObjectMapper().readValue(responseBody, ResponseTypeReferenceFactory.newServiceDetailsResponse());
		return response.getResponse();
	}
	
	public Response<Void> setApplicationAttributes(final String applicationName, SetApplicationAttributesRequest request) throws JsonGenerationException, JsonMappingException, UniformInterfaceException, IOException, RestClientException {
		ClientResponse clientResponse = doPost("/" + applicationName + "/attributes", request);
		String responseBody = clientResponse.getEntity(String.class);
		Response<Void> response = new ObjectMapper().readValue(responseBody, ResponseTypeReferenceFactory.newVoidResponse());
		return response;
	}
	
	public DeleteServiceInstanceAttributeResponse deleteServiceInstanceAttribute(
			final String appName,
			final String serviceName,
			final int instanceId,
			final String attributeName) throws JsonParseException, JsonMappingException, RestClientException, IOException {
		ClientResponse clientResponse = doDelete("/" + appName + "/service/" + serviceName + "/instances/" + instanceId + "/attributes/" + attributeName);
		String responseBody = clientResponse.getEntity(String.class);
		Response<DeleteServiceInstanceAttributeResponse> response = new ObjectMapper().readValue(responseBody, ResponseTypeReferenceFactory.newDeleteServiceInstanceAttributeResponse());
		return response.getResponse();
	}

	
	private ClientResponse doDelete(final String url) throws JsonParseException, JsonMappingException, RestClientException, IOException {
		ClientResponse response = resource.path(url)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.delete(ClientResponse.class);
		checkForError(response);
		return response;
	}


	private ClientResponse doPost(final String url, final Object body) throws JsonGenerationException, JsonMappingException, UniformInterfaceException, IOException, RestClientException {
		ClientResponse response = resource.path(url)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.post(ClientResponse.class, new ObjectMapper().writeValueAsString(body));
		checkForError(response);
		return response;
	}
	
	private ClientResponse doGet(final String url) throws TimeoutException, JsonParseException, JsonMappingException, RestClientException, IOException {
		ClientResponse response = resource.path(url).type(MediaType.APPLICATION_ATOM_XML_TYPE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE).get(ClientResponse.class);
		checkForError(response);
		return response;
	}
	
	private void checkForError(final ClientResponse response) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		if (response.getStatus() != 200) {
			String responseBody = response.getEntity(String.class);
			Response<Void> entity = new ObjectMapper().readValue(responseBody, ResponseTypeReferenceFactory.newVoidResponse());
			throw new RestClientException(response.getStatus(),entity.getMessageId(), entity.getMessage());
		}
	}

}
