package com.gigaspaces.common;



import org.openspaces.security.spring.SpringSecurityManager;

import com.gigaspaces.security.Authentication;
import com.gigaspaces.security.AuthenticationException;
import com.gigaspaces.security.SecurityException;
import com.gigaspaces.security.directory.UserDetails;

public class MySecurityManager extends SpringSecurityManager{
	
	@Override
	public Authentication authenticate(UserDetails userDetails)
			throws AuthenticationException {
		 
		Authentication authentication = super.authenticate(userDetails);
		if (userDetails instanceof MyUserDetails) {
			MyUserDetails myUserDetails = new MyUserDetails(authentication.getUserDetails());
			return new Authentication(myUserDetails);
		} else {
			throw new SecurityException("userDetails is of class: "
					+ userDetails.getClass()
					+ " and not of class: MyUserDetails");
		}
	}
}