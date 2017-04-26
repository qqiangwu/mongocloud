package reins.wuqq.cluster;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Integer ERROR_SHARD_NOT_EXIST = 13129;

    @Autowired
    PersistedClusterDetail clusterDetail;

    public void addShardToCluster(final @Nonnull Instance router, final @Nonnull Instance shard) {
        for (;;) {
            try {
                @Cleanup
                val mongo = new MongoClient(router.getHostIP(), router.getPort());
                val db = mongo.getDatabase(DB_ADMIN);

                db.runCommand(prepareCommandForAttending(shard));

                return;
            } catch (MongoCommandException e) {
                val code = e.getResponse().getInt32("code").intValue();

                switch (code) {
                    case 11000: return;
                    case 6: break;
                    default:
                        log.error("addShardToCluster", e);
                        return;
                }
            }
        }
    }

    public boolean removeShardFromCluster(@Nonnull final Instance shard) {
        val router = clusterDetail.getProxyServer();

        if (!router.isPresent()) {
            return false;
        }

        return removeShardFromClusterImpl(router.get(), shard);
    }

    private boolean removeShardFromClusterImpl(final Instance router, final Instance shard) {
        try {
            @Cleanup
            val mongo = new MongoClient(router.getHostIP(), router.getPort());
            val db = mongo.getDatabase(DB_ADMIN);
            val cmd = new BasicDBObject()
                    .append("removeShard", shard.getId());

            val ret = db.runCommand(cmd);

            if (shardNotExists(ret)) {
                return true;
            }

            return "completed".equals(ret.getString("state"));
        } catch (MongoCommandException e) {
            log.error("removeShard", e);

            return false;
        }
    }

    private boolean shardNotExists(final Document document) {
        val code = document.getInteger("code");
        return code != null && ERROR_SHARD_NOT_EXIST.equals(code.intValue());
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