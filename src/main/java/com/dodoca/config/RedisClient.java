package com.dodoca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/24 16:25
 * @Description:
 */
@Component
public class RedisClient {
    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    @Autowired
    JedisPool jedisPool;


    public Boolean exists(String key) {
        Jedis jedis = null;
        boolean result = false;
        try {
            jedis = jedisPool.getResource();
            result = jedis.exists(key);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return result;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public Boolean hexists(String key, String hexists) {
        Jedis jedis = null;
        boolean result = false;
        try {
            jedis = jedisPool.getResource();
            result = jedis.hexists(key, hexists);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return result;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public String get(String key){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.get(key);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public  String set(String key, String value){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.set(key, value);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }


    public  Long hset(String key, String field, String value){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.hset(key, field, value);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }


    public  String hget(String key, String field){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.hget(key, field);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public  Long hdel(String key, String field){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.hdel(key, field);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }


    public Long setnx(String key, String value){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.setnx(key, value);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    /**
     *
     * @param key
     * @param expireSecond 过期时间 m
     * @param value
     * @return
     */
    public  String setex(String key, int expireSecond, String value){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.setex(key, expireSecond, value);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public String getSet(String key, String value){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.getSet(key, value);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public Long expire(String key, int seconds){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.expire(key, seconds);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public Long ttl(String key) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.ttl(key);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public Long del(String key){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = jedisPool.getResource();
            result = jedis.del(key);
        } catch (Exception e){
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }



    /**
     * 尝试获取分布式锁
     * @param lockKey 锁
     * @param requestId 请求标识
     * @param expireMillisecond 超期时间 ms
     * @return 是否获取成功
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireMillisecond) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireMillisecond);
            if (LOCK_SUCCESS.equals(result)) {
                return true;
            }
        }catch (Exception e) {
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return false;
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
        }catch (Exception e) {
            logger.info("redis异常 , 告警 -----------------");
            e.printStackTrace();
            return false;
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }
}
