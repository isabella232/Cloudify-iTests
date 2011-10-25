package test.remoting;

import static test.utils.LogUtils.log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.EventDrivenRemotingProxyConfigurer;
import org.openspaces.remoting.ExecutorRemotingProxyConfigurer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.AbstractTestSuite;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

import com.gigaspaces.example.IParameter;
import com.gigaspaces.example.IResult;
import com.gigaspaces.example.ITestService;
import com.gigaspaces.example.dto.SimpleParameter;

public class RemotingTest extends AbstractTestSuite{
	
    private GigaSpace gigaSpace;
    private ITestService testService;
    
    @Override
    @BeforeClass
    public void beforeClass(){
    	super.beforeClass();
    	GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
    	GridServiceManager gsm = AdminUtils.loadGSM(gsa);
    	AdminUtils.loadGSC(gsa);
    	File puFile = DeploymentUtils.getArchive("RemotingServicePu.jar");
    	ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(puFile));
        gigaSpace = pu.waitForSpace().getGigaSpace();
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testUsingEventDrivenProxy(){
    	testService = new EventDrivenRemotingProxyConfigurer<ITestService>(gigaSpace, ITestService.class).proxy();
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testUsingExecutorProxy(){
    	testService = new ExecutorRemotingProxyConfigurer<ITestService>(gigaSpace, ITestService.class).proxy();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testsBody(){
    	int actionCounter = 0 ;
    	
    	Collection<IParameter> parameters = new HashSet<IParameter>();
        parameters.add(new SimpleParameter("one"));
        parameters.add(new SimpleParameter("two"));
        log("Calling service with parameters " + parameters);
        IResult result = testService.add(parameters);
        Assert.assertEquals(result.getCode(),parameters.size());
        actionCounter++;
        
        IParameter [] parametersArray  = new IParameter [] {new SimpleParameter("three"), new SimpleParameter("four")} ;
        log("Calling service with parameters " + parametersArray);
        result = testService.add(parametersArray);
        Assert.assertEquals(result.getCode(),parametersArray.length * 2);
        actionCounter++;
        
        IParameter parameter1 = new SimpleParameter("five");
        IParameter parameter2 = new SimpleParameter("six");
        log("Calling service with parameters " + parameters + ", " + parameter2);
        result = testService.modify(parameters, parameter2);
        Assert.assertEquals(result.getCode(),actionCounter + parameters.size() + 2);
        actionCounter++;
        
        log("Calling service with parameter " + parameter1);
        result = testService.add(parameter1);
        Assert.assertEquals(result.getCode(),actionCounter);
        actionCounter++;
        
        log("Calling service with parameters " + parameter2 + ", " + parameters);
        result = testService.modify(parameter2, parameters);
        Assert.assertEquals(result.getCode(),actionCounter + parameters.size() + 8);
        actionCounter++;
        
        log("Calling service with parameters " + parameter1 + ", " + parameter2);
        result = testService.modify(parameter1, parameter2);
        Assert.assertEquals(result.getCode(),actionCounter);
        actionCounter++;
        
        Collection<IParameter> parameters2 = new TreeSet<IParameter>();
        parameters2.add(new SimpleParameter("seven"));
        parameters2.add(new SimpleParameter("eight"));
        
        log("Calling service with parameters " + parameter2 + ", " + parameters);
        result = testService.modify(parameter2, parameters);
        Assert.assertEquals(result.getCode(),actionCounter + parameters.size() + 8);
        actionCounter++;
        
        log("Calling service with parameters " + parameter2 + ", " + parameters2);
        result = testService.modify(parameter2, parameters2);
        Assert.assertEquals(result.getCode(),actionCounter + parameters.size() + 8);
        actionCounter++;;
        
        Collection<IParameter> parameters3 = new ArrayList<IParameter>();
        parameters3.add(new SimpleParameter("nine"));
        parameters3.add(new SimpleParameter("ten"));
      
        log("Calling service with parameters " + parameter1 + ", " + parameters3);
        result = testService.modify(parameter1, parameters3);
        Assert.assertEquals(result.getCode(),actionCounter + parameters.size() + 8);
        actionCounter++;;
    }

}
