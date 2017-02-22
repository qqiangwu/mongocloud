package reins.wuqq.resource;

import lombok.val;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Period;
import java.time.temporal.ChronoUnit;

@Configuration
public class MesosConfiguration {

    @Bean
    public FrameworkInfo frameworkInfo(final PersistedFrameworkDetail frameworkConfiguration, final Environment env) {
        val frameworkId = FrameworkID.newBuilder()
                .setValue(frameworkConfiguration.getFrameworkId())
                .build();

        return FrameworkInfo.newBuilder()
                .setId(frameworkId)
                .setFailoverTimeout(Period.ofDays(7).get(ChronoUnit.SECONDS))
                .setCheckpoint(false)
                .build();
    }

    @Bean
    public SchedulerDriver schedulerDriver(final Scheduler scheduler,
                                           final FrameworkInfo frameworkInfo,
                                           @Value("zk.mesos") final String mesosMaster) {
        return new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster);
    }
}
