package framework.tools;


import java.io.File;

import test.utils.ScriptUtils;

public class SGTestHelper {

    /**
     * This is a very cool method which returns jar or directory from where the supplied class has loaded.
     *
     * @param claz the class to get location.
     * @return jar/path location of supplied class.
     */
    public static String getClassLocation( Class claz )
    {
        return claz.getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
    }

    /** @return SGTest root directory */
    public static String getSGTestRootDir()
    {
        String tmp;
        String sgTestJarPath = getClassLocation( SGTestHelper.class );
        String sgTestRootDir = new File(sgTestJarPath).getParent();
        tmp = sgTestRootDir.toLowerCase();
        if(!tmp.endsWith("sgtest"))
             sgTestRootDir = new File(sgTestJarPath).getParentFile().getParent();
        return sgTestRootDir;
    }
    
    public static String getBuildDir(){
        return ScriptUtils.getBuildPath();
    }
}
