package edu.reins.mongocloud.cluster.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.mongo.MongoCommandRunner;
import edu.reins.mongocloud.cluster.mongo.RsDefinition;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.instance.Instances;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Collectors;

@Slf4j
@Component
public class InitRsCommand {
    private static final String DB_CMD_RS_INIT = "replSetInitiate";

    @Autowired
    private EventBus eventBus;

    @Autowired
    private MongoCommandRunner commandRunner;

    /**
     * @throws MongoCommandException if the command failed and if will be handled by the recover method
     * @throws RuntimeException      if programming error occurs
     */
    @Retryable(MongoCommandException.class)
    public void exec(final RsDefinition rsDefinition) {
        Assert.notEmpty(rsDefinition.getMembers());

        val cmd = buildCmdForInitRs(rsDefinition);
        val master = getMaster(rsDefinition);

        commandRunner.runCommand(master, cmd);

        eventBus.post(new ClusterEvent(rsDefinition.getClusterID(), ClusterEventType.RS_INITED));
    }

    @Nothrow
    @Recover
    public void recover(final MongoCommandException e, final RsDefinition rsDefinition) {
        LOG.error("< initRs(cluster: {}): failed to init", rsDefinition.getClusterID(), e);

        eventBus.post(new ClusterEvent(rsDefinition.getClusterID(), ClusterEventType.FAIL, e.getMessage()));
    }

    @Nothrow
    private InstanceHost getMaster(final RsDefinition rs) {
        return rs.getMembers().get(0).getHost();
    }

    @Nothrow
    private BasicDBObject buildCmdForInitRs(final RsDefinition rsDefinition) {
        val members = rsDefinition.getMembers().stream()
                .map(instance -> new BasicDBObject()
                        .append("_id", rsDefinition.getMembers().indexOf(instance))
                        .append("host", Instances.toAddress(instance)))
                .collect(Collectors.toList());

        val config = new BasicDBObject()
                .append("_id", rsDefinition.getClusterID().getValue())
                .append("configsvr", rsDefinition.isConfig())
                .append("members", members);

        return new BasicDBObject(DB_CMD_RS_INIT, config);
    }
}
