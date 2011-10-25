package com.gatewayPUs.common;

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
import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting; 
import com.gigaspaces.annotation.pojo.SpaceVersion;

/**
 * A simple object used to work with the Space. 
 */
@SpaceClass
public class ProcessedMessage  {

    private Integer id;
    private String info;
    boolean processed = false;
    private int version;
    
    /**
     * Necessary Default constructor
     */
    public ProcessedMessage() {            
    }
    
    /**
     * Constructs a new Message with info
     */
    public ProcessedMessage(String info) {
        this.info = info;
    }

    /**
     * Constructs a new Message with the given id and info
     * and info.
     */
    public ProcessedMessage(Integer id, String info) {
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

    /**
	 * A simple toString to print out the state.
     */
    public String toString() {
        return "id[" + id + "] info[" + info +"]";
    }
    
    

	/**
	 * @return the version
	 */
    @SpaceVersion
	public int getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the proccesd
	 */
	public boolean isProcessed() {
		return processed;
	}

	/**
	 * @param proccesd the proccesd to set
	 */
	public void setProcessed(boolean proccesd) {
		this.processed = proccesd;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((info == null) ? 0 : info.hashCode());
		result = prime * result + (processed ? 1231 : 1237);
		result = prime * result + version;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessedMessage other = (ProcessedMessage) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
			return false;
		if (processed != other.processed)
			return false;
		if (version != other.version)
			return false;
		return true;
	}


    
    
}
