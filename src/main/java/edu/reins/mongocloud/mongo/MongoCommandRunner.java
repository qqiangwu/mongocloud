package edu.reins.mongocloud.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.support.Units;
import lombok.val;
import org.bson.Document;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MongoCommandRunner {
    private static final BasicDBObject CMD_GETCONF = new BasicDBObject("replSetGetConfig", 1);
    private static final BasicDBObject CMD_SERVER_STATUS = new BasicDBObject("serverStatus", 1);
    private static final BasicDBObject DB_CMD_STATS = new BasicDBObject()
            .append("dbStats", 1)
            .append("scale", Units.MB);
    private static final String DB_ADMIN = "admin";

    // FIXME    EVICTION
    private Map<InstanceHost, MongoClient> mongoClients = new ConcurrentHashMap<>();

    @PreDestroy
    public void destroy() {
        for (final MongoClient client: mongoClients.values()) {
            client.close();
        }
    }

    /**
     * @throws com.mongodb.MongoException        if the command failed
     */
    public Document runAdminCommand(final InstanceHost host, final BasicDBObject cmd) {
        val mongo = getMongoClient(host);
        val db = mongo.getDatabase(DB_ADMIN);

        return db.runCommand(cmd);
    }

    /**
     * @throws com.mongodb.MongoException        if the command failed
     */
    public int getStorageInMB(final InstanceHost host) {
        int storageMB = 0;

        val mongo = getMongoClient(host);

        for (final String dbName: mongo.listDatabaseNames()) {
            final Document result = mongo.getDatabase(dbName).runCommand(DB_CMD_STATS);

            storageMB += result.getInteger("dataSize");
        }

        return storageMB;
    }

    /**
     * @throws com.mongodb.MongoException        if the command failed
     */
    public Document getConfig(final InstanceHost host) {
        return runAdminCommand(host, CMD_GETCONF).get("config", Document.class);
    }

    /**
     * @throws com.mongodb.MongoException        if the command failed
     */
    public Document getServerStatus(final InstanceHost host) {
        return runAdminCommand(host, CMD_SERVER_STATUS);
    }

    private MongoClient getMongoClient(final InstanceHost host) {
        return mongoClients.computeIfAbsent(host, h -> new MongoClient(h.getIp(), h.getPort()));
    }
}