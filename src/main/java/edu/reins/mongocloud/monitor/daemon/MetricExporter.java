package edu.reins.mongocloud.monitor.daemon;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterReport;
import edu.reins.mongocloud.cluster.ShardedCluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceReport;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.Units;
import edu.reins.mongocloud.support.annotation.Nothrow;
import io.prometheus.client.Gauge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MetricExporter {
    private static final String LABEL_INSTANCE = "instance";
    private static final String LABEL_CLUSTER = "cluster";

    private static final Gauge INSTANCE_CPU_USAGE = new Gauge.Builder()
            .name("instance_cpu_usage")
            .help("The cpu usage of a instance")
            .labelNames(LABEL_INSTANCE)
            .register();
    private static final Gauge INSTANCE_QPS = new Gauge.Builder()
            .name("instance_qps")
            .help("Instance reads per seconds")
            .labelNames(LABEL_INSTANCE)
            .register();
    private static final Gauge INSTANCE_TPS = new Gauge.Builder()
            .name("instance_tps")
            .help("Instance writes per seconds")
            .labelNames(LABEL_INSTANCE)
            .register();
    private static final Gauge CLUSTER_SHARDS = new Gauge.Builder()
            .name("cluster_shard_count")
            .help("Cluster shard counts")
            .labelNames(LABEL_CLUSTER)
            .register();
    private static final Gauge CLUSTER_STORAGE = new Gauge.Builder()
            .name("cluster_storage_usage")
            .help("Cluster storage usage")
            .labelNames(LABEL_CLUSTER)
            .register();

    @Autowired
    private Monitor monitor;

    @Autowired
    private Context context;

    @Nothrow
    @Scheduled(fixedDelay = 5 * Units.SECONDS)
    public void collect() {
        collectInstances();
        collectClusters();
    }

    @Nothrow
    private void collectInstances() {
        monitor.getInstances().stream()
                .map(context::getInstance)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this::collectInstance);
    }

    @Nothrow
    private void collectInstance(final Instance instance) {
        final String id = instance.getID().getValue();
        final InstanceReport report = instance.getReport();

        INSTANCE_CPU_USAGE.labels(id).set(report.getCpuPercent());
        INSTANCE_QPS.labels(id).set(report.getTotalReads());
        INSTANCE_TPS.labels(id).set(report.getTotalWrites());
    }

    @Nothrow
    private void collectClusters() {
        monitor.getClusters().stream()
                .map(context::getCluster)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(c -> c instanceof ShardedCluster)
                .forEach(this::collectCluster);
    }

    @Nothrow
    private void collectCluster(final Cluster cluster) {
        final String id = cluster.getID().getValue();
        final ClusterReport report = cluster.getReport();

        CLUSTER_SHARDS.labels(id).set(report.getShardCount());
        CLUSTER_STORAGE.labels(id).set(report.getStorageInMB());
    }
}
