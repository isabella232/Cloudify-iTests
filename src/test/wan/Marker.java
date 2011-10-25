package test.wan;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.annotation.pojo.SpaceRouting;

@SpaceClass
public class Marker {
	public int id = -999;
	public int type = -999;

	public Marker() {
	}
		

	@SpaceRouting
	@SpaceId(autoGenerate=false)
    @SpaceProperty(nullValue = "-999")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	@SpaceProperty(nullValue = "-999")
	public int getType() {
		return type;
	}


	public void setType(int type) {
		this.type = type;
	}
	
}