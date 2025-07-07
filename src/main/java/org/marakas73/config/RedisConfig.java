package org.marakas73.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {
    private final RedisProperties redisProperties;
    private final long cacheTtl;
    private final String maxMemory;
    private final String maxMemoryPolicy;

    public RedisConfig(
            RedisProperties redisProperties,
            @Value("${spring.redis.cache.time-to-live}") long cacheTtl,
            @Value("${spring.redis.maxmemory}") String maxMemory,
            @Value("${spring.redis.maxmemory-policy}") String maxMemoryPolicy
    ) {
        this.redisProperties = redisProperties;
        this.cacheTtl = cacheTtl;
        this.maxMemory = maxMemory;
        this.maxMemoryPolicy = maxMemoryPolicy;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        config.setDatabase(redisProperties.getDatabase());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Apply Redis memory configurations
        setRedisConfigurations(factory);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    private void setRedisConfigurations(RedisConnectionFactory factory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;

        // Initialize the connection
        lettuceFactory.afterPropertiesSet();

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();

        // Set maxmemory
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().setConfig("maxmemory", maxMemory);
            return null;
        });

        // Set maxmemory-policy
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().setConfig("maxmemory-policy", maxMemoryPolicy);
            return null;
        });
    }
}
