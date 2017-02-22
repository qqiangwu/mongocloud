package reins.wuqq.cluster.handler;

import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;
import reins.wuqq.model.Instance;
import reins.wuqq.model.InstanceState;
import reins.wuqq.model.InstanceType;
import reins.wuqq.support.InstanceUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

@Component
public class PreparingShardHandler extends AbstractStateHandler {
    @Value("${docker.shard.image}")
    private String dockerImageForShardServer;

    @Value("${docker.shard.args}")
    private String dockerArgs;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_SHARD;
    }

    @Override
    public void enter() {
        super.enter();

        ensureShardCount();
    }

    private void ensureShardCount() {
        if (clusterDetail.needMoreShard()) {
            launchShardServer();
        } else {
            ensureShardIsRunning();
        }
    }

    private void launchShardServer() {
        val instance = new Instance();

        instance.setName("ShardServer");
        instance.setId(UUID.randomUUID().toString());
        instance.setType(InstanceType.SHARD);
        instance.setImage(dockerImageForShardServer);
        instance.setArgs(dockerArgs);

        resourceProvider.launch(instance);
        clusterDetail.addInstance(instance);
    }

    private void ensureShardIsRunning() {
        val shardNotRunning = clusterDetail.getShards()
                .stream()
                .filter(InstanceUtil.notRunning())
                .findFirst();

        if (shardNotRunning.isPresent()) {
            sync(shardNotRunning.get());
        } else {
            transitOutOfState();
        }
    }

    @Override
    public void onNodeStarted(@Nonnull final TaskStatus status) {
        val taskID = status.getTaskId();

        getInstance(taskID)
                .ifPresent(instance -> {
                    instance.setState(InstanceState.RUNNING);
                    clusterDetail.updateInstance(instance);
                    ensureShardCount();
                });
    }

    private void transitOutOfState() {
        mongoCluster.transitTo(ClusterState.RUNNING);
    }
}
