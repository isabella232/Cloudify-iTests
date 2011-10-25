package test.alerts.resources;

import java.util.List;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;

import com.gigaspaces.async.AsyncResult;

public class BasicDistTask implements DistributedTask<Integer, Long> {
	
	private GigaSpace space;

	private static final long serialVersionUID = 1L;
	
	public BasicDistTask() {
		
	}
	
	public BasicDistTask(GigaSpace space) {
		this.space = space;
	}

	public Integer execute() throws Exception {
		int second = 0;

		for (int i = 0 ; i < Integer.MAX_VALUE ; i++) {
			for (int j = 0 ; j < Integer.MAX_VALUE ; j++) {
			}
		}
		return null;
		
	}

	public Long reduce(List<AsyncResult<Integer>> results) throws Exception {
		
		return null;
	}
}
