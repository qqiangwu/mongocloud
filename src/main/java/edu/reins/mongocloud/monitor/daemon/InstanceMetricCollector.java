package edu.reins.mongocloud.monitor.daemon;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.MongoMediator;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.monitor.Monitor;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

@Daemon
@Slf4j
public class InstanceMetricCollector {
    @Autowired
    private Monitor monitor;

    @Autowired
    private MongoMediator mongoMediator;

    @Autowired
    private Context context;

    @Nothrow
    @Scheduled(fixedDelay = 10 * 1000)
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

        // TODO collect CPU and tps
    }
}
