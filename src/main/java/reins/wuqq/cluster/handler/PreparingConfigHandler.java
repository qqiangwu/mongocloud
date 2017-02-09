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
public class PreparingConfigHandler extends AbstractStateHandler {
    @Value("${docker.config}")
    private String dockerImageForConfigServer;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_CONFIG;
    }

    @Override
    public void enter() {
        super.enter();

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

        resourceProvider.launch(instance);
        clusterDetail.addInstance(instance);
    }

    private Instance prepareConfigInstance() {
        val instance = new Instance();

        instance.setName("ConfigServer");
        instance.setId(UUID.randomUUID().toString());
        instance.setType(InstanceType.CONFIG_SERVER);
        instance.setImage(dockerImageForConfigServer);

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
