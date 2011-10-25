package com.gigaspaces.example;

import java.io.Serializable;

public interface IResult extends Serializable {

    public int getCode();
    
    public String getDescription();
}
