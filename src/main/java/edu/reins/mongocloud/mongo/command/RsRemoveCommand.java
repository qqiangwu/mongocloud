package edu.reins.mongocloud.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.mongo.request.RsRemoveRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RsRemoveCommand {
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
    public void exec(final RsRemoveRequest request) {
        final InstanceHost master = getMaster(request.getCluster());
        final Document conf = commandRunner.getConfig(master);
        final BasicDBObject cmd = buildCommand(request, conf);

        commandRunner.runCommand(master, cmd);

        eventBus.post(
                new ClusterEvent(request.getCluster(), ClusterEventType.CHILD_REMOVED, request.getInstance()));
    }

    @Nothrow
    @Recover
    public void recover(final MongoException e, final RsRemoveRequest request) {
        LOG.error("< rsRemove(cluster: {}, child: {})", request.getCluster(), request.getInstance(), e);

        eventBus.post(new ClusterEvent(request.getCluster(), ClusterEventType.FAIL, e.getMessage()));
    }

    /**
     * @throws IllegalArgumentException     if the clusterID or the instanceID not exist
     */
    private BasicDBObject buildCommand(final RsRemoveRequest request, final Document conf) {
        final Instance victim = getInstance(request.getInstance());

        changeMembers(conf, victim);
        changeVersion(conf);

        return new BasicDBObject("replSetReconfig", conf);
    }

    @Nothrow
    @SuppressWarnings("unchecked")
    private void changeMembers(final Document conf, final Instance victim) {
        final List<Document> members = conf.get("members", List.class);

        members.removeIf(d -> d.get("_id").equals(victim.getLocalID()));

        conf.put("members", members);
    }

    @Nothrow
    private void changeVersion(final Document conf) {
        final int version = conf.getInteger("version");

        conf.put("version", version + 1);
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

    /**
     * @throws IllegalArgumentException     if the instanceID don't exist
     */
    private Instance getInstance(final InstanceID instanceID) {
        final Instance instance = context.getInstances().get(instanceID);

        if (instance == null) {
            throw new IllegalArgumentException(String.format("rsJoin: bad instanceID[%s]", instanceID));
        }

        return instance;
    }
}
