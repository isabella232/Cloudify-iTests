package apps.stateless.simple;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * A simple stateless processing unit that does nothing. Used for testing stateless deployments.
 * 
 * @author Moran Avigdor
 * @since 8.0.1
 */
public class SimpleStatelessPu {

    @PostConstruct
    public void construct() {
    	System.out.println("Simple Stateless Pu -[   @PostConstruct ]- called");
    }

    @PreDestroy
    public void destroy() {
    	System.out.println("Simple Stateless Pu -[   @PreDestroy    ]- called");
    }
	
}
