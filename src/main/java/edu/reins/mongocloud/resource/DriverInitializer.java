package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.MongoCloudInitializer;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class DriverInitializer implements MongoCloudInitializer {
    @Autowired
    Executor executor;

    @Autowired
    MesosSchedulerDriver schedulerDriver;

    @Override
    @Nothrow
    public void initialize(final ApplicationContext context) {
        LOG.info("initDriver");

        executor.execute(() -> {
            Thread.currentThread().setName("MesosDriver");

            LOG.info("launchDriver");

            schedulerDriver.run();
            schedulerDriver.stop(true);

            LOG.info("driverStops");
        });
    }
}
