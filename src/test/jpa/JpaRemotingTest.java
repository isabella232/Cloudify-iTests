package test.jpa;

import java.io.File;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.ExecutorRemotingProxyConfigurer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.AbstractTestSuite;
import test.jpa.objects.JpaObject;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;


/**
 * JPA through space remoting test.
 * Verifies that when JPA is used through remoting, a single space proxy is used and not a clustered one.
 * 
 * @author idan
 * @since 8.0.1
 *
 */
public class JpaRemotingTest extends AbstractTestSuite {

    private GigaSpace gigaSpace;
    private GridServiceManager gsm;
	
    @Override
    @BeforeClass
    public void beforeClass(){
    	super.beforeClass();
    	GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
    	
    	gsm = AdminUtils.loadGSM(gsa);
    	AdminUtils.loadGSC(gsa);
    	AdminUtils.loadGSC(gsa);
    	
    	File puFile = DeploymentUtils.getArchive("jpa-remoting-service.jar");
    	ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(puFile));
        gigaSpace = pu.waitForSpace().getGigaSpace();
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testJpaRemotingEmbeddedProxy() {
        JpaObject obj1 = new JpaObject(10);
        JpaObject obj2 = new JpaObject(11);        
        gigaSpace.write(obj1);
        gigaSpace.write(obj2);
        
		JpaService service = new ExecutorRemotingProxyConfigurer<JpaService>(
				gigaSpace, JpaService.class).broadcast(true)
				.remoteResultReducer(new JpaCountEntityReducer()).proxy();
        
        Integer count = service.getEntityCount(JpaObject.class);
        Assert.assertEquals(new Integer(2), count);
        
        count = service.getEntityCountById(JpaObject.class, obj1.getId());
        Assert.assertEquals(new Integer(1), count);
    }
    
}
