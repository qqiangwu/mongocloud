package edu.reins.mongocloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class DaemonInitializer implements MongoCloudInitializer {
    @Autowired
    Executor executor;

    @Override
    public void initialize(final ApplicationContext context) {
        log.info("> launchDaemons");

        context.getBeansWithAnnotation(Daemon.class)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof Runnable)
                .forEach(entry -> {
                    log.info("< launchDaemon(daemon: {})", entry.getKey());

                    executor.execute((Runnable) entry.getValue());
                });
    }
}