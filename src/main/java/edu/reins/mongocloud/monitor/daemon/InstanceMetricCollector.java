package edu.reins.mongocloud.monitor.daemon;

import com.mongodb.MongoCommandException;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceReport;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.mongo.MongoCommandRunner;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.Units;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Daemon
@Slf4j
public class InstanceMetricCollector {
    @Autowired
    private Monitor monitor;

    @Autowired
    private MongoCommandRunner commandRunner;

    @Autowired
    private Context context;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${monitor.query.url}")
    private String queryURL;

    @Value("${monitor.query.pattern}")
    private String queryPattern;

    @Nothrow
    @Scheduled(fixedDelay = Units.SECONDS)
    public void exec() {
        monitor.getInstances()
                .stream()
                .map(this::getInstance)
                .filter(Objects::nonNull)
                .forEach(this::doCollect);
    }

    @Nothrow
    private Instance getInstance(final InstanceID id) {
        final Instance instance = context.getInstances().get(id);

        if (instance == null) {
            LOG.warn("getInstanceFailed(instance: {})", id);
        }

        return instance;
    }

    @Nothrow
    private void doCollect(final Instance instance) {
        LOG.info("collectMetric(instance: {})", instance.getID());

        final Integer cpuPercent = collectCPU(instance);
        final Pair<Integer, Integer> counters = collectTPS(instance);

        final InstanceReport report = InstanceReport.builder()
                .cpuPercent(cpuPercent)
                .totalReads(counters.getLeft())
                .totalWrites(counters.getRight())
                .build();

        eventBus.post(new InstanceEvent(InstanceEventType.UPDATE_STATUS, instance.getID(), report));
    }

    @SuppressWarnings("unchecked")
    @Nothrow
    @Nullable
    private Integer collectCPU(final Instance instance) {
        final String containerName = instance.getContainerInfo().getName();
        final String query = String.format(queryPattern, containerName);

        try {
            final Map response = restTemplate.getForObject(queryURL, Map.class, query);
            final Map data = (Map) response.get("data");

            if (data == null) {
                LOG.warn("getNothing(instance: {}, name: {})", instance.getID(), instance.getContainerInfo().getName());
                return null;
            }

            final List result = (List) data.get("result");
            if (result.isEmpty()) {
                return null;
            }

            final List value = (List)((Map) result.get(0)).get("value");

            return (int) Double.parseDouble((String) value.get(1));
        } catch (RestClientException e) {
            LOG.warn("restFailed(url: {}, query: {})", queryURL, query);

            return null;
        }
    }

    @Nothrow
    private Pair<Integer, Integer> collectTPS(final Instance instance) {
        try {
            final Document opcounters = commandRunner
                    .getServerStatus(instance.getHost())
                    .get("opcounters", Document.class);

            final int reads = opcounters.getInteger("query");
            final int writes = opcounters.getInteger("insert") + opcounters.getInteger("update");

            return Pair.of(reads, writes);
        } catch (MongoCommandException e) {
            LOG.warn("< collectFailed(instance: {}): collect tps failed when connecting mongodb", instance.getID());
            return Pair.of(null, null);
        }
    }
}