package com.gigaspaces.example.dto;

import java.io.Serializable;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

@SpaceClass
@SuppressWarnings("serial")
public class SimpleEntry implements Serializable {
    
    private Integer id;
    
    private String value;
    
    
    public SimpleEntry() {        
    }    


    @SpaceId(autoGenerate=false)
    public Integer getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setId(Integer val) {
        id = val;
    }

    public void setValue(String val) {
        value = val;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleEntry other = (SimpleEntry) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "SimpleEntry [id=" + id + ", value=" + value + "]";
    }
}
