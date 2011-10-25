package com.gigaspaces;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;


public class MyPu implements InitializingBean,DisposableBean{

	public void afterPropertiesSet() throws Exception {
		throw new MyException();
		
	}

	public void destroy() throws Exception {
		
	}

}
