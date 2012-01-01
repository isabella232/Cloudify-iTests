package framework.utils.usm;

/***********
 * This is just a basic exception class used in USM recipes.
 * @author barakme
 *
 */
public class MyUSMException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public MyUSMException() {
		super();
	}

	public MyUSMException(String message, Throwable cause) {
		super(message, cause);
	}

	public MyUSMException(String message) {
		super(message);
	}

	public MyUSMException(Throwable cause) {
		super(cause);
	}

	

}
