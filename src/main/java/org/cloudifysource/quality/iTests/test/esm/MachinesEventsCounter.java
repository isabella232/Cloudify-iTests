package org.cloudifysource.quality.iTests.test.esm;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminEventListener;
import org.openspaces.admin.machine.events.ElasticMachineProvisioningProgressChangedEvent;
import org.openspaces.admin.machine.events.ElasticMachineProvisioningProgressChangedEventListener;
import org.openspaces.admin.pu.elastic.events.ElasticProcessingUnitDecisionEvent;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;

public class MachinesEventsCounter implements AdminEventListener, ElasticMachineProvisioningProgressChangedEventListener {
	Logger logger = Logger.getLogger(this.getClass().getName());

	private final List<ElasticMachineProvisioningProgressChangedEvent> machineEvents;

	public MachinesEventsCounter(Admin admin) {

		machineEvents = new ArrayList<ElasticMachineProvisioningProgressChangedEvent>();
		admin.getMachines().getElasticMachineProvisioningProgressChanged().add(this, false);
	}

	private static int countEvents(Class<? extends ElasticMachineProvisioningProgressChangedEvent> eventClass,  List<ElasticMachineProvisioningProgressChangedEvent> machineEvents) {
		int count = 0;
		for (ElasticMachineProvisioningProgressChangedEvent  machineEvent : machineEvents) {
			if (eventClass.isAssignableFrom(machineEvent.getClass())) {
				count++;
			}
		}
		return count;
	}

	public void repetitiveAssertNumberOfMachineEvents(final Class<? extends ElasticMachineProvisioningProgressChangedEvent> eventClass, final int expected, long timeoutMilliseconds) {
		List<ElasticMachineProvisioningProgressChangedEvent> copy = getMachineEventsCopy();
		int actual = countEvents(eventClass, copy);
		if (actual > expected) {
			AssertUtils.assertFail("Expected " + expected +" " + eventClass + " events. actual " + actual + " : " + copy);
		}

		AssertUtils.repetitiveAssertTrue("Expected " + expected +" " + eventClass + " events", new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				List<ElasticMachineProvisioningProgressChangedEvent> copy = getMachineEventsCopy();
				int actual = countEvents(eventClass, copy);
				boolean condition = expected == actual;
				if (!condition) {
					LogUtils.log("Expected " + expected +" " + eventClass + " events. actual " + actual + " : " + copy);
				}
				return condition;
			}
		}, 
		timeoutMilliseconds);

		LogUtils.log("Found " + countEvents(eventClass, machineEvents) + " " + eventClass + " events");
	}

	private List<ElasticMachineProvisioningProgressChangedEvent> getMachineEventsCopy() {
		List<ElasticMachineProvisioningProgressChangedEvent> copy;
		synchronized(machineEvents) {
			copy = new ArrayList<ElasticMachineProvisioningProgressChangedEvent>(machineEvents);
		}
		return copy;
	}

	@Override
	public void elasticMachineProvisioningProgressChanged(
			ElasticMachineProvisioningProgressChangedEvent event) {

		if (event instanceof ElasticProcessingUnitDecisionEvent) {
			synchronized (machineEvents) {
				machineEvents.add(event);
			}
		}
	}
}
