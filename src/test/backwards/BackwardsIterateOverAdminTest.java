package test.backwards;

import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;

import java.lang.reflect.Method;
import java.util.Hashtable;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;

public class BackwardsIterateOverAdminTest extends AbstractTest {
	public int i = 0;
	private final String pack = "org.openspaces"; //<-the package scope of the iteration
	private Hashtable<String, Method> methodsExecuted = new Hashtable<String, Method>();
	private GridServiceAgent gsa;
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer gsc0;
	private ProcessingUnit pu;

	@BeforeMethod
	public void startSetUp() {
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = AdminUtils.loadGSM(machine);
		gsc0 = AdminUtils.loadGSC(machine);
		pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-space.war")));
		pu.waitFor(pu.getTotalNumberOfInstances());
		log("servlet space deployed");
	}
	//iterates over all non parameter-ed methods in package pack
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() throws Exception{
		
		Method [] methods = admin.getClass().getDeclaredMethods();
		Object [] empty = null;
		Object ans = null;

		//iterate over Admin methods, calls the recursive method
		for(Method method : methods){
			//not iterating over static, State (because of illegal monitor) and close
			if (method.getParameterTypes().length == 0 && !methodsExecuted.contains(method)
					&& !method.toString().contains("static") && !method.toString().contains("State")
					&& !method.toString().contains("close")){
				i++;
				methodsExecuted.put(method.toString(), method);
				log(i + ") " + "invoking " + method.toString());
				ans = method.invoke(admin, empty);
				log(i + ") " + method.toString() + " returned: " + ans);
			}
			if(ans != null){
				if(!ans.getClass().isArray() && isInApi(ans)){
					invokeAll(ans);
				}
				else if(ans.getClass().isArray()){
					Object [] array = (Object[]) ans;
					if(array.length != 0){
						invokeAll(array[0]);
					}
				}
			}
		}
		log("overall methods invoked: " + i + "\n");

	}


	//iterate over the pack-package methods recursively
	private void invokeAll(Object param) throws Exception{
		if(param != null && isInApi(param)){
			if (!param.getClass().isArray()){
				Method [] methods = param.getClass().getDeclaredMethods();
				Object ans = null;
				Object [] empty = null;
				//invoke parameter-less methods
				for(Method method : methods){
					if (execute(method)){
						methodsExecuted.put(method.toString(), method);
						i++;
						log(i + ") " + "invoking " + method.toString());
						ans = method.invoke(param, empty);
						log(i + ") " + method.toString() + " returned: " + ans);
						if(ans != null && isInApi(ans)){
							invokeAll(ans);
						}
					}
				}
			}
			else if(param.getClass().isArray()){
				Object [] array = (Object[]) param;
				if(array.length != 0){
					invokeAll(array[0]);
				}
			}
		}
	}

	private boolean isInApi(Object toCheck){
		return (toCheck.getClass().getName().startsWith(pack));
	}

	//not iterating over static, private, shutdown, DefaultElasticServiceManagers.waitForAtLeastOne, assertStateChangesPermitted 
	private boolean execute(Method toCheck){
		return (toCheck.getParameterTypes().length == 0 && !methodsExecuted.contains(toCheck)
				&& toCheck.toString().contains(pack) && !toCheck.toString().contains("static")
				&& !toCheck.toString().contains("private") && !toCheck.toString().contains("shutdown")
				&& !toCheck.toString().contains("DefaultAdmin.close()")
				&& !toCheck.toString().contains("DefaultElasticServiceManagers.waitForAtLeastOne") 
				&& !toCheck.toString().contains("assertStateChangesPermitted") && !toCheck.toString().contains("rescheduleStatisticsMonitor"));
	}

}


