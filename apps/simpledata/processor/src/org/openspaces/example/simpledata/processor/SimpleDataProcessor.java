package org.openspaces.example.simpledata.processor;

import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.example.simpledata.common.SimpleDataPojo;

public class SimpleDataProcessor {

    @SpaceDataEvent
    public SimpleDataPojo processData(SimpleDataPojo data) {
        data.setRawData(data.getRawData() + "World !!");
        return data;
    }

}
