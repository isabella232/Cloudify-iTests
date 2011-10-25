package org.openspaces.example.simpledata.common;

import java.io.Serializable;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

	@SpaceClass
	public class SimpleDataPojo implements Serializable {

	    private Long id;


	    private String rawData;


	    /**
	     * Constructs a new Data object.
	     */
	    public SimpleDataPojo() {

	    }

	    /**
	     * Constructs a new Data object with the given type
	     * and raw data.
	     */
	    public SimpleDataPojo(String rawData) {
	        this.rawData = rawData;
	    }

	    /**
	     * The id of this object.
	     */
	    @SpaceId
	    public Long getId() {
	        return id;
	    }

	    /**
	     * The id of this object. Its value will be auto generated when it is written
	     * to the space.
	     */
	    public void setId(Long id) {
	        this.id = id;
	    }

	    /**
	     * The raw data this object holds.
	     */
	    public String getRawData() {
	        return rawData;
	    }

	    /**
	     * The raw data this object holds.
	     */
	    public void setRawData(String rawData) {
	        this.rawData = rawData;
	    }

	    public String toString() {
	        return "id[" + id + "] rawData[" + rawData + "]";
	    }
	}

