package com.gigaspaces.processor;

import org.openspaces.events.adapter.SpaceDataEvent;

import com.gigaspaces.common.MessageSecure;

public class Processor {
	
	public Processor(){
	
	}
	
	@SpaceDataEvent
    public MessageSecure processMessage(MessageSecure msg) {
        msg.setInfo(msg.getInfo() + "World " + msg.getId() + "!!");
        return msg;
    }
}
