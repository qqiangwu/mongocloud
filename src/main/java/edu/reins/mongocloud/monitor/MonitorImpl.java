package edu.reins.mongocloud.monitor;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class MonitorImpl implements Monitor {
    private final Set<ClusterID> clusters = new ConcurrentSkipListSet<>();

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
    public Collection<ClusterID> getClusters() {
        return Collections.unmodifiableCollection(clusters);
    }
}
