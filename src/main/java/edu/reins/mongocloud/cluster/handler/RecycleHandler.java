package edu.reins.mongocloud.cluster.handler;

import edu.reins.mongocloud.model.ClusterState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.support.InstanceUtil;

@Component
@Slf4j(topic = "cluster.Recycle")
public class RecycleHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.RECYCLE;
    }

    @Override
    public void enter() {
        super.enter();

        log.info("enter");

        ensureShardCount();
    }

    private void ensureShardCount() {
        val currentShardCount = clusterDetail.getShards().size();
        val targetShard = clusterDetail.getShardsNeeded();

        if (currentShardCount > targetShard) {
            val diff = currentShardCount - targetShard;

            log.info("> recycle(current: {}, target: {})", currentShardCount, targetShard);

            clusterDetail.getShards()
                    .stream()
                    .limit(diff)
                    .forEach(shard -> {
                        log.info("< recycle(id: {})", InstanceUtil.toReadable(shard));

                        shard.setState(InstanceState.KILLING);
                        clusterDetail.updateInstance(shard);
                    });
        }
    }
}
