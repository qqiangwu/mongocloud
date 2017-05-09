package edu.reins.mongocloud.cluster.daemon;

import edu.reins.mongocloud.cluster.MongoCluster;
import edu.reins.mongocloud.model.ClusterState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "daemon.Scaler")
public class AutoScaler {
    private static final double STORAGE_HIGH_UTILIZATION = 0.7;
    private static final double STORAGE_LOW_UTILIZATION = 0.2;

    @Autowired
    MongoCluster mongoCluster;

    @Autowired
    MetricReporter metricReporter;

    @Scheduled(fixedDelay = 30 * 1000)
    public void scale() {
        if (!mongoCluster.isInitialized() || !mongoCluster.getState().equals(ClusterState.RUNNING)) {
            return;
        }

        if (isResourceDeficient()) {
            scaleOut();
        } else if (isResourceSuperfluous()) {
            scaleIn();
        }
    }

    private boolean isResourceDeficient() {
        return metricReporter.getStorageUsage() > STORAGE_HIGH_UTILIZATION;
    }

    private boolean isResourceSuperfluous() {
        return metricReporter.getStorageUsage() < STORAGE_LOW_UTILIZATION &&
                mongoCluster.getShardCount() > 3;
    }

    private void scaleOut() {
        val oldCount = mongoCluster.getShardCount();
        val newCount = oldCount + 1;

        log.info("scaleOut(from: {}, to: {})", oldCount, newCount);

        mongoCluster.scaleOutTo(newCount);
    }

    private void scaleIn() {
        val oldCount = mongoCluster.getShardCount();
        val newCount = oldCount - 1;

        log.info("scaleIn(from: {}, to: {})", oldCount, newCount);

        mongoCluster.scaleInTo(newCount);
    }
}
