package edu.reins.mongocloud.monitor.daemon;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.ClusterReport;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

@Daemon
@Slf4j
public class ClusterAutoscaler {
    private static final double STORAGE_HIGH_UTILIZATION = 0.7;
    private static final double STORAGE_LOW_UTILIZATION = 0.3;

    @Autowired
    private Monitor monitor;

    @Autowired
    private Context context;

    @Autowired
    private EventBus eventBus;

    @Value("${instance.data.definition.disk}")
    private int capacity;

    @Nothrow
    @Scheduled(fixedDelay = 60 * 1000)
    public void exec() {
        monitor.getClusters().forEach(this::tryScaling);
    }

    @Nothrow
    private void tryScaling(final ClusterID id) {
        final Cluster cluster = context.getClusters().get(id);

        if (cluster == null) {
            LOG.warn("< getClusterFailed(cluster: {})", id);
        }

        tryScalingImpl(cluster);
    }

    @Nothrow
    private void tryScalingImpl(final Cluster cluster) {
        final ClusterReport report = cluster.getReport();

        if (isResourceDeficient(report)) {
            scaleOut(cluster);
        } else if (isResourceSuperfluous(report)) {
            scaleIn(cluster);
        }
    }

    @Nothrow
    private boolean isResourceDeficient(final ClusterReport report) {
        return getStorageUsage(report) > STORAGE_HIGH_UTILIZATION;
    }

    @Nothrow
    private boolean isResourceSuperfluous(final ClusterReport report) {
        return report.getShardCount() > 1 && getStorageUsage(report) < STORAGE_LOW_UTILIZATION;
    }

    @Nothrow
    private void scaleOut(final Cluster cluster) {
        LOG.info("scaleOut(cluster: {}, from: {}, to: {})",
                cluster.getID(), cluster.getInstances().size(), cluster.getInstances().size() + 1);

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_OUT));
    }

    @Nothrow
    private void scaleIn(final Cluster cluster) {
        LOG.info("scaleIn(cluster: {}, from: {}, to: {})",
                cluster.getID(), cluster.getInstances().size(), cluster.getInstances().size() - 1);

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_IN));
    }

    private double getStorageUsage(final ClusterReport report) {
        val storage = report.getStorageInMB();

        return storage / (report.getShardCount() * capacity * 1.0);
    }
}