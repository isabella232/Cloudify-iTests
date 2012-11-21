package framework.utils.usm;

/**************
 * A simple class used to test recipe imports in the USM.
 * @author barakme
 *
 */

public class StringWrapper {

	private String wrappedString;
	
	public StringWrapper(final String wrapperString) {
		this.wrappedString = wrapperString;
	}
	
	public String toString() {
		
		return this.wrappedString;
	}
}

