package com.mkhwang.jpa_practice.config;

import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
  private final RedisConnectionFactory redisConnectionFactory;

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(this.redisConnectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public RedisTemplate<String, ProductStock> productStockRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, ProductStock> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(ProductStock.class));
    return template;
  }
}
