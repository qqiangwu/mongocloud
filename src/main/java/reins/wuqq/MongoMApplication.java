package reins.wuqq;

import lombok.val;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import reins.wuqq.support.ZKParser;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class MongoMApplication {

	public static void main(String[] args) {
		val context = SpringApplication.run(MongoMApplication.class, args);
		val driver = context.getBean(SchedulerDriver.class);

		val status = driver.run();
		driver.stop(true);

		SpringApplication.exit(context, () -> status.getNumber());
	}

	@Bean
	public ZooKeeperState zooKeeperState(@Value("${zk.mongo}") final String zk) {
        val matcher = ZKParser.validateZkUrl(zk);

        return new ZooKeeperState(matcher.group(1), 10, TimeUnit.SECONDS, matcher.group(2));
    }
}