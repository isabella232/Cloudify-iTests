/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gigaspaces.common;

import java.io.Serializable;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * A simple object used to work with the Space. 
 */
public class MessageSecure  implements Serializable{

    private Integer id;
	
    private String info;
    private Boolean processed = false;

	/**
     * Necessary Default constructor
     */
    public MessageSecure() {            
    }

    /**
     * Constructs a new Message with the given id and info
     * and info.
     */
    public MessageSecure(Integer id, String info) {
        this.id = id;
        this.info = info;
    }

    /**
     * The id of this message.
     * We will use this attribute to route the message objects when 
     * they are written to the space, defined in the Message.gs.xml file.
     */
	@SpaceRouting 
	@SpaceId
    public Integer getId() {
        return id;
    }

    /**
     * The id of this message. 
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * The information this object holds.
     */
    public String getInfo() {
        return info;
    }

    /**
     * The information this object holds.
     */
    public void setInfo(String info) {
        this.info = info;
    }
    
    public Boolean getProcessed() {
		return processed;
	}

	public void setProcessed(Boolean processed) {
		this.processed = processed;
	}


    /**
	 * A simple toString to print out the state.
     */
    public String toString() {
        return "id[" + id + "] info[" + info +"] processed[" + processed + "]";
    }
}
