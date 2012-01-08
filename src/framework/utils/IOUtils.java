package framework.utils;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

/**
 * A set of utility methods for IO manipulation
 * @author elip
 *
 */
public class IOUtils {
	
	/**
	 * 
	 * @param oldFile
	 * @param newFile
	 * @throws IOException
	 */
	public static void replaceFile(String oldFile, String newFile) throws IOException {
		File old = new File(oldFile);
		old.delete();
		Files.copy(new File(newFile), new File(oldFile));
	}

}
