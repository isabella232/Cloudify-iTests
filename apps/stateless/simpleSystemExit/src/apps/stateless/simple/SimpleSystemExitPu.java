package apps.stateless.simple;

import java.util.logging.Logger;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;


/**
 * This processing unit exists the GSC with the configured error code
 * Used for testing GSA restart-on-exit functionality
 * 
 * @author itaif
 * @since 8.0.5
 */
public class SimpleSystemExitPu implements InitializingBean{
	
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private String exitCode;
	
	@Required
	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Simple System Exit Pu -[   @PostConstruct ]- called. exitCode = " + exitCode);
		try {
			int exitErrorCode = Integer.valueOf(exitCode);
    		logger.info("System.exit(" + exitCode + ")");
    		System.exit(exitErrorCode);
    	}
    	catch (NumberFormatException e) {
    		//fall through
    	}
    	logger.info("exitCode " + exitCode + " is undefined");
    }
	
}
