package com.gatewayPUs.processorAsyncPersistent;

import com.gatewayPUs.common.MessageGW;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;

import java.util.logging.Logger;

@Polling
public class GatewayAlternigProcessor {
	Logger logger=Logger.getLogger(this.getClass().getName());

	@EventTemplate
    public MessageGW unprocessedDataTemplate() {
		MessageGW messageGW = new MessageGW();
        messageGW.setProcessed(false);
        return messageGW;
    }
	
	@SpaceDataEvent
    public MessageGW processMessage(MessageGW msg) {
		logger.info("Processor PROCESSING: " + msg);
        msg.setProcessed(true);
		return msg;
		
    }
    public GatewayAlternigProcessor(){
    	logger.info("Processor instantiated, waiting for messages...");
    }

}