package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.exception.InitializationException;
import edu.reins.mongocloud.resource.impl.FrameworkStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@Slf4j
public class MesosConfiguration {
    private static final String FRAMEWORK_NAME = "MongoCloud";

    @Value("${mongocloud.web.ip}")
    String ip;

    @Value("${mongocloud.web.port}")
    int webPort;

    @Value("${mongocloud.failover.minutes}")
    int failoverMinutes;

    @PostConstruct
    public void setup() {
        if (ip == null) {
            throw new InitializationException("LIBPROCESS_IP cannot be null");
        }

        log.info("setup(ip: {}, port: {})", ip, webPort);
    }

    @Bean
    public FrameworkInfo frameworkInfo(final FrameworkStore frameworkConfiguration) {
        log.info("initFramework");

        val frameworkInfo = FrameworkInfo.newBuilder();

        frameworkConfiguration
                .getFrameworkId()
                .ifPresent(id -> {
                    log.info("frameworkDetected(id: {})", id);

                    frameworkInfo.setId(FrameworkID.newBuilder().setValue(id));
                });

        return frameworkInfo
                .setFailoverTimeout(Duration.ofMinutes(failoverMinutes).get(ChronoUnit.SECONDS))
                .setCheckpoint(true)
                .setWebuiUrl(String.format("http://%s:%d", ip, webPort))
                .setName(FRAMEWORK_NAME)
                .setUser(FRAMEWORK_NAME)
                .build();
    }

    @Bean
    public SchedulerDriver schedulerDriver(final Scheduler scheduler,
                                           final FrameworkInfo frameworkInfo,
                                           @Value("${zk.mesos}") final String mesosMaster) {
        log.info("initDriver(scheduler, {}, info: {}, zk: {})", scheduler, frameworkInfo, mesosMaster);

        return new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster);
    }
}