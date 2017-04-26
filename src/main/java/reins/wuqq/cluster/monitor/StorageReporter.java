package reins.wuqq.cluster.monitor;

import io.prometheus.client.Gauge;
import lombok.Cleanup;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reins.wuqq.cluster.MongoCluster;
import reins.wuqq.cluster.MongoUtil;
import reins.wuqq.model.InstanceState;

@Component
public class StorageReporter {
    private static final Gauge storageUsage = Gauge.build()
            .name("mm_storage_usage")
            .help("Mongo Cluster Storage Usage In GB")
            .register();

    private static final Gauge shardCount = Gauge.build()
            .name("mm_shard_count")
            .help("Mongo Cluster Shard Count")
            .register();

    @Autowired
    MongoCluster mongoCluster;

    @Autowired
    MongoUtil mongoUtil;

    @Scheduled(fixedDelay = 5 * 1000)
    public void collectStorageUsage() {
        val router = mongoCluster.getDetail().getProxyServer();

        if (router == null) {
            return;
        }

        val storage = mongoUtil.getStorageInGB(router);

        storageUsage.set(storage);
    }

    @Scheduled(fixedDelay = 5 * 1000)
    public void collectShardCount() {
        val shards = mongoCluster.getDetail().getShards().values();
        val activeShards = shards.stream().filter(instance -> instance.getState().equals(InstanceState.RUNNING));

        shardCount.set(activeShards.count());
    }
}