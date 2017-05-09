package edu.reins.mongocloud.cluster.handler;

import edu.reins.mongocloud.model.ClusterState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "cluster.Running")
public class RunningHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.RUNNING;
    }

    @Override
    public void scaleOutTo(final int shardNumber) {
        val currentCount = clusterDetail.getShards().size();
        val numToAdd = shardNumber - currentCount;

        if (numToAdd > 0) {
            log.info("ScaleOut(old: {}, new: {})", currentCount, currentCount + numToAdd);

            clusterDetail.incrementShardNumber(numToAdd);

            mongoCluster.transitTo(ClusterState.PREPARING_SHARD);
        }
    }

    @Override
    public void scaleInTo(final int shardNumber) {
        if (shardNumber < 3) {
            return;
        }

        val currentCount = clusterDetail.getShards().size();
        val numToDecr = currentCount - shardNumber;

        if (numToDecr > 0 && numToDecr < currentCount) {
            log.info("ScaleIn(old: {}, new: {})", currentCount, currentCount - numToDecr);

            clusterDetail.decrementShardNumber(numToDecr);

            mongoCluster.transitTo(ClusterState.RECYCLE);
        }
    }
}
