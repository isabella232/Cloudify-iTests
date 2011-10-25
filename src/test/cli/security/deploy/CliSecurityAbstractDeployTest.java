package test.cli.security.deploy;

import static framework.utils.LogUtils.log;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.ScriptUtils;

import test.AbstractTest;

public class CliSecurityAbstractDeployTest extends AbstractTest {

    protected GridServiceManager gsm;
    
    @Override
    protected Admin newAdmin() {
        return AdminUtils.createSecuredAdmin("Master", "master");
    }
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        log("Waiting for 1 GSA");
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        
        //Needed so GS.main could work properly
        String groupKey = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(groupKey, value);

        String securityConfigFilePathKey = "com.gs.security.properties-file";
        String securityConfigFilePathValue = ScriptUtils.getBuildPath()+"/config/security/spring-test-security.properties";
        System.setProperty(securityConfigFilePathKey, securityConfigFilePathValue);

        String[] props = {
             "-D"+securityConfigFilePathKey+"="+securityConfigFilePathValue,
             "-Dcom.gs.security.enabled=true"
        };
        
        log("Loading 1 GSM and 1 GSC");
        gsm = AdminUtils.loadGSMWithSystemProperty(gsa.getMachine(), props);
        AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props); 
        
    }
}
