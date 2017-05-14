package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.MongoCloudInitializer;
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
    public void initialize(final ApplicationContext context) {
        log.info("initDriver");

        executor.execute(() -> {
            Thread.currentThread().setName("MesosDriver");

            log.info("launchDriver");

            schedulerDriver.run();
            schedulerDriver.stop(true);

            log.info("driverStops");
        });
    }
}
