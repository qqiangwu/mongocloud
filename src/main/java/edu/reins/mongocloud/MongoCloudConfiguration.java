package edu.reins.mongocloud;

import edu.reins.mongocloud.support.ZKParser;
import lombok.val;
import org.apache.mesos.state.State;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoCloudConfiguration {
    @Bean
    public Executor threadPoolExecutorFactoryBean() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public State zooKeeperState(@Value("${zk.mongo}") final String zk) {
        val matcher = ZKParser.validateZkUrl(zk);

        return new ZooKeeperState(matcher.group(1), 10, TimeUnit.SECONDS, matcher.group(2));
    }
}
