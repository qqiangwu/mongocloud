package edu.reins.mongocloud.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.ClusterReport;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.support.Units;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CollectClusterCommand {
    private static final BasicDBObject DB_CMD_STATS = new BasicDBObject()
            .append("dbStats", 1)
            .append("scale", Units.MB);

    @Autowired
    private EventBus eventBus;

    @Autowired
    private MongoCommandRunner commandRunner;

    /**
     * @throws MongoException if the command failed and if will be handled by the recover method
     * @throws RuntimeException      if programming error occurs
     */
    @Retryable(MongoException.class)
    public void exec(final Cluster cluster) {
        val result = commandRunner.runCommand(getMaster(cluster), DB_CMD_STATS);

        val report = ClusterReport.builder()
                .storageInMB(result.getInteger("dataSize").intValue())
                .build();

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.UPDATE_STATUS, report));
    }

    @Nothrow
    @Recover
    public void recover(final MongoException e, final Cluster cluster) {
        LOG.error("< collect(cluster: {})", cluster.getID(), e);

        eventBus.post(new ClusterEvent(cluster.getID(), ClusterEventType.FAIL, e.getMessage()));
    }

    @Nothrow
    private InstanceHost getMaster(final Cluster cluster) {
        return cluster.getInstances().get(0).getHost();
    }
}
