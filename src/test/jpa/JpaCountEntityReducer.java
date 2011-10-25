package test.jpa;

import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

/**
 * JPA count entity reducer.
 * Sums results of a JPA count remote call.
 * 
 * @author idan
 * @since 8.0.1
 *
 */
public class JpaCountEntityReducer implements RemoteResultReducer<Integer, Integer> {

	public Integer reduce(SpaceRemotingResult<Integer>[] results,
			SpaceRemotingInvocation invocation) throws Exception {
		Integer sum = 0;
		for (SpaceRemotingResult<Integer> result : results) {
			if (result.getException() != null)
				throw (Exception) result.getException();
			sum += result.getResult();
		}
		return sum;
	}

}
