package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;

public class ByonAddTemplatesWithSecurityTest extends AbstractByonAddRemoveTemplatesTest{
	
	private static final String ACCESS_IS_DENIED = "Permission not granted, access is denied.";
	private static final String ADDED_SUCCESSFULLY_MESSAGE = "added successfully";
	private static final String REMOVE_SUCCEEDED_MESSAGE = "removed successfully";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.user(SecurityConstants.USER_PWD_ALL_ROLES).password(SecurityConstants.USER_PWD_ALL_ROLES).secured(true)
			.securityFilePath(SecurityConstants.LDAP_SECURITY_FILE_PATH)
			.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH)
			.keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		service.setBootstrapper(bootstrapper);
		super.bootstrap(service);
		username = SecurityConstants.USER_PWD_ALL_ROLES;
		password = SecurityConstants.USER_PWD_ALL_ROLES;
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testAddTemplatesAndInstallWithDifferentUsers() throws Exception {
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate = folderHandler.addTempalteForServiceInstallation();
		
		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN).password(SecurityConstants.USER_PWD_CLOUD_ADMIN).setRestUrl(getRestUrl());
		bootstrapper.addTemplate(folderHandler.getFolder().getAbsolutePath(), false);
	
		final String templateName = addedTemplate.getTemplateName();
		String serviceName = templateName + "_service";
		
		ServiceInstaller installer = new ServiceInstaller(getRestUrl(), serviceName);
		installer
		.cloudifyUsername(SecurityConstants.USER_PWD_APP_MANAGER)
		.cloudifyPassword(SecurityConstants.USER_PWD_APP_MANAGER)
		.recipePath(serviceCreator.createServiceDir(serviceName, templateName).getAbsolutePath())
		.timeoutInMinutes(AbstractTestSupport.OPERATION_TIMEOUT);
		installer.install();
		
		installer.uninstall();
		
		bootstrapper.removeTemplate(templateName, false);
	
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testAddTemplates() throws Exception{
		
		verifyAddTemplate(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, true);
		verifyAddTemplate(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, true);
		verifyAddTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, false);
		verifyAddTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, false);
		verifyAddTemplate(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, true);
		verifyAddTemplate(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, true);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testGetTemplate() throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		TemplatesFolderHandler folderHandler = this.templatesHandler.createNewTemplatesFolder();		
		TemplateDetails addedTemplate = folderHandler.addDefaultTempalte();
		String templateName = addedTemplate.getTemplateName();
		
		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER).password(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER).setRestUrl(getRestUrl());
		bootstrapper.addTemplate(folderHandler.getFolder().getAbsolutePath(), false);
		
		verifyGetTemplate(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, false);
		verifyGetTemplate(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, false);
		verifyGetTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, false);
		verifyGetTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, false);
		verifyGetTemplate(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, true);
		verifyGetTemplate(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, true);
		
		bootstrapper.removeTemplate(templateName, false);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testRemoveTemplate() throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		TemplatesFolderHandler folderHandler = this.templatesHandler.createNewTemplatesFolder();		
		TemplateDetails addedTemplate = folderHandler.addDefaultTempalte();
		String templateName = addedTemplate.getTemplateName();
		
		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN).password(SecurityConstants.USER_PWD_CLOUD_ADMIN).setRestUrl(getRestUrl());
		bootstrapper.addTemplate(folderHandler.getFolder().getAbsolutePath(), false);
		
		verifyRemoveTemplate(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, templateName, true);
		verifyRemoveTemplate(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, templateName, true);
		verifyRemoveTemplate(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, templateName, true);
		verifyRemoveTemplate(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, templateName, true);
		verifyRemoveTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, templateName, false);
		
		TemplatesFolderHandler folderHandler2 = this.templatesHandler.createNewTemplatesFolder();		
		addedTemplate = folderHandler2.addDefaultTempalte();
		templateName = addedTemplate.getTemplateName();
		bootstrapper.addTemplate(folderHandler2.getFolder().getAbsolutePath(), false);
		
		verifyRemoveTemplate(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName, false);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testListTemplate() throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();

		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER).password(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER);
		
		TemplateDetails addedTemplate1 = folderHandler.addDefaultTempalte();
		String templateName1 = addedTemplate1.getTemplateName();
		
		bootstrapper.user(SecurityConstants.USER_PWD_CLOUD_ADMIN).password(SecurityConstants.USER_PWD_CLOUD_ADMIN).setRestUrl(getRestUrl());
		bootstrapper.addTemplate(folderHandler.getFolder().getAbsolutePath(), false);
		
		TemplatesFolderHandler folderHandler2 = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate2 = folderHandler2.addDefaultTempalte();
		String templateName2 = addedTemplate2.getTemplateName();
		
		bootstrapper.addTemplate(folderHandler2.getFolder().getAbsolutePath(), false);
				
		verifyListTemplates(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, true, true);
		verifyListTemplates(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, true, true);
		verifyListTemplates(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, true, true);
		verifyListTemplates(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, true, true);
		verifyListTemplates(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, false, false);
		verifyListTemplates(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, templateName1, templateName2, false, false);
		
		bootstrapper.removeTemplate(addedTemplate1.getTemplateName(), false);
		bootstrapper.removeTemplate(addedTemplate2.getTemplateName(), false);

	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	public void verifyAddTemplate(String user, String password, String userDescription, boolean isExpectedToFail) throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate = folderHandler.addDefaultTempalte();
		
		bootstrapper.user(user).password(password).setRestUrl(getRestUrl());
		String output = bootstrapper.addTemplate(folderHandler.getFolder().getAbsolutePath(), isExpectedToFail);
		
		if(isExpectedToFail){
			AssertUtils.assertTrue(userDescription + " succeeded in adding a template", output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
		}
		else{
			AssertUtils.assertTrue(userDescription + " didn't succeed in adding a template", output.contains(addedTemplate.getTemplateName()) && output.contains(ADDED_SUCCESSFULLY_MESSAGE) && !output.contains("Failed"));
		}
		
		bootstrapper.removeTemplate(addedTemplate.getTemplateName(), true);
	}
	
	public void verifyGetTemplate(String user, String password, String userDescription, String adderDescription, String templateName, boolean isExpectedToFail) throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();	
		bootstrapper.user(user).password(password).setRestUrl(getRestUrl());
		String output = bootstrapper.getTemplate(templateName, isExpectedToFail);
		
		if(isExpectedToFail){
			AssertUtils.assertTrue(userDescription + " can see the template " + templateName, output.contains(ACCESS_IS_DENIED));
		}
		else{
			AssertUtils.assertTrue(userDescription + " doesn't see the template " + templateName , !output.contains(ACCESS_IS_DENIED));		
		}
		
	}
	
	public void verifyListTemplates(String user, String password, String userDescription, String adderDescription1, String adderDescription2, String templateName1, String templateName2, boolean seesTemplate1, boolean seesTemplate2) throws Exception{
		CloudBootstrapper bootstrapper = new CloudBootstrapper();	
		bootstrapper.user(user).password(password).setRestUrl(getRestUrl());
		String output = bootstrapper.listTemplates(true);
		
		if(seesTemplate1){
			AssertUtils.assertTrue(userDescription + " doesn't see the template added by " + adderDescription1 + "output: " + output, output.contains(templateName1 + ":"));
		}
		else{
			AssertUtils.assertTrue(userDescription + " sees the template added by " + adderDescription1 + "output: " + output, !output.contains(templateName1 + ":"));		
		}
		
		if(seesTemplate2){
			AssertUtils.assertTrue(userDescription + " doesn't see the template added by " + adderDescription2 + "output: " + output, output.contains(templateName2 + ":"));
		}
		else{
			AssertUtils.assertTrue(userDescription + " sees the template added by " + adderDescription2 + "output: " + output, !output.contains(templateName2 + ":"));		
		}
	}
	
	public void verifyRemoveTemplate(String user, String password, String userDescription, String templateName, boolean isExpectedToFail) throws Exception{
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();	
		bootstrapper.user(user).password(password).setRestUrl(getRestUrl());
		String output = bootstrapper.removeTemplate(templateName, isExpectedToFail);
		
		if(isExpectedToFail){
			AssertUtils.assertTrue(userDescription + " succeeded removing the template " + templateName, output.contains(ACCESS_IS_DENIED));
		}
		else{
			AssertUtils.assertTrue(userDescription + " didn't succeed removing the template " + templateName, output.contains(REMOVE_SUCCEEDED_MESSAGE));		
		}
	}
	
	public String listTemplates() throws Exception{

		CloudBootstrapper bootstrapper = new CloudBootstrapper();	
		bootstrapper
		.user(SecurityConstants.USER_PWD_ALL_ROLES)
		.password(SecurityConstants.USER_PWD_ALL_ROLES)
		.setRestUrl(getRestUrl());
		
		String output = bootstrapper.listTemplates(false);

		return output;
	}
	
	@Override
	public int getNumOfMngMachines() {
		return 1;
	}
}
