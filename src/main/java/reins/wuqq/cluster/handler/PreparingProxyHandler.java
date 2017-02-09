package reins.wuqq.cluster.handler;

import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;
import reins.wuqq.model.Instance;
import reins.wuqq.model.InstanceState;
import reins.wuqq.model.InstanceType;
import reins.wuqq.support.ErrorUtil;
import reins.wuqq.support.InstanceUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

@Component
public class PreparingProxyHandler extends AbstractStateHandler {
    @Value("${docker.proxy}")
    private String dockerImageForProxyServer;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_PROXY;
    }

    @Override
    public void enter() {
        super.enter();

        ensureProxyServerIsRunning();
    }

    private void ensureProxyServerIsRunning() {
        val proxyServerOpt = clusterDetail.getProxyServer();

        if (proxyServerOpt.isPresent()) {
            val proxyServer = proxyServerOpt.get();

            if (InstanceUtil.isRunning().test(proxyServer)) {
                transitOutOfState();
            } else {
                sync(proxyServer);
            }
        } else {
            launchProxyServer();
        }
    }

    private void launchProxyServer() {
        val proxyInstance = prepareProxyInstance();

        resourceProvider.launch(proxyInstance);
        clusterDetail.addInstance(proxyInstance);
    }

    private Instance prepareProxyInstance() {
        val configServerIP = getConfigServerIP();
        val proxyInstance = new Instance();

        proxyInstance.setName("ProxyServer");
        proxyInstance.setId(UUID.randomUUID().toString());
        proxyInstance.setType(InstanceType.PROXY_SERVER);
        proxyInstance.setImage(dockerImageForProxyServer);
        proxyInstance.getEnv().put("config", configServerIP);

        return proxyInstance;
    }

    private void transitOutOfState() {
        mongoCluster.transitTo(ClusterState.PREPARING_SHARD);
    }

    private String getConfigServerIP() {
        return clusterDetail.getConfigServer()
                .filter(InstanceUtil.withState(InstanceState.RUNNING))
                .orElseThrow(ErrorUtil.thrower("ConfigServer not launched"))
                .getHostIP();
    }

    @Override
    public void onNodeStarted(@Nonnull final TaskStatus status) {
        val taskID = status.getTaskId();

        clusterDetail.getProxyServer()
                .filter(InstanceUtil.withID(taskID))
                .ifPresent(instance -> {
                    instance.setState(InstanceState.RUNNING);
                    clusterDetail.updateInstance(instance);
                    transitOutOfState();
                });
    }
}
