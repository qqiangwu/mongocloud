package edu.reins.mongocloud.cluster.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import edu.reins.mongocloud.instance.InstanceHost;
import lombok.Cleanup;
import lombok.val;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class MongoCommandRunner {
    private static final String DB_ADMIN = "admin";

    /**
     * @throws com.mongodb.MongoCommandException        if the command failed
     */
    public Document runCommand(final InstanceHost host, final BasicDBObject cmd) {
        @Cleanup
        val mongo = new MongoClient(host.getIp(), host.getPort());
        val db = mongo.getDatabase(DB_ADMIN);

        return db.runCommand(cmd);
    }
}
