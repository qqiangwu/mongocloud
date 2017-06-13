package edu.reins.mongocloud.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoCloudConfiguration {
    @Bean
    public ExecutorService threadPoolExecutorFactoryBean() {
        final int corePool = 4;
        final int maxPool = 32;
        final int keepAliveMin = 1;
        final int queueSize = 512;

        return new ThreadPoolExecutor(
                corePool,
                maxPool,
                keepAliveMin, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
