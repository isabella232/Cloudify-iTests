package com.gatewayPUs.embeddedFeeder20Targets;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.gatewayPUs.embeddedFeeder20Targets.ThreadBarrier;

import com.gatewayPUs.common.MessageGW;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.internal.utils.Assert;
import com.gigaspaces.query.IdQuery;


public class EmbeddedGatewayFeeder20Targets{

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
	
	@PostConstruct
	public void construct()  {   
		
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					barrier = new ThreadBarrier(numberOfThreads + 1);
					// wait until initialization has ended
					
					MessageGW initializationEndedMsg = gigaSpace.readById(new IdQuery<MessageGW>(MessageGW.class , initializationEndedMsgId) ,DEFAULT_TEST_TIMEOUT);
					Assert.notNull(initializationEndedMsg, "initializationEndedMsg could not be read from sourceSpace by the embeddedFeeder");
					// warm up write
					for(int i = 0; i<warmUpConstant; i++){
						MessageGW msg = new MessageGW(i);
						msg.setId(new Integer(i));
						gigaSpace.write(msg);
					}
					// continues update
					new MyExecutor().execute(new GatewayFeederTask());
					long initTime = System.currentTimeMillis();
					barrier.await();
					barrier.await();
					double totalUpdateTimeInSeconds =(System.currentTimeMillis()-initTime)/1000.0;
					long roundThroughPut = Math.round((updatesPerThread*numberOfThreads)/totalUpdateTimeInSeconds);
					
					MessageGW timeLoggingMsg = new MessageGW(timeLogMsgId, "Through put is: " + roundThroughPut + 
							"[Objects/sec] when each thread is updating: " +updatesPerThread + " objects (" + numberOfThreads + 
							" threads) after a warm up write of " + warmUpConstant + " objects");
					
					gigaSpace.write(timeLoggingMsg);
					System.out.println("--- FEEDER WROTE " + timeLoggingMsg);
					
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
					MessageGW msg = new MessageGW(uniqeObjectIdToUpdate, "FEEDER updated for the " + i + "-th time");
					msg.setId(new Integer(uniqeObjectIdToUpdate));
					gigaSpace.write(msg);
				}
				barrier.await();
			}  catch (Exception e) {
				barrier.reset(e);
			}
		}
	}
	
	private class MyExecutor implements Executor{

		@Override
		public void execute(Runnable command) {
			
			for(int i=0 ; i<numberOfThreads ; i++)
				new Thread(command).start();
		}
	}
}