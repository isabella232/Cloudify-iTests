package test.cli.common;

import com.gigaspaces.annotation.pojo.SpaceRouting;

public class SpaceMessage {
    
    private String name;
    private int num1;
    
    public SpaceMessage() {}
    
    public SpaceMessage(String name, int num1) {
        this.name = name;
        this.num1 = num1;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setNum1(int num1) {
        this.num1 = num1;
    }
    
    @SpaceRouting
    public int getNum1() {
        return num1;
    }
    
}
