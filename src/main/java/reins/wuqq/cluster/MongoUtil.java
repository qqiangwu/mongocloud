package reins.wuqq.cluster;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import lombok.Cleanup;
import lombok.val;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;

@Component
public class MongoUtil {
    private static final String DB_ADMIN = "admin";

    public void addShardToCluster(final @Nonnull Instance router, final @Nonnull Instance shard) {
        @Cleanup
        val mongo = new MongoClient(router.getHostIP(), router.getPort());
        val db = mongo.getDatabase(DB_ADMIN);

        try {
            db.runCommand(prepareCommandForAttending(shard));
        } catch (MongoCommandException e) {
            val rep = e.getResponse();

            if (rep.getInt32("code").intValue() != 11000) {
                throw e;
            }
        }
    }

    private Bson prepareCommandForAttending(final Instance shard) {
        val shardAddr = String.format("%s:%s", shard.getHostIP(), shard.getPort());

        return new BasicDBObject()
                .append("addShard", shardAddr)
                .append("name", shard.getId());
    }
}