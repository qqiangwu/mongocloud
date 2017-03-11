package reins.wuqq.resource;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@Slf4j
public class MesosConfiguration {
    private static final String FRAMEWORK_USER = "reins";
    private static final String FRAMEWORK_NAME = "MONGO-M";

    @Bean
    public FrameworkInfo frameworkInfo(final PersistedFrameworkDetail frameworkConfiguration) {
        log.info("FrameworkConfig:init(conf: {})", frameworkConfiguration.get());

        val frameworkInfo = FrameworkInfo.newBuilder();

        if (frameworkConfiguration.getFrameworkId() != null) {
            frameworkInfo.setId(FrameworkID.newBuilder().setValue(frameworkConfiguration.getFrameworkId()));
        }

        // FIXME: increase the timeout
        return frameworkInfo
                .setFailoverTimeout(Duration.ofMinutes(10).get(ChronoUnit.SECONDS))
                .setCheckpoint(true)
                .setUser(FRAMEWORK_USER)
                .setName(FRAMEWORK_NAME)
                .build();
    }

    @Bean
    public SchedulerDriver schedulerDriver(final Scheduler scheduler,
                                           final FrameworkInfo frameworkInfo,
                                           @Value("${zk.mesos}") final String mesosMaster) {
        log.info("SchedulerDriver:init(scheduler, {}, info: {}, zk: {})", scheduler, frameworkInfo, mesosMaster);

        return new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster);
    }
}
