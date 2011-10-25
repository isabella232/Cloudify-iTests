package com.gigaspaces;

public class MyException extends Exception{

    private static final long serialVersionUID = 6804589365020732202L;
    String info;
	
	public MyException() { }
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	
}
