package org.cloudifysource.quality.iTests.test.rest.component;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.SetApplicationAttributesRequest;
import org.cloudifysource.dsl.rest.request.SetServiceAttributesRequest;
import org.cloudifysource.dsl.rest.request.SetServiceInstanceAttributesRequest;
import org.cloudifysource.dsl.rest.response.DeleteApplicationAttributeResponse;
import org.cloudifysource.dsl.rest.response.DeleteServiceAttributeResponse;
import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.GetApplicationAttributesResponse;
import org.cloudifysource.dsl.rest.response.GetServiceAttributesResponse;
import org.cloudifysource.dsl.rest.response.GetServiceInstanceAttributesResponse;
import org.cloudifysource.dsl.rest.response.Response;
import iTests.framework.utils.AssertUtils;
import org.codehaus.jackson.type.TypeReference;
import org.testng.annotations.Test;

public class DeplyomentsControllerAttributesMethodsTest extends DeploymentsControllerTest {



    /**
     * ==========================================================================================================================================
     * 													Attributes Section Tests
     * ==========================================================================================================================================
     */

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1" ,enabled = true)
    public void testSetGetDeleteApplicationAttribute() throws Exception {

        testValidateApplication();
        String url = "/helloworld/attributes";
        String deleteUrl = url + "/newAttr2";

        SetApplicationAttributesRequest saar = new SetApplicationAttributesRequest();
        saar.setAttributes(createTestAttributeMap());

        Response<Void> responseSetAttribute = client.postVoidResponse(url, saar);

        // test set application attribute
        AssertUtils.assertEquals(
                CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName(),
                responseSetAttribute.getMessageId());


        // test get application attribute
        GetApplicationAttributesResponse appAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetApplicationAttributesResponse>>() {});
        Map<String, Object> attributes = appAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.get("newAttr2"), "two");

        // test delete application attribute
        DeleteApplicationAttributeResponse deleteAppAttribute = client.responseDeleteMethod(deleteUrl, new TypeReference<Response<DeleteApplicationAttributeResponse>>() {});
        Object attributeDelete = deleteAppAttribute.getPreviousValue();


        AssertUtils.assertEquals(attributeDelete, "two");



        // test get application attribute after delete
        appAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetApplicationAttributesResponse>>() {});
        attributes = appAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.containsKey("newAttr2"),false);

    }



    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1" ,enabled = true)
    public void testSetGetDeleteServiceAttribute() throws Exception {

        testValidateApplication();
        String url = "/helloworld/service/tomcat/attributes";
        String deleteUrl = url + "/newAttr2";

        SetServiceAttributesRequest ssarequest = new SetServiceAttributesRequest();
        ssarequest.setAttributes(createTestAttributeMap());

        Response<Void> responseSetAttribute = client.postVoidResponse(url, ssarequest );

        // test set service attribute
        AssertUtils.assertEquals(
                CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName(),
                responseSetAttribute.getMessageId());


        // test get service attribute
        GetServiceAttributesResponse servAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetServiceAttributesResponse>>() {});
        Map<String, Object> attributes = servAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.get("newAttr2"), "two");



        // test delete service attribute
        DeleteServiceAttributeResponse deleteServiceAttribute = client.responseDeleteMethod(deleteUrl, new TypeReference<Response<DeleteServiceAttributeResponse>>() {});
        Object attributeDelete = deleteServiceAttribute.getPreviousValue();


        AssertUtils.assertEquals(attributeDelete, "two");



        // test get service attribute after delete
        servAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetServiceAttributesResponse>>() {});
        attributes = servAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.containsKey("newAttr2"),false);




    }




    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1" ,enabled = true)
    public void testSetGetDeleteServiceInstanceAttribute() throws Exception {

        testValidateApplication();
        String url = "/helloworld/service/tomcat/instances/1/attributes";
        String deleteUrl = url + "/newAttr2";

        SetServiceInstanceAttributesRequest ssiarequest = new SetServiceInstanceAttributesRequest();
        ssiarequest.setAttributes(createTestAttributeMap());

        Response<Void> responseSetAttribute = client.postVoidResponse(url, ssiarequest );

        // test set service instance attribute
        AssertUtils.assertEquals(
                CloudifyMessageKeys.OPERATION_SUCCESSFULL.getName(),
                responseSetAttribute.getMessageId());


        // test get service instance attribute
        GetServiceInstanceAttributesResponse servInstAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetServiceInstanceAttributesResponse>>() {});
        Map<String, Object> attributes = servInstAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.get("newAttr2"), "two");


        // test delete service instance attributes
        DeleteServiceInstanceAttributeResponse deleteServiceInstanceAttribute = client.responseDeleteMethod(deleteUrl ,new TypeReference<Response<DeleteServiceInstanceAttributeResponse>>() {});
        Object  attributeDelete = deleteServiceInstanceAttribute.getPreviousValue();


        AssertUtils.assertEquals(attributeDelete, "two");



        // test get service attribute after delete
        servInstAttributeResponse = client.responseGetMethod(url, new TypeReference<Response<GetServiceInstanceAttributesResponse>>() {});
        attributes = servInstAttributeResponse.getAttributes();

        AssertUtils.assertEquals(attributes.containsKey("newAttr2"),false);



    }



    private Map<String, Object> createTestAttributeMap() {
        Map<String, Object> requestBodyAttributes = new HashMap<String, Object>();
        requestBodyAttributes.put("newAttr2", "two");
        return requestBodyAttributes;
    }



}
