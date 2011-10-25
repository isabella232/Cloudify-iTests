package com.gatewayPUs.synthProcessor;

import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;

import com.gatewayPUs.common.MessageGW;
import com.gatewayPUs.common.ProcessedMessage;



@Polling
public class GatewaySynthisizingProcessor {
	
	@EventTemplate
    public MessageGW unprocessedDataTemplate() {
		MessageGW messageGW = new MessageGW();
        messageGW.setProcessed(false);
        return messageGW;
    }
	
	@SpaceDataEvent
    public ProcessedMessage processMessage(MessageGW msg) {
        ProcessedMessage ans = new ProcessedMessage();
        ans.setVersion(msg.getVersion());
		return ans;
		
    }
    public GatewaySynthisizingProcessor(){
     System.out.println("Processor instantiated...");
    }
    
}