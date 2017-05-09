package edu.reins.mongocloud.cluster.handler;

import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.model.ClusterState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import edu.reins.mongocloud.cluster.MongoCluster;
import edu.reins.mongocloud.cluster.PersistedClusterDetail;
import edu.reins.mongocloud.cluster.StateHandler;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.resource.PersistedFrameworkDetail;
import edu.reins.mongocloud.support.InstanceUtil;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j(topic = "cluster.StateHandler")
public abstract class AbstractStateHandler implements StateHandler {
    @Autowired
    protected PersistedFrameworkDetail frameworkConfiguration;

    @Autowired
    protected PersistedClusterDetail clusterDetail;

    @Autowired
    protected ResourceProvider resourceProvider;

    @Autowired
    protected MongoCluster mongoCluster;

    @Value("${scheduler.conf.retry}")
    protected int retryCount;

    @Override
    public void enter() {
        if (clusterDetail.getState() != getState()) {
            clusterDetail.setState(getState());
        }
    }

    @Override
    public void leave() {

    }

    protected void checkRetries() {
        if (++retryCount > 5) {
            log.error("RetryFailed(state: {})", getState());
            throw new RuntimeException("Retried Too Much");
        }
    }

    @Override
    public final void onPlatformPrepared() {
        throw new IllegalStateException("The underlying platform should be initialized before");
    }

    @Override
    public final void onClusterDestroyed() {

    }

    @Override
    public void onInstanceLaunched(@Nonnull final Instance instance) {
        log.info("StateHandler:onInstanceLaunched(instance: {})", instance.getId());

        instance.setState(InstanceState.LAUNCHING);
        clusterDetail.updateInstance(instance);
    }

    @Override
    public void onInstanceStarted(@Nonnull final TaskStatus status) {
        log.info("> onInstanceStarted(id: {})", status.getTaskId().getValue());

        val taskID = status.getTaskId();
        val instanceOpt = getInstance(taskID);

        if (!instanceOpt.isPresent()) {
            log.info("< onInstanceStarted(id: {}): recycle legacy instance", status.getTaskId().getValue());

            val instance = instanceOpt.get();

            instance.setState(InstanceState.KILLING);
            clusterDetail.updateInstance(instance);

            mongoCluster.transitTo(ClusterState.RECYCLE);
        }
    }

    @Override
    public void onInstanceLost(@Nonnull final TaskStatus status) {
        val taskID = status.getTaskId();
        val instanceOpt = getInstance(taskID);

        instanceOpt.ifPresent(instance -> {
            log.info("> cluster.onInstanceLost(instance: {}, state: {})", InstanceUtil.toReadable(instance), instance.getState());

            clusterDetail.removeInstance(instance);

            if (!instance.getState().equals(InstanceState.DIED)) {
                log.error("< cluster.unexpectedLost(instance: {}, state: {})", InstanceUtil.toReadable(instance), instance.getState());
                mongoCluster.transitTo(ClusterState.PREPARING_CONFIG);
            }
        });
    }

    @Override
    public void scaleOutTo(final int shardNumber) {
        throw new IllegalStateException("Scale out is not permitted now");
    }

    @Override
    public void scaleInTo(final int shardNumber) {
        throw new UnsupportedOperationException("Scale in is not permitted not");
    }

    protected Optional<Instance> getInstance(@Nonnull final TaskID taskID) {
        val withID = InstanceUtil.withID(taskID);

        if (clusterDetail.getConfigServer().filter(withID).isPresent()) {
            return clusterDetail.getConfigServer();
        } else if (clusterDetail.getProxyServer().filter(withID).isPresent()) {
            return clusterDetail.getProxyServer();
        } else {
            return clusterDetail.getShardServer(taskID.getValue());
        }
    }

    protected void sync(@Nonnull Instance instance) {
        resourceProvider.sync(InstanceUtil.toTaskID(instance));
    }
}
