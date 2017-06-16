package edu.reins.mongocloud.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.val;
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

        val blockingQueue = new ArrayBlockingQueue<Runnable>(queueSize);
        val threadFactory = new ThreadFactoryBuilder().setNameFormat("Pool-%d").build();
        val rejectPolicy = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePool,
                maxPool,
                keepAliveMin, TimeUnit.MINUTES,
                blockingQueue,
                threadFactory,
                rejectPolicy);
    }
}
