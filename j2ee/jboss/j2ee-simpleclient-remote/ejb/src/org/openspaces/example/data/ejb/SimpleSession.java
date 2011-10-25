package org.openspaces.example.data.ejb;

import test.data.Data;

public interface SimpleSession {
	public Data read(String id);
	public void write(String id);
	public int count();
}
