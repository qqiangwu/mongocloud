package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContextImpl implements Context {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private Environment environment;

    private Map<InstanceID, Instance> instances = new ConcurrentHashMap<>();

    private Map<ClusterID, Cluster> clusters = new ConcurrentHashMap<>();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Map<InstanceID, Instance> getInstances() {
        return instances;
    }

    @Override
    public Map<ClusterID, Cluster> getClusters() {
        return clusters;
    }

    @Override
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    @Override
    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    @Override
    public Environment getEnv() {
        return environment;
    }
}
