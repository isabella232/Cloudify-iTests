import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.Admin;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.openspaces.admin.gsa.GridServiceAgent;
import java.util.concurrent.TimeUnit;
     		
try {
    
    Admin admin = new AdminFactory().addGroups("sgtest-webui").useDaemonThreads(true).createAdmin(); 
    admin.getGridServiceAgents().waitFor(2,30, TimeUnit.SECONDS);       
	GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();							
	if (agents != null) {
		for (GridServiceAgent gsa : agents) {
			gsa.shutdown();
		}		
	}
}
	
catch (Throwable t) {
	StackTraceUtils.sanitize(t);
	t.printStackTrace();
	System.exit(1);
	System.exit(0);
}
