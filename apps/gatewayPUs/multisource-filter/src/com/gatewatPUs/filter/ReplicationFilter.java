package com.gatewatPUs.filter;

import java.text.DateFormat;
import java.util.Calendar;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.cluster.IReplicationFilter;
import com.j_spaces.core.cluster.IReplicationFilterEntry;
import com.j_spaces.core.cluster.ReplicationPolicy;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceLateContext;


public class ReplicationFilter implements IReplicationFilter {
	private int timeout;

	@GigaSpaceLateContext(name = "gigaSpace")
	GigaSpace gigaSpace;


	public void init(IJSpace space, String paramUrl, ReplicationPolicy replicationPolicy) {
		System.out.println(" Space replication filter created on space [" + space.getURL() + "]");
	}

	public void process(int direction, IReplicationFilterEntry replicationFilterEntry, String remoteSpaceMemberName) {
		synchronized (System.out) {
			System.out.println("**************************FILTER EVENT**************************");
			System.out.println("hash code: " + this.hashCode());
			System.out.println("remote space name: " + remoteSpaceMemberName);
			if (direction == IReplicationFilter.FILTER_DIRECTION_OUTPUT)
			{System.out.println("direction: out" + "\n");}
			else 
			{System.out.println("direction: in" + "\n");}


			if (direction == IReplicationFilter.FILTER_DIRECTION_OUTPUT) {
				if(remoteSpaceMemberName.equals("gateway:LONDON"/*"israelSpace_container2_1:israelSpace"*/) && ((Integer)replicationFilterEntry.getFieldValue("data") == 1)){
					try {
						long start = System.currentTimeMillis();
						printCurrentTime();
						System.out.println("waiting for " + timeout);
						System.out.println("stock partition: " + replicationFilterEntry.getFieldValue("data"));
						System.out.println("stock id: " + replicationFilterEntry.getFieldValue("stockId"));
						Thread.sleep(timeout);
						System.out.println("waited for " + ((System.currentTimeMillis() - start) / 1000.0) + " second(s)");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void close() {
		System.out.println("Space replication filter closed ");
	}

	/**
	 * @param wAIT_TIME the wAIT_TIME to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	private void printCurrentTime(){
		Calendar cal = Calendar.getInstance();
		DateFormat df = DateFormat.getTimeInstance(DateFormat.FULL);
		System.out.println(df.format(cal.getTime()));
	}
}