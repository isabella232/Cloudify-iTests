package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;

import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.googlecode.ipv6.IPv6Address;

public class AddRemoveTemplatesMultipleManagementMachinesTest extends AbstractByonAddRemoveTemplatesTest{

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {		
		super.bootstrap();
	}

	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addRemoveTemplates() throws Exception {
				
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails template = folderHandler.addDefaultTempalte();
		String templateName = template.getTemplateName();
		
		templatesHandler.addTemplatesToCloud(folderHandler);
		templatesHandler.assertExpectedList();
				
		String templateRemotePath = getTemplateRemoteDirFullPath(templateName) + template.getTemplateFile().getName();	

		verifyTemplateExistence(mngMachinesIP[0], template, templateRemotePath, true);
		verifyTemplateExistence(mngMachinesIP[1], template, templateRemotePath, true);
							
		templatesHandler.removeTemplateFromCloud(folderHandler, templateName, false, null);
		templatesHandler.assertExpectedList();
		
		verifyTemplateExistence(mngMachinesIP[0], template, templateRemotePath, false);
		verifyTemplateExistence(mngMachinesIP[1], template, templateRemotePath, false);
			
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void failedAddRemoveTemplates() throws Exception {
		
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails template = folderHandler.addDefaultTempalte();
		String templateName = template.getTemplateName();
		templatesHandler.addTemplatesToCloud(folderHandler);
		
		String templateRemotePath = getTemplateRemoteDirFullPath(templateName) + template.getTemplateFile().getName();
		
		LogUtils.log(SSHUtils.runCommand(mngMachinesIP[1], AbstractTestSupport.OPERATION_TIMEOUT, "rm -f " + templateRemotePath, USER, PASSWORD));
		
		String output = templatesHandler.removeTemplateFromCloud(folderHandler, templateName, true, null);
		
		AssertUtils.assertTrue("successfully removed template from " + mngMachinesIP[1], output.contains("Failed to remove template [" + templateName + "]"));
		if (IPUtils.isIPv6Address(mngMachinesIP[1])) {
			AssertUtils.assertTrue("successfully removed template from " + mngMachinesIP[1], 
					output.contains(IPv6Address.fromString(mngMachinesIP[1]).toInetAddress().toString().replaceAll("/", "")));
		} else {
			AssertUtils.assertTrue("successfully removed template from " + mngMachinesIP[1], 
					output.contains(mngMachinesIP[1]));
		}
		
		template.setExpectedToFail(true);
		folderHandler.addCustomTemplate(template);
		output = templatesHandler.addTemplatesToCloud(folderHandler);
				
		int failedIndex = output.indexOf("Failed to add the following templates:");
		AssertUtils.assertTrue("successfully added " + templateName + " to " + mngMachinesIP[1], failedIndex != -1);

		int successIndex = output.indexOf("Successfully added the following templates:");
		AssertUtils.assertTrue("failed to add " + templateName + " to " + mngMachinesIP[0], successIndex != -1);
		
		String failedOutput = output.substring(failedIndex, successIndex);
		String successOutput = output.substring(successIndex);
		
		if (IPUtils.isIPv6Address(mngMachinesIP[1])) {
			AssertUtils.assertTrue("successfully added " + templateName + " to " + mngMachinesIP[1], 
					failedOutput.contains(IPv6Address.fromString(mngMachinesIP[1]).toInetAddress().toString().replaceAll("/", "")));
		} else {
			AssertUtils.assertTrue("successfully added " + templateName + " to " + mngMachinesIP[1], 
					failedOutput.contains(mngMachinesIP[1]));
		}
		AssertUtils.assertTrue("successfully added " + templateName + " to " + mngMachinesIP[1], failedOutput.contains(templateName));

		if (IPUtils.isIPv6Address(mngMachinesIP[1])) {
			AssertUtils.assertTrue("failed to add " + templateName + " to " + mngMachinesIP[0], 
					successOutput.contains(IPv6Address.fromString(mngMachinesIP[0]).toInetAddress().toString().replaceAll("/", "")));
		} else {
			AssertUtils.assertTrue("failed to add " + templateName + " to " + mngMachinesIP[0], 
					successOutput.contains(mngMachinesIP[0]));
		}
		AssertUtils.assertTrue("failed to add " + templateName + " to " + mngMachinesIP[0], successOutput.contains(templateName));
		
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}


	@Override
	public int getNumOfMngMachines() {
		return 2;
	}



}
