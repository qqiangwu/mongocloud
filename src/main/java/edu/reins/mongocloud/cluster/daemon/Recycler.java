package edu.reins.mongocloud.cluster.daemon;

import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.model.ClusterState;
import edu.reins.mongocloud.model.Instance;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import edu.reins.mongocloud.cluster.MongoCluster;
import edu.reins.mongocloud.cluster.MongoUtil;
import edu.reins.mongocloud.cluster.PersistedClusterDetail;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.support.InstanceUtil;

@Component
@Slf4j(topic = "daemon.Recycler")
public class Recycler {
    @Autowired
    PersistedClusterDetail clusterDetail;

    @Autowired
    MongoCluster mongoCluster;

    @Autowired
    ResourceProvider resourceProvider;

    @Autowired
    MongoUtil mongoUtil;

    @Scheduled(fixedDelay = 10 * 1000)
    public void recycle() {
        log.info("recycle");

        if (!mongoCluster.isInitialized()) {
            return;
        }

        recycleKilling();
        recycleDied();
        tryRecover();
    }

    private void recycleKilling() {
        clusterDetail.getShards().stream()
                .filter(InstanceUtil.withState(InstanceState.KILLING))
                .findAny()
                .ifPresent(instance -> {
                    log.info("> waitForRemoving(id: {})", InstanceUtil.toReadable(instance));

                    if (tryRemove(instance)) {
                        log.info("< removeDone(id: {})", InstanceUtil.toReadable(instance));

                        instance.setState(InstanceState.DIED);
                        clusterDetail.updateInstance(instance);
                    }
                });
    }

    private void recycleDied() {
        clusterDetail.getShards().stream()
                .filter(InstanceUtil.withState(InstanceState.DIED))
                .forEach(this::kill);
    }

    private void tryRecover() {
        if (!mongoCluster.getState().equals(ClusterState.RECYCLE)) {
            return;
        }

        val needRecycle = clusterDetail.getShards()
                .stream()
                .anyMatch(InstanceUtil.withState(InstanceState.KILLING));

        if (needRecycle) {
            return;
        }

        mongoCluster.transitTo(ClusterState.RUNNING);
    }

    private boolean tryRemove(final Instance shard) {
        return mongoUtil.removeShardFromCluster(shard);
    }

    private void kill(final Instance shard) {
        log.info("kill(id: {})", InstanceUtil.toReadable(shard));

        resourceProvider.kill(InstanceUtil.toTaskID(shard));
    }
}
