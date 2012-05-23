package test.cli.cloudify.cloud;

import java.util.Set;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;

import com.google.common.base.Predicate;

public class Ec2Utils {

	private static ComputeServiceContext context = new ComputeServiceContextFactory().createContext("aws-ec2", "0VCFNJS3FXHYC7M6Y782", "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO");
	
	public static Set<? extends NodeMetadata> createServer(String serverName) throws RunNodesException{
		Set<? extends NodeMetadata> nodes = context.getComputeService().createNodesInGroup(serverName, 1);
		return nodes;
	}
	
	public static void shutdownServer(String serverId){
		context.getComputeService().destroyNode(serverId);
	}
	
	public static Set<? extends ComputeMetadata> getAllNodes(){
		return context.getComputeService().listNodes();
	}
	
	public static Set<? extends NodeMetadata> getServers(final Predicate<ComputeMetadata> filter) {
		return context.getComputeService().listNodesDetailsMatching(filter);
	}
	
	public static Set<? extends ComputeMetadata> getAllRunningNodes(){
		return getServers(new Predicate<ComputeMetadata>() {

			public boolean apply(final ComputeMetadata input) {
				final NodeMetadata node = (NodeMetadata) input;
				return (node.getState() == NodeState.RUNNING);
			}

		});
	}
	
	public static NodeMetadata getServerByName(final String serverName){
		Set<? extends NodeMetadata> matchingNodes = getServers(new Predicate<ComputeMetadata>() {
			
			public boolean apply(final ComputeMetadata input) {
				final NodeMetadata node = (NodeMetadata) input;
				return (node.getName().indexOf(serverName) > 0);
			}
			
		});
		
		if (!matchingNodes.isEmpty())
			return matchingNodes.iterator().next();
		return null;
	}

}
