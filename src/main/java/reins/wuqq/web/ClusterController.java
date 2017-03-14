package reins.wuqq.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reins.wuqq.Cluster;
import reins.wuqq.cluster.MongoCluster;
import reins.wuqq.model.ClusterDetail;

@RestController
@RequestMapping(path = "api")
public class ClusterController {
    @Autowired
    private MongoCluster cluster;

    @RequestMapping(path = "/resources", method = RequestMethod.GET)
    public Object getResourceDesc() {
        return null;
    }

    @RequestMapping(path = "/cluster", method = RequestMethod.GET)
    public ClusterDetail getClusterDetail() {
        return cluster.getDetail();
    }
}
