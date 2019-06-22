package com.dodoca.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/23 18:15
 * @Description:
 */
@Configuration
public class RedisConfig {
    @Autowired
    RedisProperties redisProperties;

    @Value("${spring.redis.timeout}")
    private int timeout;

    @Value("${spring.redis_config.database}")
    private int configDatabase;

    @Value("${spring.redis_config.host}")
    private String configHost;

    @Value("${spring.redis_config.password}")
    private String configPassword;

    @Value("${spring.redis_config.port}")
    private int configPort;

    @Value("${spring.redis_config.pool.max-idle}")
    private int configMaxIdle;

    @Value("${spring.redis_config.pool.min-idle}")
    private int configMinIdle;

    @Value("${spring.redis_config.pool.max-active}")
    private int configMaxActive;

    @Value("${spring.redis_config.pool.max-wait}")
    private long configMaxWait;

    @Bean
    public JedisPool jedisPool(){
        JedisPoolConfig config = poolConfig(redisProperties.getJedis().getPool().getMaxIdle(), redisProperties.getJedis().getPool().getMinIdle(),
                redisProperties.getJedis().getPool().getMaxActive(), redisProperties.getJedis().getPool().getMaxWait().toMillis());
        return new JedisPool(config, redisProperties.getHost(), redisProperties.getPort(), timeout, redisProperties.getPassword());
    }

    private JedisPoolConfig poolConfig(int maxIdle, int minIdle, int maxTotal, long maxWaitMillis) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        return poolConfig;
    }

    /**
     * 有密码
     */
    private JedisConnectionFactory jedisConnectionFactory(String hostName, int port, String password,
                                                         int maxIdle, int minIdle, int maxTotal, int database,
                                                         long maxWaitMillis) {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(hostName);
        jedisConnectionFactory.setPort(port);
        jedisConnectionFactory.setPassword(password);
        if (database != 0) {
            jedisConnectionFactory.setDatabase(database);
        }
        jedisConnectionFactory.setPoolConfig(poolConfig(maxIdle, minIdle, maxTotal, maxWaitMillis));
        // 初始化连接pool
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }

    /**
     * 无密码
     */
    public JedisConnectionFactory jedisConnectionFactory(String hostName, int port, int maxIdle, int minIdle,
                                                    int maxTotal, int index, long maxWaitMillis) {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(hostName);
        jedisConnectionFactory.setPort(port);
        if (index != 0) {
            jedisConnectionFactory.setDatabase(index);
        }
        jedisConnectionFactory.setPoolConfig(poolConfig(maxIdle, minIdle, maxTotal, maxWaitMillis));
        // 初始化连接pool
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }

    @Bean(name = "redisConfigTemplate")
    public StringRedisTemplate stringRedisTemplate(){
        JedisConnectionFactory connectionFactory = jedisConnectionFactory(configHost, configPort, configPassword, configMaxIdle,
                configMinIdle, configMaxActive, configDatabase, configMaxWait);
        connectionFactory.setTimeout(timeout);
        connectionFactory.afterPropertiesSet();
        return new StringRedisTemplate(connectionFactory);
    }


}
