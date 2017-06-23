package edu.reins.mongocloud.monitor;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MonitorImpl implements Monitor {
    private final List<ClusterID> clusters = new CopyOnWriteArrayList<>();

    private final List<InstanceID> instances = new CopyOnWriteArrayList<>();

    @Nothrow
    @Override
    public void register(final ClusterID clusterID) {
        clusters.add(clusterID);
    }

    @Nothrow
    @Override
    public void unregister(final ClusterID clusterID) {
        clusters.remove(clusterID);
    }

    @Nothrow
    @Override
    public void register(final InstanceID instanceID) {
        instances.add(instanceID);
    }

    @Nothrow
    @Override
    public void unregister(final InstanceID instanceID) {
        instances.remove(instanceID);
    }

    @Nothrow
    @Override
    public Collection<ClusterID> getClusters() {
        return Collections.unmodifiableCollection(clusters);
    }

    @Nothrow
    @Override
    public Collection<InstanceID> getInstances() {
        return Collections.unmodifiableCollection(instances);
    }
}
