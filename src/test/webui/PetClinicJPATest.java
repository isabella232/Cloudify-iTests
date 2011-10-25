package test.webui;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ToStringUtils.gscToString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.annotations.BeforeMethod;

import test.utils.ThreadBarrier;
import test.web.AbstractWebTest;

import com.thoughtworks.selenium.Selenium;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 2 gsc's (one is only loaded during the test)
 * Tests:
 * - 1: Deploy two instances of petclinic webapp (in shared mode). 
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic-web-jpa
 * 
 * Verify webapp is not accessible by:
 * http://IP:8080/petclinic-web-jpa
 *
 * - 2: Start additional GSC and relocate petclinic.PU[1] to empty GSC.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic-web-jpa
 * 
 * - 3: Increase (+1) the petclinic webapp instances.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic-web-jpa 

 * Verify webapp is not accessible by:
 * http://IP:8080/petclinic-web-jpa
 * 
 * - 4: Decrease (-1) the petclinic webapp instances (third instance).
 * Verify webapp is not accessible by:
 * http://IP:8080/petclinic-web-jpa
 * 
 * - 5: Kill the GSC that contains at least one petclinic.PU instance.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic-web-jpa
 * 
 * - 6: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */ 

public class PetClinicJPATest extends AbstractWebTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer gsc0;
	private GridServiceContainer gsc1;
	private GridServiceContainer gsc2;
	private String ip;
	private Integer index = -1;
	private final int USER_NUM = 1;
	private final int OWENER_NUM = 10;
	private final int PET_NUM = 5;
	private final int VISIT_NUM = 5;
	private final int READ_NUM = 10;
	private final int TEST_NUM = 2;
	private ProcessingUnit pu;
	ThreadBarrier barrier = new ThreadBarrier(2);


	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		super.beforeTest();
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		ip = machine.getHostAddress();
		gsm = loadGSM(machine);
		gsc0 = loadGSC(machine);
		gsc2 = loadGSC(machine);
	}


	//@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void test() throws Exception {
		// 1 Deploy one instance of petclinic webapp. 
		ProcessingUnit processor = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic-processor-jpa.jar")));
		processor.waitFor(processor.getTotalNumberOfInstances());
		log("pet clinic processor deployed");

		pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic-web-jpa.war")).
				numberOfInstances(1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		log("pet clinic web app deployed");

		Thread t0 = new Thread(new InnerFeeder());
		t0.start();	
		barrier.await();
		log("feeder/s ended");

		int expected = processor.getSpace().getGigaSpace().count(null);

		Thread t1 = new Thread(new InnerTester(pu));
		t1.start();
		log("tester started");
		barrier.await();
		log("shutting down tester");
		

		Assert.assertEquals(expected, processor.getSpace().getGigaSpace().count(null));

		Thread t2 = new Thread(new InnerReader());
		admin.getProcessingUnits().getProcessingUnit("petclinic-web-jpa").waitFor(1);
		log("reader started");
		t2.start();
		barrier.await();
		log("shutting down reader");
		System.out.println(Thread.currentThread().toString());

		Assert.assertEquals(expected, processor.getSpace().getGigaSpace().count(null));

		// 6 Undeploy the petclinic webapp.

		assertEquals(1, pu.getNumberOfInstances());

		final CountDownLatch removedLatch = new CountDownLatch(pu.getTotalNumberOfInstances());
		pu.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener(){
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				removedLatch.countDown();
			}
		});

		pu.undeploy();
		assertTrue(removedLatch.await(60, TimeUnit.SECONDS));
		assertPageNotExists("http://"+ ip +":8080/petclinic-web-jpa/");
	}

	class InnerFeeder implements Runnable {
		public InnerFeeder(){
			super();
		}

		public void run(){
			try{
				feed();
				barrier.await();
			}
			catch(Throwable th){
				barrier.reset(th);
			}

		}

		private void feed() throws Exception{
			log("feeder started");
			Integer currIndex = getIndex();
			WebDriver driver = new FirefoxDriver();
			String baseUrl = "http://"+ ip + ":8080/petclinic-web-jpa/";
			final Selenium selenium = new WebDriverBackedSelenium(driver, baseUrl);
			selenium.open(baseUrl);
			if(currIndex == 0)
				selenium.click("//div[@id = 'main']/ul/li[4]/a");

			for(int i = currIndex*OWENER_NUM; i < currIndex*OWENER_NUM + OWENER_NUM; i++){
				selenium.open(baseUrl + "owners/new");
				String input = Integer.toString(i);
				selenium.type("name=firstName", input);
				selenium.type("name=lastName", input);
				selenium.type("name=address", input);
				selenium.type("name=city", input);
				selenium.type("name=telephone", input);
				selenium.click("//input[@value='Add Owner']");
				for(int j = 0; j < PET_NUM; j++){
					selenium.click("//div[@id='main']/table[2]/tbody/tr/td[2]/a");
					selenium.type("name=name", input + " " + Integer.toString(j));
					selenium.type("name=birthDate", "1985-04-12");
					selenium.click("//input[@value='Add Pet']");
					for(int k = 0; k < VISIT_NUM; k++){
						selenium.open(baseUrl + "owners/search");
						selenium.type("id=lastName", input);
						selenium.click("//input[@value='Find Owners']");
						selenium.click("//div[@id='main']/table[" + ((j * 2) + 4) + "]/tbody/tr/td[3]/a");
						selenium.typeKeys("//textarea[@id='description']", i + ", " + j + ", " + k);
						selenium.click("//input[@value='Add Visit']");
					}
				}
			}
			selenium.close();
		}
	}

	class InnerTester implements Runnable {
		private ProcessingUnit pu;
		private ProcessingUnitInstance[] puInstanceIncrement;

		public InnerTester(ProcessingUnit pu){
			super();
			this.pu = pu;
			puInstanceIncrement = new ProcessingUnitInstance[1];
		}

		public void run() {
			try{
				int t = 0;
				while(!Thread.currentThread().isInterrupted() && t < TEST_NUM){
					//2: Start additional GSC and relocate petclinic.PU[1] to empty GSC.
					relocate();
					//3: Increase (+1) the petclinic webapp instances.
					increase();
					//4: Decrease (-1) the petclinic webapp instances (third instance)
					decrease();
					//5: Kill the GSC that contains at least one petclinic.PU instance.					
					kill();

					t++;
				}
				log("tester was shutdown");
				barrier.await();
			}
			catch(Throwable th){
				barrier.reset(th);
			}
		}
		//-4.2.2: Start additional GSC and relocate petclinic.PU[1] to empty GSC.
		private void relocate() throws Exception{

			gsc1 = loadGSC(machine);

			final CountDownLatch puAddedLatch = new CountDownLatch(1);
			gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
				public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
					log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
							+ " instantiated on " + gscToString(gsc1));
					puAddedLatch.countDown();
				}
			}, false);
			
			log("waiting for instance to be relocated to " + gscToString(gsc1));
			pu.getInstances()[0].relocateAndWait(gsc1);
			assertEquals(1, gsc1.getProcessingUnitInstances().length);

			assertPageExists("http://"+ip +":8080/petclinic-web-jpa/");
			assertTrue(puAddedLatch.await(60, TimeUnit.SECONDS));
		}
		private void increase() throws Exception{
			final CountDownLatch puInstanceAddedLatch = new CountDownLatch(1);
			pu.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
				public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
					log("event: " + getProcessingUnitInstanceName(processingUnitInstance) + " instantiated");
					puInstanceIncrement[0] = processingUnitInstance;
					puInstanceAddedLatch.countDown();
				}
			}, false);

			int puInstances = pu.getTotalNumberOfInstances();
			pu.incrementInstance();
			assertTrue(puInstanceAddedLatch.await(60, TimeUnit.SECONDS));

			// for some reason pu.getTotalNumberOfInstances() returns 2 at this point.
			// so we write our expectation explicitly (i.e: 2)
			pu.waitFor(puInstances +1);

			assertPageExists("http://"+ip +":8080/petclinic-web-jpa/");
		}
		private void decrease() throws Exception{
			final CountDownLatch puInstanceRemovedLatch = new CountDownLatch(1);
			pu.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
				public void processingUnitInstanceRemoved(ProcessingUnitInstance processingUnitInstance) {
					log("event: " + getProcessingUnitInstanceName(processingUnitInstance) + " removed");
					puInstanceIncrement[0] = processingUnitInstance;
					puInstanceRemovedLatch.countDown();
				}
			});
			int puInstances = pu.getTotalNumberOfInstances();
			puInstanceIncrement[0].decrement();
			assertTrue(puInstanceRemovedLatch.await(60, TimeUnit.SECONDS));
			assertPageExists("http://"+ip +":8080/petclinic-web-jpa/");
			assertEquals(puInstances - 1, gsc1.getProcessingUnitInstances().length);

		}

		private void kill() throws Exception{
			log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gsc1.getProcessingUnitInstances()));
			gsc1.kill();
			log("waiting for processing units to be instantiated on GSC #0");
			pu.waitFor(1);
			int puInstances =pu.getNumberOfInstances();
			Assert.assertTrue(puInstances >= 1);
		}

	}

	

	class InnerReader implements Runnable {
		public InnerReader(){
			super();
		}

		public void run(){

			try {
				read();
				barrier.await();
			}catch(Throwable th){
				barrier.reset(th);
			}
			log("reader was shutdown");
		}

		private void read() throws Exception{
			WebDriver driver = new FirefoxDriver();
			String baseUrl = "http://"+ ip + ":8080/petclinic-web-jpa/";
			final Selenium selenium = new WebDriverBackedSelenium(driver, baseUrl);
			selenium.open(baseUrl);
			int r = 0;
			while(!Thread.currentThread().isInterrupted() && r < READ_NUM){
				selenium.open(baseUrl + "owners");
				boolean found = false;
				int j = 1;
				int i = 0;
				for(j = 1; j <= (OWENER_NUM*USER_NUM + 6) && !found; j++){
					for( i = 0; i <= OWENER_NUM*USER_NUM && !found; i++){
						if(selenium.getText("//div[@id='main']/table[1]/tbody/tr[" + j + "]/td[1]/a").equals(new String(i + " " + i))){
							found = true;
							selenium.click("//div[@id='main']/table[1]/tbody/tr[" + j + "]/td[1]/a");
						}
					}
				}
				Assert.assertEquals("" + (i-1), selenium.getText("//div[@id='main']/table[1]/tbody/tr[3]/td"));
				selenium.open(baseUrl + "owners/search");
				selenium.typeKeys("//div[@id='main']/form[@id='owner']/table/tbody/tr[1]/th/input[@id='lastName']","McTavish");
				selenium.click("//div[@id='main']/form[@id='owner']/table/tbody/tr[2]/td/p/input");
				assertEquals("Madison", selenium.getText("//div[@id='main']/table[1]/tbody/tr[3]/td"));

				r++;
			}
			selenium.close();
		}
	}




	public Integer getIndex() {
		synchronized(this.index){
			index++;
			return index;
		}
	}

}

