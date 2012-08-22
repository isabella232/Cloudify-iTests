/**
 * 
 */
package framework.utils;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;

import test.data.Person;

/**
 * @author rafi
 *
 */
public class TestUtils {

	static AtomicLong idGenerator = new AtomicLong();
	
	/**
	 * waits for numOfMachines and 1 GSA, loads 1 gsm and numOfGscsPerMachine
	 */
	public static GridServiceManager waitAndLoad(Admin admin, int numOfMachines,int numOfGscsPerMachine) {
		admin.getMachines().waitFor(numOfMachines);
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		GridServiceManager gsm = loadGSM(gsa.getMachine());
		if (numOfGscsPerMachine > 0){
			for (GridServiceAgent gsa1 : admin.getGridServiceAgents()) {
				loadGSCs(gsa1,numOfGscsPerMachine);
			}
		}
		return gsm;
	}
	
	public static ProcessingUnit deploySpace(GridServiceManager gsm, String spaceName,int numberOfInstances, int numberOfBackups){
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment(spaceName).numberOfInstances(numberOfInstances).numberOfBackups(numberOfBackups));
		pu.waitForSpace();
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		return pu;
	}
			
	public static void writePersonBatch(GigaSpace gigaSpace, int numOfObjects) {
		List<Person> objList = new ArrayList<Person>(numOfObjects);
		for (int i = 0; i < numOfObjects; i++) {
			Person p=new Person();
			p.setId(idGenerator.getAndIncrement());
			objList.add(p);
		}
		gigaSpace.writeMultiple(objList.toArray());
	}
	
	public static void repetitive(Runnable repeatedAssert, int timeout)
	{
	    for(int delay = 0; delay < timeout; delay += 5)
	    {
		try
		{
		    repeatedAssert.run();
		    return;
		}
		catch(AssertionError e)
		{
		    try
		    {
			Thread.sleep(5);
		    }
		    catch (InterruptedException e1)
		    {
		    }
		}
	    }
	    repeatedAssert.run();
	}
	
	 public static String getClasspathString() {
	     StringBuffer classpath = new StringBuffer();
	     ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	     
	     URL[] urls = ((URLClassLoader)classLoader).getURLs();
	      for(int i=0; i < urls.length; i++) {
	          classpath.append(urls[i].getFile()).append("\r\n");
	      }    
	     
	      return classpath.toString();
	  }
	 
	 public static void writeTextFile(File file, String text) throws IOException {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			try {
					out.println(text);
			} finally {
				out.close();
			}
		}
}
