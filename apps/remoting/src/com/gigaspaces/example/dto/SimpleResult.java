package com.gigaspaces.example.dto;

import com.gigaspaces.example.IResult;


@SuppressWarnings("serial")
public class SimpleResult implements IResult {
    
    private int code;
    
    private String description;
    
        
    public SimpleResult() {        
    }

    
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @param code the code to set
     */
    public void setCode(int c) {
        code = c;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String d) {
        description = d;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
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
        SimpleResult other = (SimpleResult) obj;
        if (code != other.code) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "SimpleResult [code=" + code + ", description=" + description + "]";
    }
}
