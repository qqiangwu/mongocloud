package reins.wuqq.cluster;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;

@Component
@Slf4j
public class MongoUtil {
    private static final String DB_ADMIN = "admin";
    private static final int GB = 1024 * 1024 * 1024;
    private static final Bson DB_STATS_CMD = new BasicDBObject()
            .append("dbStats", 1)
            .append("scale", GB);

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

    public int getStorageInGB(@Nonnull final Instance router) {
        @Cleanup
        val mongo = new MongoClient(router.getHostIP(), router.getPort());

        int totalCount = 0;

        for (val dbNames: mongo.listDatabaseNames()) {
            val db = mongo.getDatabase(dbNames);

            totalCount += getDbStorageInGB(db);
        }

        return totalCount;
    }

    private int getDbStorageInGB(final MongoDatabase db) {
        try {
            val stats = db.runCommand(DB_STATS_CMD);

            return stats.getInteger("dataSize").intValue();
        } catch (Exception e) {
            log.error("Failed to execute mongo command", e);
        }

        return 0;
    }
}