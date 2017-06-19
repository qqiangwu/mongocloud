package edu.reins.mongocloud.monitor;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class MonitorImpl implements Monitor {
    private final List<ClusterID> clusters = new ArrayList<>();

    @Nothrow
    @Override
    public synchronized void register(final ClusterID clusterID) {
        clusters.add(clusterID);
    }

    @Nothrow
    @Override
    public synchronized void unregister(final ClusterID clusterID) {
        clusters.remove(clusterID);
    }

    @Nothrow
    @Override
    public synchronized Collection<ClusterID> getClusters() {
        return Collections.unmodifiableCollection(clusters);
    }
}
