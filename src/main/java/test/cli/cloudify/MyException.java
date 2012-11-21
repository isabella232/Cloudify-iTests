package test.cli.cloudify;

public class MyException extends Exception{

	private static final long serialVersionUID = 582281928740482981L;
	private String errorMessage;
	
	public MyException(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	@Override
	public String toString(){
		String name = getClass().getName();
		return errorMessage != null ? name + ":" + errorMessage : name;
	}
}
