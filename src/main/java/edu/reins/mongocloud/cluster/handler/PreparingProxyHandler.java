package edu.reins.mongocloud.cluster.handler;

import edu.reins.mongocloud.model.ClusterState;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.model.InstanceType;
import edu.reins.mongocloud.support.ErrorUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import edu.reins.mongocloud.support.InstanceUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

@Component
@Slf4j(topic = "cluster.PrepareProxy")
public class PreparingProxyHandler extends AbstractStateHandler {
    @Value("${docker.proxy.image}")
    private String dockerImageForProxyServer;

    @Value("${docker.proxy.args}")
    private String dockerArgs;

    @Override
    public ClusterState getState() {
        return ClusterState.PREPARING_PROXY;
    }

    @Override
    public void enter() {
        super.enter();

        log.info("PrepareProxy:enter");

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

        log.info("PrepareProxy:launch(instance: {})", proxyInstance);

        resourceProvider.launch(proxyInstance);
        clusterDetail.addInstance(proxyInstance);
    }

    private Instance prepareProxyInstance() {
        val configServerAddr = getConfigServerAddr();
        val proxyInstance = new Instance();

        proxyInstance.setName("ProxyServer");
        proxyInstance.setId(UUID.randomUUID().toString());
        proxyInstance.setType(InstanceType.PROXY_SERVER);
        proxyInstance.setImage(dockerImageForProxyServer);
        proxyInstance.setCpus(1.0);
        proxyInstance.setMemory(1024);
        proxyInstance.setDisk(10 * 1024);
        proxyInstance.setArgs(dockerArgs.replace("$CONFIG_SERVER", configServerAddr));

        return proxyInstance;
    }

    private void transitOutOfState() {
        log.info("PrepareProxy:leave");

        mongoCluster.transitTo(ClusterState.PREPARING_SHARD);
    }

    private String getConfigServerAddr() {
        val confServer = clusterDetail.getConfigServer()
                .filter(InstanceUtil.withState(InstanceState.RUNNING))
                .orElseThrow(ErrorUtil.thrower("ConfigServer not launched"));

        return String.format("%s:%s", confServer.getHostIP(), confServer.getPort());
    }

    @Override
    public void onInstanceStarted(@Nonnull final TaskStatus status) {
        super.onInstanceStarted(status);

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
