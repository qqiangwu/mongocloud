package edu.reins.mongocloud;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.monitor.Monitor;
import org.springframework.core.env.Environment;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;

@ThreadSafe
public interface Context {
    EventBus getEventBus();

    Map<InstanceID, Instance> getInstances();
    Map<ClusterID, Cluster> getClusters();

    Optional<Instance> getInstance(InstanceID instanceID);
    Optional<Cluster> getCluster(ClusterID clusterID);

    ClusterManager getClusterManager();
    ResourceProvider getResourceProvider();

    Environment getEnv();
    Monitor getMonitor();

    MongoMediator getMongoMediator();
}
