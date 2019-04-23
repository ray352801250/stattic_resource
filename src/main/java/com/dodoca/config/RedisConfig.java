package com.dodoca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/23 18:15
 * @Description:
 */
@Configuration
@EnableAutoConfiguration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.database}")
    private  int database;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.port}")
    private  int port;

    @Value("${spring.redis.timeout}")
    private  int timeout;

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.jedis.pool")
    public JedisPoolConfig jedisPoolConfig(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        return jedisPoolConfig;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration =
                new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setDatabase(database);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        redisStandaloneConfiguration.setPort(port);
        //开启连接池
        JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jedisPoolingClientConfigurationBuilder =
                (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration.builder();
        jedisPoolingClientConfigurationBuilder.poolConfig(jedisPoolConfig());
        JedisClientConfiguration jedisClientConfiguration = jedisPoolingClientConfigurationBuilder.build();
        JedisConnectionFactory factory = new JedisConnectionFactory(redisStandaloneConfiguration,
                jedisClientConfiguration);
        logger.info("JedisConnectionFactory bean init success.");
        return factory;
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        return new StringRedisTemplate(jedisConnectionFactory());
    }

    @Bean
    public JedisPool redisPoolFactory() {
        logger.info("JedisPool init successful，host -> [{}]；port -> [{}]", host, port);
        return new JedisPool(jedisPoolConfig(), host, port, timeout, password);
    }

    @Bean
    public Jedis jedis() {
        return redisPoolFactory().getResource();
    }



}
