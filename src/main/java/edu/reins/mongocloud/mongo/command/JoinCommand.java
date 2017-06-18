package edu.reins.mongocloud.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.instance.Instances;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JoinCommand {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    @Autowired
    private MongoCommandRunner commandRunner;

    /**
     * @throws MongoCommandException if the command failed and if will be handled by the recover method
     * @throws RuntimeException      if programming error occurs
     */
    @Retryable(MongoCommandException.class)
    public void exec(final JoinRequest joinRequest) {
        val master = getMaster(joinRequest.getRouter().getCluster());
        val cmd = buildCommand(joinRequest);

        commandRunner.runCommand(master, cmd);

        eventBus.post(new ClusterEvent(joinRequest.getCluster(),
                ClusterEventType.CHILD_JOINED,
                joinRequest.getParticipant()));
    }

    @Nothrow
    @Recover
    public void recover(final MongoCommandException e, final JoinRequest joinRequest) {
        LOG.error("< join(cluster: {})", joinRequest.getCluster(), e);

        eventBus.post(new ClusterEvent(joinRequest.getCluster(), ClusterEventType.FAIL, e.getMessage()));
    }

    /**
     * @throws IllegalArgumentException     if the clusterID or the participantID not exist
     */
    private BasicDBObject buildCommand(final JoinRequest request) {
        val replica = getCluster(request.getParticipant());
        val master = replica.getMaster();
        val replicaName = replica.getID().getValue();
        val shardRef = String.format("%s/%s", replicaName, Instances.toAddress(master));

        return new BasicDBObject()
                .append("addShard", shardRef)
                .append("name", replicaName)
                .append("maxSize", master.getDefinition().getDisk());
    }

    /**
     * @throws IllegalArgumentException     if the clusterID don't exist
     */
    private InstanceHost getMaster(final ClusterID clusterID) {
        val cluster = getCluster(clusterID);

        return cluster.getMaster().getHost();
    }

    /**
     * @throws IllegalArgumentException     if the clusterID don't exist
     */
    private Cluster getCluster(final ClusterID clusterID) {
        val cluster = context.getClusters().get(clusterID);

        if (cluster == null) {
            throw new IllegalArgumentException(String.format("Cluster: %s not exist", clusterID));
        }

        return cluster;
    }
}
