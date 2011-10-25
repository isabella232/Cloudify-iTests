package com.gigaspaces.gateway;

import com.gatewayPUs.common.Stock;
import com.gigaspaces.cluster.replication.gateway.conflict.*;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author idan
 * @since 8.0.3
 *
 */
public class MyConflictResolver extends ConflictResolver {

	private Map<Integer, Boolean> actions = null;
	
	@Autowired
	protected GigaSpace gigaSpace;
	
	public MyConflictResolver() {
		actions = new HashMap<Integer, Boolean>();
		actions.put(1, true);
		actions.put(2, false);
		actions.put(3, true);
		actions.put(4, false);
		actions.put(5, true);
		actions.put(6, false);
		actions.put(7, true);
        actions.put(1000, true);
		actions.put(2000, false);
        actions.put(3000, false);
	}
	
	@Override
	public void onDataConflict(String sourceGatewayName, DataConflict conflict) {
		for (DataConflictOperation operation : conflict.getOperations()) {
			if (operation.getOperationEntry() instanceof Stock) {
				Stock stock = (Stock) operation.getOperationEntry();
				Boolean action = actions.get(stock.getStockId());
				if (action == null) {
					switch (stock.getStockId()) {
						case 100:
							stock.setStockName("CCCC");
							operation.override();
							break;
						case 300:
						case 400:
						case 500:
							operation.override();
							break;
					}
//					if (person.getId().equals(200L)) {
//						if (operation.getResolveAttempt() == 3) {
//							TestEntry testEntry = gigaSpace.readById(TestEntry.class, 0);
//							testEntry.setValue(new Object());
//							gigaSpace.write(testEntry);
//							operation.abort();
//						}
//					}
				} else if (action.booleanValue() == true) {
					operation.abort();
				} else {
					operation.override();
				}
			}
		}
	}

	@Override
	public void onRegisterTypeDescriptorConflict(String sourceGatewayName,
			RegisterTypeDescriptorConflict conflict) {
	}

	@Override
	public void onAddIndexConflict(String sourceGatewayName,
			AddIndexConflict conflict) {
	}
	
	
}
