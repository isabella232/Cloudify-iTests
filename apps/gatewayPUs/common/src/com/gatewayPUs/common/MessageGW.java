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
public class MessageGW implements Comparable<MessageGW>  {

    private Integer id;
    private String info;
    private boolean processed = false;
    private int version;
    
    /**
     * Necessary Default constructor
     */
    public MessageGW() {
    }


    public MessageGW(Integer id) {
        this.id = id;
    }

    public MessageGW(boolean processed) {
        this.processed = processed;
    }

    /**
     * Constructs a new MessageGW with info
     */
    public MessageGW(String info) {
        this.info = info;
    }

    /**
     * Constructs a new MessageGW with the given id and info
     * and info.
     */
    public MessageGW(Integer id, String info) {
        this.id = id;
        this.info = info;
    }

    /**
     * The id of this message.
     * We will use this attribute to route the message objects when 
     * they are written to the space, defined in the MessageGW.gs.xml file.
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


    
    

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MessageGW [id=" + id + ", info=" + info + ", processed="
				+ processed + ", version=" + version + "]";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageGW messageGW = (MessageGW) o;

        if (processed != messageGW.processed) return false;
        if (version != messageGW.version) return false;
        if (id != null ? !id.equals(messageGW.id) : messageGW.id != null) return false;
        if (info != null ? !info.equals(messageGW.info) : messageGW.info != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (info != null ? info.hashCode() : 0);
        result = 31 * result + (processed ? 1 : 0);
        result = 31 * result + version;
        return result;
    }

    public int compareTo(MessageGW o) {
		return this.id.compareTo(o.id);
	}


    
    
}
