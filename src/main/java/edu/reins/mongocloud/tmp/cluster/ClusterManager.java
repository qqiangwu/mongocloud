package edu.reins.mongocloud.tmp.cluster;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.tmp.EventBus;
import edu.reins.mongocloud.tmp.core.ResourceProvider;

import java.util.Map;

public class ClusterManager {
    private EventBus eventBus;
    private ResourceProvider resourceProvider;
    private Map<Long, Instance> instances;
    private Map<Long, Cluster> clusters;
}
