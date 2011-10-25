package test.admin.space;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.SpaceInstanceRuntimeDetails;
import org.openspaces.admin.space.SpaceRuntimeDetails;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Data;
import test.data.Person;
import test.utils.ProcessingUnitUtils;

public class SpaceRuntimeDetailsTest extends AbstractTest {

	private GridServiceManager gsm;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();
		gsm = loadGSM(machine);
		loadGSC(machine);
		loadGSC(machine);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
	public void test() throws Exception {
		singleSpaceDeployment();
		clusterSpaceDeployment();
	}

	private void singleSpaceDeployment() {
		ProcessingUnit singleSpacePu = gsm.deploy(new SpaceDeployment("singleSpace"));
		singleSpacePu.waitFor(singleSpacePu.getTotalNumberOfInstances());
		checkSpaceRuntimeDetails(singleSpacePu);
		checkSpaceInstanceRuntimeDetails(singleSpacePu, false);
	}
	
	private void clusterSpaceDeployment() {
		ProcessingUnit clusterSpacePu = gsm.deploy(new SpaceDeployment("clusterSpace").partitioned(2, 1).maxInstancesPerVM(0));
		clusterSpacePu.waitFor(clusterSpacePu.getTotalNumberOfInstances());
		checkSpaceRuntimeDetails(clusterSpacePu);
		checkSpaceInstanceRuntimeDetails(clusterSpacePu, true);
	}
	
	private void checkSpaceRuntimeDetails(ProcessingUnit pu) {
		SpaceRuntimeDetails runtimeDetails = pu.waitForSpace().getRuntimeDetails();
		ProcessingUnitUtils.waitForActiveElection(pu);
		
		assertEquals(0, runtimeDetails.getCount());
		assertEquals(1, runtimeDetails.getClassNames().length);
		assertEquals(Object.class.getName(), runtimeDetails.getClassNames()[0]);
		assertEquals(1, runtimeDetails.getCountPerClassName().size());
		assertEquals(0, runtimeDetails.getCountPerClassName().get(Object.class.getName()).intValue());
		
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		gigaSpace.writeMultiple(createPersonArray(10));
		
		assertEquals(10, runtimeDetails.getCount());
		assertEquals(2, runtimeDetails.getClassNames().length);
		assertEquals(Object.class.getName(), runtimeDetails.getClassNames()[0]);
		assertEquals(Person.class.getName(), runtimeDetails.getClassNames()[1]);
		assertEquals(2, runtimeDetails.getCountPerClassName().size());
		assertEquals(0, runtimeDetails.getCountPerClassName().get(Object.class.getName()).intValue());
		assertEquals(10, runtimeDetails.getCountPerClassName().get(Person.class.getName()).intValue());
		
		gigaSpace.writeMultiple(createDataArray(20));
		assertEquals(30, runtimeDetails.getCount());
		assertEquals(3, runtimeDetails.getClassNames().length);
		assertEquals(Object.class.getName(), runtimeDetails.getClassNames()[0]);
		assertEquals(Data.class.getName(), runtimeDetails.getClassNames()[1]);
		assertEquals(Person.class.getName(), runtimeDetails.getClassNames()[2]);
		assertEquals(3, runtimeDetails.getCountPerClassName().size());
		assertEquals(0, runtimeDetails.getCountPerClassName().get(Object.class.getName()).intValue());
		assertEquals(10, runtimeDetails.getCountPerClassName().get(Person.class.getName()).intValue());
		assertEquals(20, runtimeDetails.getCountPerClassName().get(Data.class.getName()).intValue());
	}
	
	private void checkSpaceInstanceRuntimeDetails(ProcessingUnit pu, boolean isClustered) {
		
		for (SpaceInstance spaceInstance : pu.getSpace().getInstances()) {
			SpaceInstanceRuntimeDetails runtimeDetails = spaceInstance.getRuntimeDetails();
			if (isClustered) {
				assertEquals(15, runtimeDetails.getCount());
				assertEquals(3, runtimeDetails.getClassNames().length);
				assertEquals(Object.class.getName(), runtimeDetails.getClassNames()[0]);
				assertEquals(Data.class.getName(), runtimeDetails.getClassNames()[1]);
				assertEquals(Person.class.getName(), runtimeDetails.getClassNames()[2]);
				
				assertEquals(3, runtimeDetails.getCountPerClassName().size());
				assertEquals(0, runtimeDetails.getCountPerClassName().get(Object.class.getName()).intValue());
				assertEquals(5, runtimeDetails.getCountPerClassName().get(Person.class.getName()).intValue());
				assertEquals(10, runtimeDetails.getCountPerClassName().get(Data.class.getName()).intValue());
			} else {
				assertEquals(30, runtimeDetails.getCount());
				assertEquals(3, runtimeDetails.getClassNames().length);
				assertEquals(Object.class.getName(), runtimeDetails.getClassNames()[0]);
				assertEquals(Data.class.getName(), runtimeDetails.getClassNames()[1]);
				assertEquals(Person.class.getName(), runtimeDetails.getClassNames()[2]);
				assertEquals(3, runtimeDetails.getCountPerClassName().size());
				assertEquals(0, runtimeDetails.getCountPerClassName().get(Object.class.getName()).intValue());
				assertEquals(10, runtimeDetails.getCountPerClassName().get(Person.class.getName()).intValue());
				assertEquals(20, runtimeDetails.getCountPerClassName().get(Data.class.getName()).intValue());
			}
		}
		
	}
	
	private Person[] createPersonArray(int size) {
		Person[] array = new Person[size];
		for (int i=0; i<array.length; ++i) {
			array[i] = new Person(Long.valueOf(i));
		}
		return array;
	}
	
	private Data[] createDataArray(int size) {
		Data[] array = new Data[size];
		for (int i=0; i<array.length; ++i) {
			Data d = new Data(i);
			d.setId("id"+i);
			d.setData("data"+i);
			array[i] = d;
		}
		return array;
	}
}
