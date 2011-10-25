package com.gigaspaces.ps.hibernateimpl.common;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.annotation.pojo.SpaceProperty.IndexType;

@SpaceClass
public class Account{
	private String firstName;

	private String lastName;

	private Integer id;

	// must have empty constructor
	public Account() {
	}
 
	public Account(String firstName, String lastName, Integer id) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.id = id;
		
	}

	public String toString() {
		return "ID:" + id + " firstName:" + firstName + " lastName:" + lastName;
	}

	@SpaceProperty(index=IndexType.BASIC)
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@SpaceRouting
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@SpaceId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
