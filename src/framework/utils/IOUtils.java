package framework.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

/**
 * A set of utility methods for IO manipulation
 * @author elip, sagi
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

	public static void replaceTextInFile(String FileName, String oldText, String newText) throws IOException {
		HashMap<String, String> toPass = new HashMap<String, String>();
		toPass.put(oldText, newText);
		replaceTextInFile(FileName, toPass);
	}
	
	public static void replaceTextInFile(String filePath, Map<String, String> map) throws IOException {
		LogUtils.log("replacing props is file : " + filePath);
		File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = "", oldtext = "";
        while((line = reader.readLine()) != null)
            {
            oldtext += line + "\r\n";
        }
        reader.close();
        String newtext = new String(oldtext);
        for (String toReplace : map.keySet()) {
        	LogUtils.log("replacing " + toReplace + " with " + map.get(toReplace));
        	newtext = newtext.replaceAll(toReplace, map.get(toReplace));
        }
       
        FileWriter writer = new FileWriter(filePath);
        writer.write(newtext);
        writer.close();
	}
	
	public static void replaceTextInFile(File file, Map<String,String> map) throws IOException {
		String originalFileContents = FileUtils.readFileToString(file);
		String modified = originalFileContents;
		for (String s : map.keySet()) {
			modified = modified.replace(s, map.get(s));
		}
		FileUtils.write(file, modified);
	}
	
	public static File writePropertiesToFile(final Properties props , final File destinationFile) throws IOException {
		Properties properties = new Properties();
		for (Entry<Object, Object> entry : props.entrySet()) {
			properties.setProperty(entry.getKey().toString(), entry.getValue().toString());
		}
		FileOutputStream fileOut = new FileOutputStream(destinationFile);
		properties.store(fileOut,null);
		fileOut.close();
		String readFileToString = FileUtils.readFileToString(destinationFile);
		destinationFile.delete();
		FileUtils.writeStringToFile(destinationFile, readFileToString.replaceAll("#", "//"));
		return destinationFile;

	}
}


