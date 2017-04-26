package reins.wuqq;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import reins.wuqq.support.PersistedState;
import reins.wuqq.support.ZKParser;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
@EnablePrometheusEndpoint
public class MongoMApplication {

	public static void main(String[] args) {
		val context = SpringApplication.run(MongoMApplication.class, args);

		if (args.length == 0) {
			runSchedulerDriver(context);
		} else {
			handleCmdLine(context, args);
		}
	}

	public static void runSchedulerDriver(final ApplicationContext context) {
		log.info("App:run");

		val driver = context.getBean(SchedulerDriver.class);

		val status = driver.run();
		driver.stop(true);

		SpringApplication.exit(context, () -> status.getNumber());
	}

	public static void handleCmdLine(final ApplicationContext context, String[] args) {
		switch (args[0]) {
			case "clear":
				log.info("Clear all states");
				context.getBeansOfType(PersistedState.class).values().forEach(state -> state.clear());
				break;
		}

		SpringApplication.exit(context, ()-> 0);
	}

	@Bean
	public ZooKeeperState zooKeeperState(@Value("${zk.mongo}") final String zk) {
        val matcher = ZKParser.validateZkUrl(zk);

        return new ZooKeeperState(matcher.group(1), 10, TimeUnit.SECONDS, matcher.group(2));
    }
}