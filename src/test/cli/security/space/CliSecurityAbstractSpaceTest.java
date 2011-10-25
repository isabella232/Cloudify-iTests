package test.cli.security.space;

import static framework.utils.LogUtils.log;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.ScriptUtils;

import test.AbstractTest;

public class CliSecurityAbstractSpaceTest extends AbstractTest {

    protected GridServiceAgent gsa;
    protected GridServiceManager gsm;
    
    @Override
    protected Admin newAdmin() {
        return AdminUtils.createSecuredAdmin("Master", "master");
    }
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
        
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);
        
        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        gsa = admin.getGridServiceAgents().getAgents()[0];


        String securityConfigFilePathKey = "com.gs.security.properties-file";
        String securityConfigFilePathValue = ScriptUtils.getBuildPath()+"/config/security/spring-test-security.properties";
        System.setProperty(securityConfigFilePathKey, securityConfigFilePathValue);
        
        String[] props = {
                "-D"+securityConfigFilePathKey+"="+securityConfigFilePathValue,
                "-Dcom.gs.security.enabled=true"
           };
        
        
        log("starting: 1 GSM and 2 GSC's at 1 machine");
        gsm = AdminUtils.loadGSMWithSystemProperty(gsa.getMachine(), props);
        AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props); 
        AdminUtils.loadGSCWithSystemProperty(gsa.getMachine(), props); 
    }
    
}
