package edu.reins.mongocloud.web;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.*;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.clustermanager.exception.ClusterNotFoundException;
import edu.reins.mongocloud.clustermanager.exception.OperationNotAllowedException;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.Errors;
import edu.reins.mongocloud.web.vo.ClusterVO;
import edu.reins.mongocloud.web.vo.InstanceVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ClusterAPI {
    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private Context context;

    @GetMapping(path = "create")
    public void createCluster(@RequestParam("id") String id) throws ClusterIDConflictException {
        if (!clusterManager.isInitialized()) {
            throw new IllegalStateException("cluster is not initialized");
        }

        val definition = new ClusterDefinition(id, 1);

        clusterManager.createCluster(definition);
    }

    @GetMapping(path = "get")
    public ClusterVO getCluster(@RequestParam("id") String id) {
        final ClusterID clusterID = Clusters.of(id);
        final Cluster cluster = context.getClusters().get(clusterID);

        LOG.info("getCluster(id: {}, found: {})", clusterID, cluster != null);

        return toVO(cluster);
    }

    @GetMapping(path = "getAll")
    public Collection<ClusterVO> getAllClusters() {
        return context.getClusters().values()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @GetMapping(path = "scaleOut")
    public void scaleOut(@RequestParam("id") String id) throws ClusterNotFoundException, OperationNotAllowedException {
        final ClusterID clusterID = Clusters.of(id);

        clusterManager.scaleOut(clusterID);
    }

    @GetMapping(path = "scaleIn")
    public void scaleIn(@RequestParam("id") String id) throws ClusterNotFoundException, OperationNotAllowedException {
        final ClusterID clusterID = Clusters.of(id);

        clusterManager.scaleIn(clusterID);
    }

    // FIXME    remove this: for test purpose only
    @GetMapping(path = "rsAdd")
    public void rsAdd() {
        final Cluster cluster = context.getClusters().values().stream()
                .filter(c -> c instanceof ReplicaCluster)
                .findAny()
                .orElseThrow(Errors.throwException(IllegalStateException.class, "replica set not found"));

        context.getEventBus().post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_OUT));
    }

    // FIXME    remove this: for test purpose only
    @GetMapping(path = "rsRemove")
    public void rsRemove() {
        final Cluster cluster = context.getClusters().values().stream()
                .filter(c -> c instanceof ReplicaCluster)
                .findAny()
                .orElseThrow(Errors.throwException(IllegalStateException.class, "replica set not found"));

        context.getEventBus().post(new ClusterEvent(cluster.getID(), ClusterEventType.SCALE_IN));
    }

    @GetMapping(path = "routers")
    public String getRouter() {
        final Cluster cluster = context.getClusters().values().stream()
                .filter(c -> c instanceof RouterCluster)
                .findAny()
                .orElseThrow(Errors.throwException(IllegalStateException.class, "router not found"));

        final String url = cluster.getInstances().stream()
                .map(Instance::getHost)
                .map(h -> String.format("%s:%d", h.getIp(), h.getPort()))
                .collect(Collectors.joining(","));

        return String.format("mongodb://%s/wuqq", url);
    }

    private ClusterVO toVO(final Cluster cluster) {
        final ClusterVO vo = new ClusterVO();

        vo.setId(cluster.getID());
        vo.setState(cluster.getState());
        vo.setReport(cluster.getReport());
        vo.setInstances(cluster.getInstances().stream().map(this::toVO).collect(Collectors.toList()));

        return vo;
    }

    private InstanceVO toVO(final Instance instance) {
        final InstanceVO vo = new InstanceVO();

        vo.setId(instance.getID());
        vo.setState(instance.getState());
        vo.setType(instance.getType());
        vo.setReport(instance.getReport());

        if (instance.getState() == InstanceState.RUNNING) {
            vo.setHost(instance.getHost());
            vo.setContainerInfo(instance.getContainerInfo());
        }

        return vo;
    }
}
