package edu.reins.mongocloud.impl;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskExecutorFactoryBean;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class MongoCloudConfiguration {
    @Bean("POOL")
    public TaskExecutorFactoryBean taskExecutorFactoryBean() {
        final int corePool = 4;
        final int maxPool = 32;
        final int queueSize = 512;

        val factory = new TaskExecutorFactoryBean();

        factory.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        factory.setKeepAliveSeconds(60);
        factory.setPoolSize(String.format("%d-%d", corePool, maxPool));
        factory.setQueueCapacity(queueSize);

        return factory;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        val scheduler = new ThreadPoolTaskScheduler();

        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("SCHED-");

        return scheduler;
    }
}
