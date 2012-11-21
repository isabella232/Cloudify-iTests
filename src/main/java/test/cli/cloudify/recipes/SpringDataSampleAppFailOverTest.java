//package test.cli.cloudify.recipes;
//
//import java.io.File;
//import java.util.ArrayList;
//
//import org.junit.Assert;
//import org.openspaces.admin.pu.ProcessingUnit;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import test.cli.cloudify.CommandTestUtils;
//import test.utils.LogUtils;
//
//import org.cloudifysource.dsl.Service;
//import org.cloudifysource.dsl.internal.ServiceReader;
//
///**
// * 
// * @author gal
// * TODO: the import of the application to SGTest doesn't work. can't find a way to directthe property 
// * files to the zip files I tried to copy the zip files to the work dir but it didn't work and it's a 
// * bad solution any way commented out those parts in the befor Test
// */
//public class SpringDataSampleAppFailOverTest extends ApplicationFailOverAbstractTest {
//	
//	private final int tomcatPort = 9000;
//	private int mongodPort;
//	private String dataSampleAppDirPath;	
//	private boolean springDataSampleInstalled = false;
//	
//	public SpringDataSampleAppFailOverTest(){
//		super();	
//	}
//	
//	@Override
//	@BeforeMethod
//	public void beforeTest() {
//		super.beforeTest();
////		File mongodArchiveSrc;
//		if(System.getProperty("user.name").equals("tgrid")){
//			dataSampleAppDirPath = CommandTestUtils.getPath("apps/cloudify/recipes/springDataSample_Linux");
////			mongodArchiveSrc = new File(dataSampleAppDirPath + "/mongodb-osx-x86_64-2.0.0.tgz");
//		}
//		else{
//			dataSampleAppDirPath = CommandTestUtils.getPath("apps/cloudify/recipes/springDataSample_Windows");
////			mongodArchiveSrc = new File(dataSampleAppDirPath + "/mongodb-win32-i386-2.0.0.zip");
//		}	
//		springDataSampleInstalled = false;
//		mongodPort = mongodPort();
////		
////		File tomcatArchiveSrc = new File(dataSampleAppDirPath + "/apache-tomcat-6.0.33.zip");
////		File tomcatArchiveDest = new File(ScriptUtils.getBuildPath() + "/work/processing-units/tomcat_1");
////		File mongodArchiveDest = new File(ScriptUtils.getBuildPath() + "/work/processing-units/mongod_1");
////		tomcatArchiveDest.mkdir();
////		mongodArchiveDest.mkdir();
////		tomcatArchiveDest.setWritable(true, false);
////		mongodArchiveDest.setWritable(true, false);
////		try {
////			FileSystemUtils.copyRecursively(tomcatArchiveSrc, tomcatArchiveDest);
////			FileSystemUtils.copyRecursively(mongodArchiveSrc, mongodArchiveDest);
////		} catch (IOException e) {
////			e.printStackTrace();
////			Assert.assertTrue(false);
////		}
//	}
//
//	@Override
//	@AfterMethod
//	public void afterTest() {
//		try {
//			if(springDataSampleInstalled)
//				runCommand("connect " + restUrl + " ;uninstall-application springDataSample");
//			
//		} catch (Exception e) {	}
//		super.afterTest();
//	}
//
//	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
//	public void testDataSampleApp() throws Exception{
//		assertInstallApp(tomcatPort, mongodPort , dataSampleAppDirPath);
//	}
//	
//	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
//	public void testDataSampleAppMongodPuInstFailOver() throws Exception{
//		assertInstallApp(tomcatPort, mongodPort , dataSampleAppDirPath);
//		springDataSampleInstalled = isDataSampleAppInstalled();
//		
//		ProcessingUnit mongod = admin.getProcessingUnits().getProcessingUnit("mongod");	
//		int mongodInstancesAfterInstall = mongod.getInstances().length;
//		LogUtils.log("destroying the pu instance holding mongod");
//		mongod.getInstances()[0].destroy();
//		
//		assertPuInstanceKilled("mongod" , mongodPort , mongodInstancesAfterInstall);
//		assertPuInstanceRessurected("mongod" , mongodPort , mongodInstancesAfterInstall);
//	}
//
//	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
//	public void testDataSampleAppMongodGSCFailOver() throws Exception{
//		assertInstallApp(tomcatPort, mongodPort , dataSampleAppDirPath);
//		springDataSampleInstalled = isDataSampleAppInstalled();
//		
//		ProcessingUnit mongod = admin.getProcessingUnits().getProcessingUnit("mongod");
//		int mongodInstancesAfterInstall = mongod.getInstances().length;
//		LogUtils.log("restarting GSC containing mongod");
//		mongod.getInstances()[0].getGridServiceContainer().kill();
//		
//		assertPuInstanceKilled("mongod" , mongodPort , mongodInstancesAfterInstall);
//		assertPuInstanceRessurected("mongod", mongodPort, mongodInstancesAfterInstall);
//	}
//	
//	private boolean isDataSampleAppInstalled() {
//		ProcessingUnit mongod = admin.getProcessingUnits().getProcessingUnit("mongod");
//		ProcessingUnit tomcat = admin.getProcessingUnits().getProcessingUnit("tomcat");
//		Assert.assertTrue("application was not installed", (mongod!=null && tomcat!=null));
//		return true;
//	}
//	
//	private int mongodPort() {
//		Service mongodService = null;
//		try {
//			mongodService = ServiceReader.getServiceFromDirectory(new File(dataSampleAppDirPath + "/mongod")).getService();
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.assertTrue(false);
//		} 
//		return ((ArrayList<Integer>) mongodService.getPlugins().get(0).getConfig().get("Port")).get(0);
//	}
//}
