package edu.reins.mongocloud.mongo.command;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.instance.Instances;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.mongo.request.RsRequest;
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
    public void exec(final RsRequest rsRequest) {
        Assert.notEmpty(rsRequest.getMembers());

        val cmd = buildCmdForInitRs(rsRequest);
        val master = getMaster(rsRequest);

        commandRunner.runCommand(master, cmd);

        eventBus.post(new ClusterEvent(rsRequest.getClusterID(), ClusterEventType.RS_INITED));
    }

    @Nothrow
    @Recover
    public void recover(final MongoCommandException e, final RsRequest rsRequest) {
        LOG.error("< initRs(cluster: {}): failed to init", rsRequest.getClusterID(), e);

        eventBus.post(new ClusterEvent(rsRequest.getClusterID(), ClusterEventType.FAIL, e.getMessage()));
    }

    @Nothrow
    private InstanceHost getMaster(final RsRequest rs) {
        return rs.getMembers().get(0).getHost();
    }

    @Nothrow
    private BasicDBObject buildCmdForInitRs(final RsRequest rsRequest) {
        val members = rsRequest.getMembers().stream()
                .map(instance -> new BasicDBObject()
                        .append("_id", rsRequest.getMembers().indexOf(instance))
                        .append("host", Instances.toAddress(instance)))
                .collect(Collectors.toList());

        val config = new BasicDBObject()
                .append("_id", rsRequest.getClusterID().getValue())
                .append("configsvr", rsRequest.isConfig())
                .append("members", members);

        return new BasicDBObject(DB_CMD_RS_INIT, config);
    }
}
