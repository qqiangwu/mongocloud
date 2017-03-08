package reins.wuqq.cluster.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Required;
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
@Slf4j(topic = "reins.PrepareConfig")
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
        log.info("PrepareConfig:launch");

        val instance = prepareConfigInstance();

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

        return instance;
    }

    private void transitOutOfState() {
        mongoCluster.transitTo(ClusterState.PREPARING_PROXY);
    }

    @Override
    public void onNodeStarted(@Nonnull final TaskStatus status) {
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
