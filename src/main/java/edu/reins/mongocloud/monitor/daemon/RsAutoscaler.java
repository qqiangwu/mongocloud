package edu.reins.mongocloud.monitor.daemon;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.*;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceReport;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Daemon
@Slf4j
public class RsAutoscaler {
    private static final int CPU_UTILIZATION_HIGH = 70;
    private static final int CPU_UTILIZATION_LOW = 30;
    private static final int MINIMUM_RS_SIZE = 3;

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
        monitor.getClusters().stream()
                .map(id -> context.getClusters().get(id))
                .filter(c -> c instanceof ReplicaCluster)
                .forEach(this::tryScaling);
    }

    @Nothrow
    private void tryScaling(final Cluster cluster) {
        final ClusterReport report = cluster.getReport();

        if (isResourceDeficient(cluster)) {
            scaleOut(cluster);
        } else if (isResourceSuperfluous(cluster)) {
            scaleIn(cluster);
        }
    }

    @Nothrow
    private boolean isResourceDeficient(final Cluster cluster) {
        return cluster.getInstances().stream()
                .map(Instance::getReport)
                .mapToInt(InstanceReport::getCpuPercent)
                .allMatch(percent -> percent > CPU_UTILIZATION_HIGH);
    }

    @Nothrow
    private boolean isResourceSuperfluous(final Cluster cluster) {
        final List<Instance> instances = cluster.getInstances();

        if (instances.size() > MINIMUM_RS_SIZE) {
            return instances.stream()
                    .map(Instance::getReport)
                    .mapToInt(InstanceReport::getCpuPercent)
                    .allMatch(percent -> percent < CPU_UTILIZATION_LOW);
        }

        return false;
    }

    @Nothrow
    private void scaleOut(final Cluster cluster) {
        LOG.info("scaleOut(rs: {}, from: {}, to: {})",
                cluster.getID(), cluster.getInstances().size(), cluster.getInstances().size() + 1);

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_OUT));
    }

    @Nothrow
    private void scaleIn(final Cluster cluster) {
        LOG.info("scaleIn(rs: {}, from: {}, to: {})",
                cluster.getID(), cluster.getInstances().size(), cluster.getInstances().size() - 1);

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_IN));
    }
}
