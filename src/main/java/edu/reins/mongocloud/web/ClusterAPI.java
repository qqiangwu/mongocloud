package edu.reins.mongocloud.web;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.Clusters;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
    public Collection<Cluster> getAllClusters() {
        return context.getClusters().values();
    }
}
