package org.cloudifysource.quality.iTests.test.cli.cloudify;


import java.io.File;
import java.text.MessageFormat;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;

public class Constants {
	//Changing either the service-name will require changing the service name as well.
	protected static final String SERVICE_PATH = CommandTestUtils.getPath("src/main/resources/apps/archives/servlet.war");
	
	protected static final String RESTFUL_WAR_PATH = SGTestHelper.getBuildDir()
			+ MessageFormat.format("{0}tools{0}rest{0}rest.war", File.separatorChar);
	protected static final String SERVICE_NAME = "servlet";
	protected static final String REST_ADMIN_TYPE = "REST";
	protected static final String REST_PORT = "8080";
	public static final int PROCESSINGUNIT_TIMEOUT_SEC = 20;
}
