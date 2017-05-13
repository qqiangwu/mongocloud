package edu.reins.mongocloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.reins.mongocloud.support.ZKParser;
import lombok.val;
import org.apache.mesos.state.State;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoCloudConfiguration {
    @Bean
    public ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean() {
        return new ThreadPoolExecutorFactoryBean();
    }

    @Bean
    public State zooKeeperState(@Value("${zk.mongo}") final String zk) {
        val matcher = ZKParser.validateZkUrl(zk);

        return new ZooKeeperState(matcher.group(1), 10, TimeUnit.SECONDS, matcher.group(2));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
