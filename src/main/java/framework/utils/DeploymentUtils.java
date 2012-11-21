package framework.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import framework.tools.SGTestHelper;


public class DeploymentUtils {

    private final static String ARCHIVES = SGTestHelper.getSGTestRootDir() + "/apps/archives/";

    public static File prepareApp(String appName) {
        File appsFolder = new File("./apps/" + appName);
        File metaCommonLib = new File("./apps/meta-common/lib");
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
    	File srcFile = new File("./apps/" + appStr + "/" + puStr + "/src/META-INF/spring/pu-slcache.xml");
    	File destFile = new File("./apps/" + appStr + "/" + puStr + "/target/" + puStr + "/META-INF/spring/pu.xml");
    	copyFile(srcFile, destFile);
	}

    public static File getProcessingUnit(String app, String pu) {
        return new File("./apps/" + app + "/" + pu + "/target/" + pu);
    }

    public static File getArchive(String pu) {
        return new File(ARCHIVES + pu);
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

	

	
}
