package com.dodoca;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.RedisClient;
import com.dodoca.dao.AloneActivityRecodeMapper;
import com.dodoca.service.impl.CookieDecodeService;
import com.dodoca.service.impl.RequestPhpService;
import com.dodoca.utils.DateUtils_java8;
import com.dodoca.utils.HandleRequestUtil;
import com.whalin.MemCached.MemCachedClient;
import de.ailis.pherialize.Pherialize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.JedisPool;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static de.ailis.pherialize.Pherialize.unserialize;

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

    @Autowired
    private MemCachedClient memCachedClient;

    @Autowired
    private CookieDecodeService cookieDecodeService;


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

    /**
     * 测试Memcached连接
     */
    @Test
    public void testMemcached() {
//        boolean test = memCachedClient.set("laravel:d84536a94e98fe30a1afd17b7182df184bc2716e", "a:5:{s:6:\"_token\";s:40:\"sRIZ4mqGRrzdzJf7ePo6mrL6oZzIaAG0yM0PDxrC\";s:9:\"_previous\";a:1:{s:3:\"url\";s:25:\"http://shop1.weiba456.com\";}s:5:\"flash\";a:2:{s:3:\"old\";a:0:{}s:3:\"new\";a:0:{}}s:6:\"member\";a:11:{s:2:\"id\";i:45;s:14:\"member_account\";i:10000002061;s:13:\"mobile_prefix\";s:3:\"+86\";s:6:\"mobile\";s:11:\"13949025109\";s:4:\"name\";s:6:\"光辉\";s:6:\"avatar\";s:127:\"http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTIebbzqRM9BNok2meHCFOqGJXV9WjmSYMGa0GXRjL6oAtpBBAwC0F4LS7r6rRYq7yzdLaYVvcMbdg/132\";s:16:\"is_verify_mobile\";i:1;s:6:\"gender\";i:1;s:7:\"country\";s:6:\"中国\";s:8:\"province\";s:6:\"上海\";s:4:\"city\";s:6:\"闵行\";}s:9:\"_sf2_meta\";a:3:{s:1:\"u\";i:1562928526;s:1:\"c\";i:1562910179;s:1:\"l\";s:1:\"0\";}}");
//        System.out.println("test： " + test);

        Object o = memCachedClient.get("laravel:d84536a94e98fe30a1afd17b7182df184bc2716e");
        System.out.println("====================...............");
        if (o != null) {
            System.out.println("====================" + o.toString());
        }

    }


    @Test
    public void testUnserialize() {
        String cacheInfo = "a:5:{s:6:\"_token\";s:40:\"ZHqUJDGqWOwVVqBTZpAX2lAVaHvAX1XJIbvP704w\";s:5:\"flash\";a:2:{s:3:\"old\";a:0:{}s:3:\"new\";a:0:{}}s:9:\"_previous\";a:1:{s:3:\"url\";s:25:\"http://shop1.weiba896.com\";}s:6:\"member\";a:11:{s:2:\"id\";i:45;s:14:\"member_account\";i:10000002061;s:13:\"mobile_prefix\";s:3:\"+86\";s:6:\"mobile\";s:11:\"13949025109\";s:4:\"name\";s:6:\"光辉\";s:6:\"avatar\";s:127:\"http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTIebbzqRM9BNok2meHCFOqGJXV9WjmSYMGa0GXRjL6oAtpBBAwC0F4LS7r6rRYq7yzdLaYVvcMbdg/132\";s:16:\"is_verify_mobile\";i:1;s:6:\"gender\";i:1;s:7:\"country\";s:6:\"中国\";s:8:\"province\";s:6:\"上海\";s:4:\"city\";s:6:\"闵行\";}s:9:\"_sf2_meta\";a:3:{s:1:\"u\";i:1562896466;s:1:\"c\";i:1562895834;s:1:\"l\";s:1:\"0\";}}";
        Map<Object, Object> value = (Map<Object, Object>)Pherialize.unserialize(cacheInfo).getValue();
        System.out.println("member: " + value.get("member"));
        System.out.println("guider: " + value.get("guider"));

    }

    /**
     * 测试根据session信息获取访客身份
     * @throws Exception
     */
    @Test
    public void testCookieDecodeService() throws Exception {
//        Map<Object, Object> map = cookieDecodeService.getCacheInfo("eyJpdiI6InFoS2JjTlwvNzRUTzJmS0FjcDVxWThRPT0iLCJ2YWx1ZSI6IjlDVlwvVWpOQXVEaDdleUh4and0NUVLNmc2R1FcL09cL0FnbVdiXC9yc2VrNEZPd1BXaXgwWUlXb25LWDdVWDhURXcraHVyc1lFeXh0RkVQZjdZbG5SMWNqdz09IiwibWFjIjoiNWE5ZDdhZmIwMTFiOWNiYmM2MDkwMDhmODdjM2E2OTUwMGFhMjkxYTQ0MzM5OTcyMzY2NmFlNTZlNTg4MjJmMyJ9");
        Map<Object, Object> map = cookieDecodeService.getCacheInfo("eyJpdiI6IlwvYVwvM2lkd1wvZU9rVDk0Q0VwOTFHR1E9PSIsInZhbHVlIjoiWGVYSWRxMEN4aTVcL0ZORkljSHBoaWpQVVJEbEt5Wk1nZEhQaVdZXC9cL0RoYjNPUFRybWxJWkdcL1lDQURMQW9YaFNjVGQ3Rm0wUkRoY1U4aEtJRlwvQVRDdz09IiwibWFjIjoiNmQzZjIyOGVlNzU0YzYwNGU4NTU3N2U2NDYwOTQ2NjU3YjUyZmRkZjg5NzFkZTc2NjlmNzdkYTFlYjgwNGJkZCJ9");
        System.out.println(map);
        System.out.println(map.get("member"));
    }


    /**
     * 测试url转义
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testUrlDecode() throws UnsupportedEncodingException {
        String keyWord = URLDecoder.decode("eyJpdiI6IlwvYVwvM2lkd1wvZU9rVDk0Q0VwOTFHR1E9PSIsInZhbHVlIjoiWGVYSWRxMEN4aTVcL0ZORkljSHBoaWpQVVJEbEt5Wk1nZEhQaVdZXC9cL0RoYjNPUFRybWxJWkdcL1lDQURMQW9YaFNjVGQ3Rm0wUkRoY1U4aEtJRlwvQVRDdz09IiwibWFjIjoiNmQzZjIyOGVlNzU0YzYwNGU4NTU3N2U2NDYwOTQ2NjU3YjUyZmRkZjg5NzFkZTc2NjlmNzdkYTFlYjgwNGJkZCJ9", "GBK");
        System.out.println(keyWord);  //输出你好
    }


}
