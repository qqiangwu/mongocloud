package edu.reins.mongocloud;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
@EnablePrometheusEndpoint
public class MongoCloudApplication {
	public static void main(String[] args) {
		val context = SpringApplication.run(MongoCloudApplication.class, args);

		applyInitializers(context);
	}

	private static void applyInitializers(final ApplicationContext context) {
	    context.getBeansOfType(DaemonInitializer.class).values()
                .forEach(initializer -> initializer.initialize(context));
    }
}