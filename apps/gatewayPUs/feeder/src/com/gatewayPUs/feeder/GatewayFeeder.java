package com.gatewayPUs.feeder;

import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.gatewayPUs.common.MessageGW;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceInterruptedException;
import org.openspaces.core.context.GigaSpaceContext;

import com.gigaspaces.annotation.pojo.SpaceProperty;



public class GatewayFeeder{

	    private ExecutorService executorService;
	    private int numberOfMessages;
	    private GatewayFeederTask gatewayFeederTask;
	    

		@GigaSpaceContext(name = "processor")
	    private GigaSpace gigaSpace;

	    @PostConstruct
	    public void construct() {   
	        Thread t = new Thread(new GatewayFeederTask());
	        t.start();
	    }

	    @PreDestroy
	    public void destroy() {
	        executorService.shutdown();
	    }
	    
	    /**
		 * @return the numberOfMessages
		 */
	    @SpaceProperty(nullValue="0")
		public Integer getNumberOfMessages() {
			return numberOfMessages;
		}

		/**
		 * @param numberOfMessages the numberOfMessages to set
		 */
		public void setNumberOfMessages(Integer numberOfMessages) {
			this.numberOfMessages = numberOfMessages;
		}

	    public void setGatewayFeederTask(GatewayFeederTask gatewayFeederTask) {
			this.gatewayFeederTask = gatewayFeederTask;
		}

		public GatewayFeederTask getGatewayFeederTask() {
			return gatewayFeederTask;
		}

		public class GatewayFeederTask implements Runnable {

	        private int counter;

	        public void run() {
	            try {
	            	for(counter = 0; counter<numberOfMessages; counter++){
	                long time = System.currentTimeMillis();
	                MessageGW msg = new MessageGW(counter++, "FEEDER " + Long.toString(time));
	                msg.setId(new Integer(counter));
	                gigaSpace.write(msg);
	                System.out.println("--- FEEDER WROTE " + msg);
	                }
	            } catch (SpaceInterruptedException e) {
	                // ignore, we are being shutdown
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }

	    }

	 
	}