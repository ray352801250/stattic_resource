package com.dodoca;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.RedisClient;
import com.dodoca.dao.AloneActivityRecodeMapper;
import com.dodoca.service.impl.RequestPhpService;
import com.dodoca.utils.DateUtils_java8;
import com.dodoca.utils.HandleRequestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.JedisPool;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StaticResourceServerApplicationTests {

    @Autowired
    JedisPool jedisPool;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    RequestPhpService requestPhpService;

    @Autowired
    AloneActivityRecodeMapper aloneActivityRecodeMapper;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    @Qualifier("redisConfigTemplate")
    StringRedisTemplate stringRedisTemplate;


    /**
     * 测试获取分布式锁
     */
    @Test
    public void testGetDistributedLock() {
        String uuid = UUID.randomUUID().toString();
        redisClient.tryGetDistributedLock("lock_test", uuid, 20000);

        redisClient.set("test", "111");
        redisClient.expire("test", 20);

    }




    /**
     * 测试jedis连接池
     */
    @Test
    public void testJedisPool() {
        //要创建的线程的数量
        CountDownLatch looker = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(10);
        final String key = "lockKey";
        for (int i = 0; i < latch.getCount(); i++) {
//            Jedis jedis = new Jedis("127.0.0.1",6379);
//            jedis.auth("123456");
            UUID uuid = UUID.randomUUID();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        looker.await();
                        System.out.println(Thread.currentThread().getName() + "竞争资源，尝试获取锁");
                        boolean getResult = redisClient.tryGetDistributedLock(key, uuid.toString(), 20000);
                        if (getResult) {
                            System.out.println(Thread.currentThread().getName() + "获取到了锁，处理业务，用时3秒");
                            Thread.sleep(3000);
//                            boolean releaseResult = releaseDistributedLock(jedis, key, uuid.toString());
//                            if (releaseResult) {
//                                System.out.println(Thread.currentThread().getName() + "业务处理完毕，释放锁");
//                            }
                        } else {
                            System.out.println(Thread.currentThread().getName() + "竞争资源失败，未获取到锁");
                        }
                        latch.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        try {
            System.out.println("准备，5秒后开始");
            Thread.sleep(5000);
            looker.countDown(); //发令  let all threads proceed

            latch.await(); // // wait for all to finish
            System.out.println("结束");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 测试请求php
     */
    @Test
    public void testRequestPhpService() {
        JSONObject jsonObject = requestPhpService.requestPhpServer(null, "http://shop13299363.weiba456.com/design/feature.json?t=1557448746948");
        System.out.println("jsonObject: " + jsonObject);
    }

    @Test
    public void testHandleRequestUtil() {
        String s = HandleRequestUtil.handleRequestUrl("/goods/666219592.json?static_goods_detail=bigdata&t=1557405624280");
        System.out.println("s: " + s);
    }

    /**
     * 测试商品活动信息查询
     */
    @Test
    public void testAloneActivityRecodeMapper() {
        String actType = aloneActivityRecodeMapper.getActType(1210514621, DateUtils_java8.formatLoalDateTime(LocalDateTime.now()));
        System.out.println(actType);
    }

    /**
     * 测试redis连接释放
     */
    @Test
    public void testRedisConnectionFactory2() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            Thread t2 = new Thread(() -> {
                String staticResourceLockExpireTime = stringRedisTemplate.opsForValue().get("static_resource_lock_expire_time");
                System.out.println("staticResourceLockExpireTime: " + staticResourceLockExpireTime);
            });
            t2.start();
        }
    }

    /**
     * 测试redis连接释放
     */
    @Test
    public void testRedisConnectionFactory1() {
        for (int i = 0; i < 200; i++) {
            Thread thread = new Thread(() -> {
                String hget = redisClient.hget("shop13299363.weiba896.com", "http://shop13299363.weiba896.com/shop/getAppConfig.json");
                System.out.println("hget: " + hget);
            });
            thread.start();
        }
    }





}
