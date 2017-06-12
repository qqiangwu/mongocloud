package edu.reins.mongocloud;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import org.springframework.core.env.Environment;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

@ThreadSafe
public interface Context {
    EventBus getEventBus();
    Map<InstanceID, Instance> getInstances();
    Map<ClusterID, Cluster> getClusters();
    ClusterManager getClusterManager();
    ResourceProvider getResourceProvider();
    Environment getEnv();
}
