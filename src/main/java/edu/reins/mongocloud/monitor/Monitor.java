package edu.reins.mongocloud.monitor;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.support.annotation.Nothrow;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

@ThreadSafe
public interface Monitor {
    @Nothrow
    void register(ClusterID clusterID);

    @Nothrow
    void unregister(ClusterID clusterID);

    @Nothrow
    void register(InstanceID instanceID);

    @Nothrow
    void unregister(InstanceID instanceID);

    /**
     * @return A unmodifiable view of all registered clusters
     */
    @Nothrow
    Collection<ClusterID> getClusters();

    /**
     * @return A unmodifiable view of all registered clusters
     */
    @Nothrow
    Collection<InstanceID> getInstances();
}
