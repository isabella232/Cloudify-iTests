package com.gigaspaces.example.dto;

import com.gigaspaces.example.IParameter;

@SuppressWarnings("serial")
public class SimpleParameter implements IParameter, Comparable<IParameter> {

    private String value;
    
    
    public SimpleParameter() {
    }
    
    public SimpleParameter( String val ) {
        value = val;
    }
       
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String val) {
        value = val;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        SimpleParameter other = (SimpleParameter) obj;
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
        return "SimpleParameter [value=" + value + "]";
    }

    public int compareTo(IParameter o) {
         return value.compareTo(o.getValue());
    }
}
