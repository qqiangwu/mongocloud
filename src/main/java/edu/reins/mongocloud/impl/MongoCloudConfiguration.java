package edu.reins.mongocloud.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class MongoCloudConfiguration {
    @Bean("POOL")
    public TaskExecutor taskExecutor() {
        final int corePool = 4;
        final int maxPool = 32;
        final int queueSize = 512;

        val taskExecutor = new ThreadPoolTaskExecutor();
        val threadFactoryBuilder = new ThreadFactoryBuilder();

        threadFactoryBuilder.setNameFormat("POOL-%d");
        threadFactoryBuilder.setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setKeepAliveSeconds(60);
        taskExecutor.setCorePoolSize(corePool);
        taskExecutor.setMaxPoolSize(maxPool);
        taskExecutor.setQueueCapacity(queueSize);
        taskExecutor.setThreadFactory(threadFactoryBuilder.build());

        return taskExecutor;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        val scheduler = new ThreadPoolTaskScheduler();

        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("SCHED-");
        scheduler.setErrorHandler(throwable -> System.exit(1));

        return scheduler;
    }
}
