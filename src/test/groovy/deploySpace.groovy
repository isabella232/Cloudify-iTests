
import java.io.Serializable
import java.net.InetAddress
import java.util.concurrent.TimeUnit

import org.codehaus.groovy.runtime.StackTraceUtils
import org.openspaces.admin.Admin
import org.openspaces.admin.AdminFactory
import org.openspaces.admin.gsa.GridServiceAgent
import org.openspaces.admin.gsa.GridServiceContainerOptions
import org.openspaces.admin.gsa.GridServiceManagerOptions
import org.openspaces.admin.gsc.GridServiceContainer
import org.openspaces.admin.gsc.GridServiceContainers
import org.openspaces.admin.gsm.GridServiceManager
import org.openspaces.admin.gsm.GridServiceManagers
import org.openspaces.admin.space.Space
import org.openspaces.admin.space.SpaceDeployment
import org.openspaces.core.GigaSpace

import com.gigaspaces.annotation.pojo.SpaceClass
import com.gigaspaces.annotation.pojo.SpaceId
import com.gigaspaces.internal.utils.Assert



try {

	Admin admin = new AdminFactory().addGroups("sgtest-"+InetAddress.getLocalHost().getHostName()).useDaemonThreads(true).createAdmin();
	admin.getGridServiceAgents().waitFor(1, 30, TimeUnit.SECONDS);
	GridServiceAgent gsa = admin.getGridServiceAgents().getAgents()[0];
	gsa.startGridService(new GridServiceManagerOptions());
	gsa.startGridService(new GridServiceContainerOptions());
	gsa.startGridService(new GridServiceContainerOptions());

	admin.getGridServiceManagers().waitFor(1);

	GridServiceManager gsm = admin.getGridServiceManagers().getManagers()[0];
	gsm.deploy(new SpaceDeployment("qaSpace"));

	Space space = admin.getSpaces().waitFor("qaSpace");
	GigaSpace gigaSpace = space.getGigaSpace();
	println("write")
	gigaSpace.write(new Person(1));
	println("count")
	Assert.assertEquals(gigaSpace.count(new Person(1)), 1);
	println("--- PASSED")

	// kill all GSMs
	for (GridServiceAgent gsaTemp : admin.getGridServiceAgents()) {
		GridServiceManagers gridServiceManagers = gsaTemp.getMachine()
				.getGridServiceManagers();
		for (GridServiceManager gsmTemp : gridServiceManagers) {
			gsmTemp.kill();
		}
	}

	// kill all GSCs
	for (GridServiceAgent gsaTemp : admin.getGridServiceAgents()) {
		GridServiceContainers gridServiceContainers = gsaTemp.getMachine()
				.getGridServiceContainers();
		for (GridServiceContainer gsc : gridServiceContainers) {
			gsc.kill();
		}
	}
}



catch (Throwable t) {
	StackTraceUtils.sanitize(t);
	t.printStackTrace();
	System.exit(1);
	System.exit(0);
}

@SpaceClass
public class Person implements Serializable{
	private int id;
	private static final long serialVersionUID = -4740252554938917828L;

	public Person(){}

	public Person(int id){
		this.id= id;
	}

	@SpaceId
	public int getId(){
		return this.id;
	}


	public void setId(int id){
		this.id = id;
	}

}