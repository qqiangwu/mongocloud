package reins.wuqq.cluster;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reins.wuqq.Cluster;
import reins.wuqq.ResourceProvider;
import reins.wuqq.ResourceStatusListener;
import reins.wuqq.model.ClusterDetail;
import reins.wuqq.model.ClusterState;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.Map;

@Component("cluster")
@Slf4j(topic = "reins.MongoCluster")
public class MongoCluster implements Cluster, ResourceStatusListener {
    @Autowired
    private Map<ClusterState, StateHandler> handlerMap;

    @Autowired
    private PersistedClusterDetail clusterDetail;

    private StateHandler currentHandler;

    @Override
    public synchronized void onPlatformPrepared() {
        log.info("MongoCluster:prepared(state: {})", clusterDetail.get());

        currentHandler = handlerMap.get(clusterDetail.getState());
        currentHandler.enter();
    }

    public synchronized void transitTo(@Nonnull final ClusterState state) {
        val oldHandler = currentHandler;
        val newHandler = handlerMap.get(state);

        oldHandler.leave();
        newHandler.enter();

        currentHandler = newHandler;
    }

    @Override
    public synchronized ClusterDetail getDetail() {
        return currentHandler.getDetail();
    }

    @Override
    public synchronized void scaleOutTo(long shardNumber) {
        currentHandler.scaleOutTo(shardNumber);
    }

    @Override
    public synchronized void scaleInTo(long shardNumber) {
        currentHandler.scaleInTo(shardNumber);
    }

    @Override
    public synchronized void onNodeLaunched(@Nonnull Instance instance) {
        currentHandler.onNodeLaunched(instance);
    }

    @Override
    public synchronized void onNodeStarted(@Nonnull final TaskStatus status) {
        currentHandler.onNodeStarted(status);
    }

    @Override
    public synchronized void onNodeLost(@Nonnull final TaskStatus status) {
        currentHandler.onNodeLost(status);
    }
}
