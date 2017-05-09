package edu.reins.mongocloud.cluster.handler;

import edu.reins.mongocloud.cluster.MongoUtil;
import edu.reins.mongocloud.model.ClusterState;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.model.InstanceType;
import edu.reins.mongocloud.support.InstanceUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.UUID;

@Component
@Slf4j(topic = "cluster.PrepareShard")
public class PreparingShardHandler extends AbstractStateHandler {
    @Value("${docker.shard.image}")
    private String dockerImageForShardServer;

    @Value("${docker.shard.args}")
    private String dockerArgs;

    @Autowired
    MongoUtil mongoUtil;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_SHARD;
    }

    @Override
    public void enter() {
        super.enter();

        log.info("PrepareShard:enter");

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
        instance.setCpus(1.0);
        instance.setMemory(1024);
        instance.setDisk(10 * 1024);

        log.info("PrepareShard:launch(instance: {})", instance);

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
    public void onInstanceStarted(@Nonnull final TaskStatus status) {
        super.onInstanceStarted(status);

        clusterDetail.getShardServer(status.getTaskId().getValue())
                .ifPresent(instance -> {
                    instance.setState(InstanceState.RUNNING);
                    addShardToCluster(instance);
                    clusterDetail.updateInstance(instance);
                    ensureShardCount();
                });
    }

    private void transitOutOfState() {
        mongoCluster.transitTo(ClusterState.RUNNING);
    }

    private void addShardToCluster(final Instance shard) {
        clusterDetail.getProxyServer().ifPresent(router -> {
            log.info("attend(shard: {}, router: {})",
                    InstanceUtil.toReadable(shard),
                    InstanceUtil.toReadable(router));

            mongoUtil.addShardToCluster(router, shard);
        });
    }
}
