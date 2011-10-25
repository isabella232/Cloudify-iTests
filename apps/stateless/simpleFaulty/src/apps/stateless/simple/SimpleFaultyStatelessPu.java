package apps.stateless.simple;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * This processing unit fails if the GSC has a System property: test.fail-instantiation.enabled=true
 * A simple stateless processing unit that does nothing. Used for testing stateless deployments.
 * 
 * @author Moran Avigdor
 * @since 8.0.3
 */
public class SimpleFaultyStatelessPu {
	
	public final static String FAIL_INSTANTIATION_FLAG = "test.fail-instantiation.enabled";

    @PostConstruct
    public void construct() {
    	System.out.println("Simple Faulty Stateless Pu -[   @PostConstruct ]- called");
    	boolean shouldFailInstantiation = Boolean.getBoolean(FAIL_INSTANTIATION_FLAG);
    	if (shouldFailInstantiation) {
    		throw new RuntimeException("Simple Faulty PU '"+FAIL_INSTANTIATION_FLAG+"=true' - forced fail instantiation!");
    	} else {
    		System.out.println("Simple Faulty PU '"+FAIL_INSTANTIATION_FLAG+"=false'");
    	}
    }
    
    @PreDestroy
    public void destroy() {
    	System.out.println("Simple Faulty  Stateless Pu -[   @PreDestroy    ]- called");
    }
	
}
