package com.gigaspaces.common;

import com.gigaspaces.security.Authority;
import com.gigaspaces.security.directory.UserDetails;

public class MyUserDetails implements UserDetails{

	private String username = null;
	private String password = null;
	private Authority[] privileges;
	
	public MyUserDetails(String gsUser, String gsPassword) {
		
		this.username = gsUser;
		this.password = gsPassword;
		
	}
		
	public MyUserDetails(UserDetails userDetails) {
		this.username = userDetails.getUsername();
		this.password = userDetails.getPassword();
		this.privileges = userDetails.getAuthorities();
	}

	public Authority[] getAuthorities() {
		return this.privileges;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
