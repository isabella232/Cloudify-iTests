package framework.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;


public class DeploymentUtils {

    private static final String SGTEST_VERSION_PROP = "sgtest.version";
    private static final String STATIC_SGTEST_VERSION = "3.0.0-SNAPSHOT";

    public static File prepareApp(String appName) {
        File appsFolder = new File("src/main/resources/apps/" + appName);
        File metaCommonLib = new File("src/main/resources/apps/meta-common/lib");
        File metaCommonTarget = new File(metaCommonLib.getParentFile() + "/target");
        File commonLib = new File(appsFolder.getAbsolutePath() + "/common/lib");
        File commonTarget = new File(commonLib.getParentFile() + "/target");

        File target = null;
        File lib;
        
        for (File f : appsFolder.listFiles()) {
            if (f.isDirectory() && !f.getName().contains("common") && !f.getName().contains(".svn")) {
                target = new File(f.getAbsolutePath() + "/target/" + f.getName());
                target.mkdir();
                lib = new File(target.getAbsolutePath() + "/lib");
                lib.mkdir();
                File puLib = new File(f.getAbsolutePath() + "/lib");
                File metainfSpringDir = new File(f.getAbsolutePath() + "/src/META-INF/spring");

                try {
                    if (metaCommonLib.exists())
                        copyLibs(metaCommonLib, lib);
                    if (commonLib.exists())
                        copyLibs(commonLib, lib);
                    if (puLib.exists())
                        copyLibs(puLib, lib);
                    copyClasses(new File(metaCommonTarget.getAbsolutePath() + "/classes"), target);
                    copyClasses(new File(commonTarget.getAbsolutePath() + "/classes"), target);
                    copyClasses(new File(f.getAbsolutePath() + "/target/classes"), target);
                    copyDirectory(metainfSpringDir, target);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return target;
    }
    
    public static void setPUSLCache(String appStr, String puStr) throws IOException {
    	File srcFile = new File("src/main/resources/apps/" + appStr + "/" + puStr + "/src/META-INF/spring/pu-slcache.xml");
    	File destFile = new File("src/main/resources/apps/" + appStr + "/" + puStr + "/target/" + puStr + "/META-INF/spring/pu.xml");
    	copyFile(srcFile, destFile);
	}

    public static File getProcessingUnit(String app, String pu) {
        return new File("src/main/resources/apps/" + app + "/" + pu + "/target/" + pu);
    }

    public static File getArchive(String pu) {
        String s = System.getProperty("file.separator");
        String version = getSGTestVersion();
        String[] dotSplit = pu.split("\\.");
        String name = dotSplit[0];
        String type = dotSplit[dotSplit.length - 1];
        return new File(getAppsPath(s) + "archives" + s + name + s + version + s + name + "-" + version + "." + type);
    }
    
    public static String getSGTestVersion() {
        String versionFromSystem = System.getProperty(SGTEST_VERSION_PROP);
        if(versionFromSystem == null){
            String versionFromFile = loadPropertiesFromClasspath("sgtest.properties").getProperty(SGTEST_VERSION_PROP);
            if(versionFromFile == null){
                return STATIC_SGTEST_VERSION;
            }
            else{ 
                return versionFromFile;
            }
        }
        else{
            return versionFromSystem;
        }
    }
    
    public static Properties loadPropertiesFromClasspath(String classpath) {
        InputStream is = ClassLoader.getSystemResourceAsStream(classpath);
        Properties props = new Properties();
        try {
            props.load(is);
        } catch (IOException e ) {
            e.printStackTrace();
        }
        catch (RuntimeException e ) {
            e.printStackTrace();
        }
        return props;
    }
    
    public static String getAppsPath(String s) {
        return getLocalRepository() + "repository" + s + "com" + s + "gigaspaces" + s + "quality" + s + "cloudify" + s + "sgtest" + s + "apps" + s;
    }

    private static void copyLibs(File source, File target) throws IOException {
        copyDirectory(source, target);
    }

    private static void copyClasses(File source, File target) throws IOException {
        copyDirectory(source, target);
    }

    private static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                dstDir.mkdir();
            }

            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) {
                if (children[i].equals(".svn"))
                    continue;
                copyDirectory(new File(srcDir, children[i]),new File(dstDir, children[i]));
            }
        } else {
            copyFile(srcDir, dstDir);
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    

    public static String getLocalRepository() {
        String s = System.getProperty("file.separator");
        return System.getProperty("user.home") + s + ".m2" + s;
    }

	
}
