package edu.reins.mongocloud.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class MongoCloudConfiguration {
    @Bean
    public Executor threadPoolExecutorFactoryBean() {
        return Executors.newCachedThreadPool();
    }
}
