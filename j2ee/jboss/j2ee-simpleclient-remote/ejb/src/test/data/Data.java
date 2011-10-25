package test.data;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceProperty;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import java.io.Serializable;

@SuppressWarnings("serial")
@SpaceClass
public class Data implements Serializable {

    private String id;

    private int type = -999;

    private String data;

    public Data() { }

    public Data(int type) {
        this.type = type;
    }

    @SpaceId(autoGenerate=false)
    @SpaceProperty(nullValue = "")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SpaceRouting
    @SpaceProperty(nullValue = "-999")
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    @SpaceProperty(nullValue = "")
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
