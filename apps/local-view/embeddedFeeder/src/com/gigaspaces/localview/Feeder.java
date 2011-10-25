package com.gigaspaces.localview;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.space.mode.PostPrimary;
import org.springframework.beans.factory.annotation.Autowired;

import com.gigaspaces.localview.Message;
import com.gigaspaces.localview.Stock;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.internal.utils.Assert;
import com.gigaspaces.query.IdQuery;


public class Feeder {

	private ExecutorService executorService;
	private GatewayFeederTask gatewayFeederTask;
	private ThreadBarrier barrier; 
	private int updatesPerThread;
	private int numberOfThreads;
	private int warmUpConstant;
	private int DEFAULT_TEST_TIMEOUT;
	private int initializationEndedMsgId;
	private int timeLogMsgId;
	int uniqeObjectIdToUpdate = 0;
	
	@Autowired
	private GigaSpace gigaSpace;
	
	@PostPrimary
	public void construct()  {   
		
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					barrier = new ThreadBarrier(numberOfThreads + 1);
					// wait until initialization has ended
					
					Message initializationEndedMsg = gigaSpace.readById(new IdQuery<Message>(Message.class , initializationEndedMsgId) ,DEFAULT_TEST_TIMEOUT);
					Assert.notNull(initializationEndedMsg, "initializationEndedMsg could not be read from sourceSpace by the embeddedFeeder");
					// warm up write
					for(int i = 0; i<warmUpConstant; i++){
						Message msg = new Message(i);
						Stock stock = new Stock(i);
						gigaSpace.write(msg);
						gigaSpace.write(stock);
//						System.out.println("--- FEEDER WROTE " + msg + " and " + stock);
					}
					// continues update
					new MyExecutor().execute(new GatewayFeederTask());
					long initTime = System.currentTimeMillis();
					barrier.await();
					barrier.await();
					double totalUpdateTimeInSeconds =(System.currentTimeMillis()-initTime)/1000.0;
					long roundThroughPut = Math.round((updatesPerThread*numberOfThreads)/totalUpdateTimeInSeconds);
					
					Message timeLoggingMsg = new Message(timeLogMsgId, "Through put is: " + (2 * roundThroughPut) + 
							"[Objects/sec] when each thread is updating: " +updatesPerThread + " objects (" + numberOfThreads + 
							" threads) after a warm up write of " + warmUpConstant + " objects");
					
					gigaSpace.write(timeLoggingMsg);
//					System.out.println("--- FEEDER WROTE " + timeLoggingMsg);
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	@PreDestroy
	public void destroy() {
		executorService.shutdown();
	}

	
	@SpaceProperty(nullValue="0")
	public Integer getNumberOfMessages() {
		return updatesPerThread;
	}
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public int getDEFAULT_TEST_TIMEOUT() {
		return DEFAULT_TEST_TIMEOUT;
	}

	public void setDEFAULT_TEST_TIMEOUT(int dEFAULT_TEST_TIMEOUT) {
		DEFAULT_TEST_TIMEOUT = dEFAULT_TEST_TIMEOUT;
	}

	public int getInitializationEndedMsgId() {
		return initializationEndedMsgId;
	}

	public int getUpdatesPerThread() {
		return updatesPerThread;
	}

	public int getWarmUpConstant() {
		return warmUpConstant;
	}

	public void setWarmUpConstant(int warmUpConstant) {
		this.warmUpConstant = warmUpConstant;
	}

	public void setUpdatesPerThread(int updatesPerThread) {
		this.updatesPerThread = updatesPerThread;
	}

	public void setInitializationEndedMsgId(int initializationEndedMsgId) {
		this.initializationEndedMsgId = initializationEndedMsgId;
	}

	public int getTimeLogMsgId() {
		return timeLogMsgId;
	}

	public void setTimeLogMsgId(int timeLogMsgId) {
		this.timeLogMsgId = timeLogMsgId;
	}

	public void setNumberOfMessages(Integer numberOfMessages) {
		this.updatesPerThread = numberOfMessages;
	}
	
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public void setGatewayFeederTask(GatewayFeederTask gatewayFeederTask) {
		this.gatewayFeederTask = gatewayFeederTask;
	}

	public GatewayFeederTask getGatewayFeederTask() {
		return gatewayFeederTask;
	}

	private class GatewayFeederTask implements Runnable {
		@Override
		public void run() {
			try {
				int uniqeObjectIdToUpdate = (int) (Math.abs(new Random().nextInt()) % warmUpConstant); // randomly choosing an id - high chance to choose uniquely
				barrier.await();

				for(int i = 0; i<updatesPerThread; i++){
					Message msg = new Message(uniqeObjectIdToUpdate, "FEEDER updated for the " + i + "-th time");
					Stock stock = new Stock(uniqeObjectIdToUpdate);
					stock.setStockName("FEEDER updated for the " + i + "-th time");
					gigaSpace.write(msg);
					gigaSpace.write(stock);
				}
				barrier.await();
			}  catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class MyExecutor implements Executor{

		@Override
		public void execute(Runnable command) {
			
			for(int i=0 ; i < numberOfThreads ; i++)
				new Thread(command).start();
		}
	}
}