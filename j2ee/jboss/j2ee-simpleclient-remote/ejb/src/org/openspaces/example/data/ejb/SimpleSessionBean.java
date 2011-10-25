package org.openspaces.example.data.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.openspaces.core.GigaSpace;

import test.data.Data;

@Stateless(name="SimpleSession")
@Local(SimpleSession.class)
public class SimpleSessionBean {

	@PostConstruct
	public void init() { }

	public Data read(String id) {
		GigaSpace gigaSpace = SpaceSingleton.getInstance().getSpace();
		Data data = new Data();
		data.setId(id);
		return gigaSpace.read(data);
	}

	public void write(String id) {
		GigaSpace gigaSpace = SpaceSingleton.getInstance().getSpace();
		Data data = new Data();
		data.setId(id);
		data.setType(Integer.parseInt(id));
		data.setData("J2EE data");
		gigaSpace.write(data);
	}
	
	public int count() {
	    GigaSpace gigaSpace = SpaceSingleton.getInstance().getSpace();
	    int count = gigaSpace.count(new Data());
	    return count;
	}

}
