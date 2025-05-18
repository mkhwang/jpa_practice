package com.mkhwang.jpa_practice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class ThreadConfig {

  @Bean
  public Executor taskExecutor() {
    int cores = Runtime.getRuntime().availableProcessors();
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(cores * 2);
    executor.setMaxPoolSize(cores * 10);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
  }
}
