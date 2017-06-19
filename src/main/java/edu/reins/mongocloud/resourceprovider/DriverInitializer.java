package edu.reins.mongocloud.resourceprovider;

import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.MongoCloudInitializer;
import edu.reins.mongocloud.clustermanager.ClusterManagerEvent;
import edu.reins.mongocloud.clustermanager.ClusterManagerEventType;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DriverInitializer implements MongoCloudInitializer {
    @Autowired
    private TaskExecutor executor;

    @Autowired
    private MesosSchedulerDriver schedulerDriver;

    @Autowired
    private EventBus eventBus;

    @Override
    @Nothrow
    public void initialize(final ApplicationContext context) {
        executor.execute(() -> {
            LOG.info("launchDriver");

            schedulerDriver.run();
            schedulerDriver.stop(true);

            LOG.info("driverStops");

            eventBus.post(new ClusterManagerEvent(ClusterManagerEventType.CLOSE));
        });
    }
}
