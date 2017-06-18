package edu.reins.mongocloud.web;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.Clusters;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
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
@RestController(value = "clusters/")
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

    @GetMapping(path = "")
    public Cluster getCluster(@RequestParam("id") String id) {
        final ClusterID clusterID = Clusters.of(id);
        final Cluster cluster = context.getClusters().get(clusterID);

        LOG.info("getCluster(id: {}, found: {})", clusterID, cluster != null);

        return cluster;
    }

    @GetMapping(path = "all")
    public Collection<ClusterVO> getAllClusters() {
        return context.getClusters().values()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
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
        vo.setType(vo.getType());

        if (instance.getState() == InstanceState.RUNNING) {
            vo.setHost(vo.getHost());
        }

        return vo;
    }
}
