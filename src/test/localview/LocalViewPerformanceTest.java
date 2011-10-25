package test.localview;

/**
 * 
 */

import static test.utils.LogUtils.log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DeploymentUtils;
import test.utils.ThreadBarrier;

import com.gigaspaces.localview.Message;
import com.gigaspaces.query.IdQuery;

/**
 * @author Sagi Bernstein
 *
 */
public class LocalViewPerformanceTest extends AbstractTest {
	private static final String SPACE_NAME = "qaSpace";
	private static final String SOURCE_ZONE = "SERVER";
	private static final String TARGET_ZONE = "CLIENT";
	private static final int NUM_OF_CLIENT_AGENTS = 2;
	private final ThreadBarrier barrier = new ThreadBarrier(NUM_OF_CLIENT_AGENTS + 1);
	final Object monitor = new Object();
	private GigaSpace gigaSpace;

	protected final int OBJECTS = 1000;
	protected final static int THREADS = 5;
	protected final int UPDATES_PER_THREAD = 25000;
	protected final int WARM_UP_CONSTANT = 100000;
	
	private int initializationEndedMsgId = WARM_UP_CONSTANT + 1;
	private int timeLogMsgId = WARM_UP_CONSTANT;
	private int numOfClients = 8;
	private GridServiceAgent[] gsas;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void testPerformance8clients() throws Exception{
		testPerformance(8);
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void testPerformance16clients() throws Exception{
		testPerformance(16);
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void testPerformance32clients() throws Exception{
		testPerformance(32);
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void testPerformance64clients() throws Exception{
		testPerformance(64);
	}
	
	public void testPerformance(int numOfClients) throws Exception{
		this.numOfClients = numOfClients;
		initialize();
		gigaSpace = admin.getSpaces().getSpaces()[0].getGigaSpace();
		log("indicating to the feeder that initialization is done");
		gigaSpace.write(new Message(initializationEndedMsgId));		
		log("waiting for the timeLogMsg written by the feeder indicating the end of it's work");
		Message timeLogMsg = gigaSpace.readById(new IdQuery<Message>(Message.class , timeLogMsgId) , DEFAULT_TEST_TIMEOUT);
		log(timeLogMsg.getInfo() + "\n" + "num of clients is: " + numOfClients);
		
		

	}




	private void initialize() throws Exception {
		log("initializing..");
		Assert.assertTrue(admin.getGridServiceAgents().waitFor(3));

		gsas = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaSource = gsas[2];

		gsaSource.startGridService(new GridServiceManagerOptions().vmInputArgument("-Dcom.gs.zones=" + SOURCE_ZONE));
		gsaSource.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=" + SOURCE_ZONE));
		gsaSource.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=" + SOURCE_ZONE));

		final GridServiceManager gsmSource = admin.getGridServiceManagers().waitForAtLeastOne();
		
		AbstractTest.assertNotNull(gsmSource);

		Map<String, String> props = new HashMap<String, String>();

		props.put("SPACE_NAME", SPACE_NAME);
		props.put("updatesPerThread", "" + UPDATES_PER_THREAD);
		props.put("numberOfThreads", "" + THREADS);		
		props.put("DEFAULT_TEST_TIMEOUT", "" + DEFAULT_TEST_TIMEOUT);		
		props.put("initializationEndedMsgId", "" + initializationEndedMsgId);
		props.put("timeLogMsgId", "" + timeLogMsgId);
		props.put("warmUpConstant", "" + WARM_UP_CONSTANT);
		gsmSource.deploy(prepareAndDeployment("embeddedFeeder", props));


		admin.getSpaces().waitFor(SPACE_NAME);

		if(NUM_OF_CLIENT_AGENTS > 0){
			runViewAgent1();
			runViewAgent2();
		}
		barrier.await();
		barrier.inspect();

		admin.getProcessingUnits().waitFor("local-view").waitFor(numOfClients/2);
		admin.getProcessingUnits().waitFor("local-view/").waitFor(numOfClients/2);

		log("finished initialziation");

	}

	private void runViewAgent1() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				GridServiceAgent gsaTarget1 = gsas[1];

				gsaTarget1.startGridService(new GridServiceManagerOptions().vmInputArgument("-Dcom.gs.zones=" + TARGET_ZONE));
				for (int i = 0; i < numOfClients / 8; i++) {
					gsaTarget1.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=" + TARGET_ZONE));
				}

				final GridServiceManager gsmTarget1 = gsaTarget1.getAdmin().getGridServiceManagers().waitForAtLeastOne();
				Assert.assertNotNull(gsmTarget1);


				Map<String, String> props = new HashMap<String, String>();
				props.put("SPACE_NAME", SPACE_NAME);
				props.put("QUERY_CLASS", "com.gigaspaces.localview.Message");
				props.put("WHERE", "processed = false");
					gsmTarget1.deploy(prepareAndDeployment("local-view", props)
							.numberOfInstances(numOfClients / 2).addZone(TARGET_ZONE));
				props.clear();

				try {
					barrier.await();
				} catch (InterruptedException e) {
					barrier.reset();
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					barrier.reset();
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void runViewAgent2() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				GridServiceAgent gsaTarget1 = gsas[0];

				gsaTarget1.startGridService(new GridServiceManagerOptions().vmInputArgument("-Dcom.gs.zones=" + TARGET_ZONE));
				for (int i = 0; i < numOfClients / 8; i++) {
					gsaTarget1.startGridService(new GridServiceContainerOptions().vmInputArgument("-Dcom.gs.zones=" + TARGET_ZONE));
				}

				final GridServiceManager gsmTarget2 = admin.getGridServiceManagers().waitForAtLeastOne();
				AbstractTest.assertNotNull(gsmTarget2);

				Map<String, String> props = new HashMap<String, String>();
				props.put("SPACE_NAME", SPACE_NAME);
				props.put("QUERY_CLASS", "com.gigaspaces.localview.Stock");
				props.put("WHERE", "stockId > -1");
					gsmTarget2.deploy(prepareAndDeployment("local-view/", props)
							.numberOfInstances(numOfClients / 2).addZone(TARGET_ZONE));
				props.clear();

				try {
					barrier.await();
				} catch (InterruptedException e) {
					barrier.reset();
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					barrier.reset();
					e.printStackTrace();
				}
			}
		}).start();
	}


	public static synchronized ProcessingUnitDeployment prepareAndDeployment(String name, Map<String, String> contextProperties) {
		DeploymentUtils.prepareApp("local-view");
		ProcessingUnitDeployment puDeployment = new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("local-view", name)).name(name);
		for (String key : contextProperties.keySet()) {
			puDeployment.setContextProperty(key, contextProperties.get(key));
		}
		return puDeployment;
	}

}
