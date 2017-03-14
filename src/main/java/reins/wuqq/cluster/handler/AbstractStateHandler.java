package reins.wuqq.cluster.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reins.wuqq.ResourceProvider;
import reins.wuqq.cluster.MongoCluster;
import reins.wuqq.cluster.PersistedClusterDetail;
import reins.wuqq.cluster.StateHandler;
import reins.wuqq.model.ClusterDetail;
import reins.wuqq.model.ClusterState;
import reins.wuqq.model.Instance;
import reins.wuqq.model.InstanceState;
import reins.wuqq.resource.FrameworkDetail;
import reins.wuqq.resource.PersistedFrameworkDetail;
import reins.wuqq.support.InstanceUtil;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j(topic = "reins.StateHandler")
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
    public ClusterDetail getDetail() {
        return clusterDetail.get();
    }

    @Override
    public void enter() {
        if (clusterDetail.getState() != getState()) {
            clusterDetail.setState(getState());
        }
    }

    @Override
    public void leave() {

    }

    // FIXME
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
            log.info("< onInstanceStarted(id: {}): remove legacy instance", status.getTaskId().getValue());
            resourceProvider.kill(status.getTaskId());
        }
    }

    @Override
    public void onInstanceLost(@Nonnull final TaskStatus status) {
        log.error("> cluster.onInstanceLost(id: {})", status.getTaskId().getValue());

        val taskID = status.getTaskId();
        val instanceOpt = getInstance(taskID);

        instanceOpt.ifPresent(instance -> {
            log.error("< cluster.onInstanceLost(instance: {})", instance.getId());

            clusterDetail.removeInstance(instance);
            mongoCluster.transitTo(ClusterState.PREPARING_CONFIG);
        });
    }

    @Override
    public void scaleOutTo(final long shardNumber) {
        throw new IllegalStateException("Scale out is not permitted now");
    }

    @Override
    public void scaleInTo(final long shardNumber) {
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
