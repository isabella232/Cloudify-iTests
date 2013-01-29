package framework.utils.rest;

import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.codehaus.jackson.type.TypeReference;

public class ResponseTypeReferenceFactory {

	public static TypeReference<Response<ServiceDetails>> newServiceDetailsResponse() {
		return new TypeReference<Response<ServiceDetails>>() {};
	}
	
	public static TypeReference<Response<Void>> newVoidResponse() {
		return new TypeReference<Response<Void>>() {};
	}
	
	public static TypeReference<Response<DeleteServiceInstanceAttributeResponse>> newDeleteServiceInstanceAttributeResponse() {
		return new TypeReference<Response<DeleteServiceInstanceAttributeResponse>>() {};
	}

}
