package com.gatewayPUs.processorFilter;

import com.gatewayPUs.common.Stock;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.cluster.IReplicationFilter;
import com.j_spaces.core.cluster.IReplicationFilterEntry;
import com.j_spaces.core.cluster.ReplicationPolicy;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceLateContext;


public class SimpleReplicationFilter implements IReplicationFilter {

    @GigaSpaceLateContext(name = "gigaSpace")
    GigaSpace gigaSpace;


    public void init(IJSpace space, String paramUrl, ReplicationPolicy replicationPolicy) {
        System.out.println(" Space replication filter created on space [" + space.getURL() + "]");
    }

    public void process(int direction, IReplicationFilterEntry replicationFilterEntry, String remoteSpaceMemberName) {
        if (remoteSpaceMemberName.startsWith("gateway:LONDON")) {
            if (replicationFilterEntry.getClassName().equals(Stock.class.getName())) {
                replicationFilterEntry.discard();
            }
        }
    }

    public void close() {
        System.out.println("Space replication filter closed ");
    }
}