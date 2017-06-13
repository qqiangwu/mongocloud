package edu.reins.mongocloud.cluster.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.Instances;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class MongoMediatorImpl implements MongoMediator {
    private static final String DB_ADMIN = "admin";
    private static final String DB_CMD_RS_INIT = "replSetInitiate";

    @Autowired
    private EventBus eventBus;

    @Override
    @Async
    @Nothrow
    public void initRs(final RsDefinition rsDefinition) {
        LOG.info("initRs(cluster: {}, members: {})", rsDefinition.getClusterID(), rsDefinition.getMembers().size());

        if (rsDefinition.getMembers().isEmpty()) {
            notifyClusterForFailure(rsDefinition.getClusterID(), "Empty cluster to init rs");
            return;
        }

        try {
            @Cleanup
            val mongo = connectMongo(rsDefinition);
            val db = mongo.getDatabase(DB_ADMIN);

            runInitRs(db, rsDefinition);

            notifyClusterForCompletion(rsDefinition.getClusterID());
        } catch (RuntimeException e) {
            LOG.error("initRs(cluster: {}): failed to init", rsDefinition.getClusterID(), e);

            notifyClusterForFailure(rsDefinition.getClusterID(), e.getMessage());
        }
    }

    /**
     * @throws RuntimeException for mongo operation failure
     */
    private void runInitRs(final MongoDatabase admin, final RsDefinition rsDefinition) {
        val cmd = buildCmdForInitRs(rsDefinition);

        admin.runCommand(cmd);
    }

    @Nothrow
    private Bson buildCmdForInitRs(final RsDefinition rsDefinition) {
        val members = rsDefinition.getMembers().stream()
                .map(instance -> new BasicDBObject()
                        .append("_id", instance.getID().getValue())
                        .append("host", Instances.toAddress(instance)))
                .collect(Collectors.toList());

        val config = new BasicDBObject()
                .append("_id", rsDefinition.getClusterID().getValue())
                .append("isConfig", rsDefinition.isConfig())
                .append("members", members);

        return new BasicDBObject(DB_CMD_RS_INIT, config);
    }

    @Nothrow
    private void notifyClusterForCompletion(final ClusterID clusterID) {
        eventBus.post(new ClusterEvent(clusterID, ClusterEventType.RS_INITED));
    }

    @Nothrow
    private void notifyClusterForFailure(final ClusterID clusterID, final String errorMsg) {
        eventBus.post(new ClusterEvent(clusterID, ClusterEventType.FAIL, errorMsg));
    }

    /**
     * @throws RuntimeException for connection failure
     */
    private MongoClient connectMongo(final RsDefinition rsDefinition) {
        val master = rsDefinition.getMembers().get(0).getHost();

        return new MongoClient(master.getIp(), master.getPort());
    }
}
