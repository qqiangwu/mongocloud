package edu.reins.mongocloud.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.mongo.request.RemoveRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RemoveCommand {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    @Autowired
    private MongoCommandRunner commandRunner;

    /**
     * @throws MongoException if the command failed and if will be handled by the recover method
     * @throws RuntimeException      if programming error occurs
     */
    @Retryable(MongoException.class)
    public void exec(final RemoveRequest removeRequest) {
        val master = getMaster(removeRequest.getRouter());
        val cmd = buildCommand(removeRequest);

        commandRunner.runCommand(master, cmd);

        eventBus.post(new ClusterEvent(
                removeRequest.getCluster(), ClusterEventType.CHILD_REMOVED, removeRequest.getParticipant()));
    }

    @Nothrow
    @Recover
    public void recover(final MongoException e, final RemoveRequest removeRequest) {
        LOG.error("< remove(cluster: {}, child: {})",
                removeRequest.getCluster(), removeRequest.getParticipant(), e);

        eventBus.post(new ClusterEvent(removeRequest.getCluster(), ClusterEventType.FAIL, e.getMessage()));
    }

    /**
     * @throws IllegalArgumentException     if the clusterID or the participantID not exist
     */
    private BasicDBObject buildCommand(final RemoveRequest request) {
        return new BasicDBObject()
                .append("removeShard", request.getParticipant().getValue());
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
