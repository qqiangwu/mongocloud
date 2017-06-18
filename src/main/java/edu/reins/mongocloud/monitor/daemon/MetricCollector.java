package edu.reins.mongocloud.monitor.daemon;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.mongo.MongoMediator;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

@Daemon
@Slf4j
public class MetricCollector {
    @Autowired
    private Monitor monitor;

    @Autowired
    private MongoMediator mongoMediator;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    @Nothrow
    @Scheduled(fixedDelay = 30 * 1000)
    public void exec() {
        LOG.info("collectMetric");

        monitor.getClusters()
                .stream()
                .map(this::getCluster)
                .filter(Objects::nonNull)
                .forEach(this::collectClusterMetric);
    }

    @Nothrow
    private Cluster getCluster(final ClusterID id) {
        final Cluster cluster = context.getClusters().get(id);

        if (cluster == null) {
            LOG.warn("getClusterFailed(cluster: {})", id);
        }

        return cluster;
    }

    @Nothrow
    private void collectClusterMetric(final Cluster cluster) {
        LOG.info("< collectMetric(cluster: {})", cluster.getID());

        mongoMediator.collect(cluster);
    }
}
