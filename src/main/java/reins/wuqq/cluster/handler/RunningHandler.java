package reins.wuqq.cluster.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;

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
        val currentCount = clusterDetail.getShards().size();
        val numToDecr = currentCount - shardNumber;

        if (numToDecr > 0 && numToDecr < currentCount) {
            log.info("ScaleIn(old: {}, new: {})", currentCount, currentCount - numToDecr);

            clusterDetail.decrementShardNumber(numToDecr);

            mongoCluster.transitTo(ClusterState.RECYCLE);
        }
    }
}
