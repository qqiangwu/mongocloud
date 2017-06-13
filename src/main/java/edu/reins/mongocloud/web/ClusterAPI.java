package edu.reins.mongocloud.web;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.model.ClusterDefinition;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "clusters")
public class ClusterAPI {
    @Autowired
    private ClusterManager clusterManager;

    @GetMapping(path = "create")
    public void createCluster(@RequestParam("id") String id) throws ClusterIDConflictException {
        if (!clusterManager.isInitialized()) {
            throw new IllegalStateException("cluster is not initialized");
        }

        val definition = new ClusterDefinition(id, 1);

        clusterManager.createCluster(definition);
    }
}
