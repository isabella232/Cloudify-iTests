package test.data;


import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import java.io.Serializable;

public class Message implements Serializable {
    private byte[] m_content;
    private long m_counter = -1;

    public Message() {
    }

    public Message(long initVal, byte[] content) {
        m_content = content;
        m_counter = initVal;
    }

    public void setContent(byte[] content) {
        this.m_content = content;
    }

    public void setCounter(long counter) {
        this.m_counter = counter;
    }

    public byte[] getContent() {
        return m_content;
    }

    @SpaceRouting
    @SpaceProperty(nullValue = "-1")
    public long getCounter() {
        return m_counter;
    }

    @Override
    public String toString() {
        return getClass() + "_" + m_counter + "_" + m_content;
    }
}


