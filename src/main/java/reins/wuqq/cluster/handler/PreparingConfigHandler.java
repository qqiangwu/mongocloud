package reins.wuqq.cluster.handler;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j(topic = "cluster.PrepareConfig")
public class PreparingConfigHandler extends AbstractStateHandler {
    @Value("${docker.config.image}")
    private String dockerImageForConfigServer;

    @Value("${docker.config.args}")
    private String dockerArgs;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_CONFIG;
    }

    @Override
    public void enter() {
        super.enter();

        log.info("PrepareConfig:enter");

        checkRetries();
        ensureConfigServerIsRunning();
    }

    private void ensureConfigServerIsRunning() {
        val configServerOpt = clusterDetail.getConfigServer();

        if (configServerOpt.isPresent()) {
            val configServer = configServerOpt.get();

            if (InstanceUtil.isRunning().test(configServer)) {
                transitOutOfState();
            } else {
                sync(configServer);
            }
        } else {
            launchConfigServer();
        }
    }

    private void launchConfigServer() {
        val instance = prepareConfigInstance();

        log.info("PrepareConfig:launch(instance: {})", instance);

        resourceProvider.launch(instance);
        clusterDetail.addInstance(instance);
    }

    private Instance prepareConfigInstance() {
        val instance = new Instance();

        instance.setName("ConfigServer");
        instance.setId(UUID.randomUUID().toString());
        instance.setType(InstanceType.CONFIG_SERVER);
        instance.setImage(dockerImageForConfigServer);
        instance.setArgs(dockerArgs);
        instance.setCpus(1.0);
        instance.setMemory(1024);
        instance.setDisk(10 * 1024);

        return instance;
    }

    private void transitOutOfState() {
        log.info("PrepareConfig:leave");

        retryCount = 0;
        mongoCluster.transitTo(ClusterState.PREPARING_PROXY);
    }

    @Override
    public void onInstanceStarted(@Nonnull final TaskStatus status) {
        super.onInstanceStarted(status);

        val taskID = status.getTaskId();

        clusterDetail.getConfigServer()
                .filter(InstanceUtil.withID(taskID))
                .ifPresent(instance -> {
                    instance.setState(InstanceState.RUNNING);

                    clusterDetail.updateInstance(instance);

                    transitOutOfState();
                });
    }
}
