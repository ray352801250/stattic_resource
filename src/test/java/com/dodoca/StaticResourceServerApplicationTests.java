package com.dodoca;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.MemcachedRunner;
import com.dodoca.config.RedisClient;
import com.dodoca.dao.AloneActivityRecodeMapper;
import com.dodoca.service.impl.CookieDecodeService;
import com.dodoca.service.impl.RequestPhpService;
import com.dodoca.utils.DateUtils_java8;
import com.dodoca.utils.HandleRequestUtil;
import de.ailis.pherialize.Pherialize;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
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
    private CookieDecodeService cookieDecodeService;

    @Autowired
    private MemcachedRunner memcachedRunner;


    /**
     * 测试memcached的链接
     */
    @Test
    public void testSetGet()  {
        MemcachedClient memcachedClient = memcachedRunner.getClient();
        memcachedClient.set("testkey",1000,"666666");
        System.out.println("***********  "+memcachedClient.get("testkey").toString());
    }


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
        MemcachedClient memCachedClient = memcachedRunner.getClient();
        OperationFuture<Boolean> set = memCachedClient.set("laravel:d84536a94e98fe30a1afd17b7182df184bc2716e", 10000, "a:5:{s:6:\"_token\";s:40:\"sRIZ4mqGRrzdzJf7ePo6mrL6oZzIaAG0yM0PDxrC\";s:9:\"_previous\";a:1:{s:3:\"url\";s:25:\"http://shop1.weiba456.com\";}s:5:\"flash\";a:2:{s:3:\"old\";a:0:{}s:3:\"new\";a:0:{}}s:6:\"member\";a:11:{s:2:\"id\";i:45;s:14:\"member_account\";i:10000002061;s:13:\"mobile_prefix\";s:3:\"+86\";s:6:\"mobile\";s:11:\"13949025109\";s:4:\"name\";s:6:\"光辉\";s:6:\"avatar\";s:127:\"http://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTIebbzqRM9BNok2meHCFOqGJXV9WjmSYMGa0GXRjL6oAtpBBAwC0F4LS7r6rRYq7yzdLaYVvcMbdg/132\";s:16:\"is_verify_mobile\";i:1;s:6:\"gender\";i:1;s:7:\"country\";s:6:\"中国\";s:8:\"province\";s:6:\"上海\";s:4:\"city\";s:6:\"闵行\";}s:9:\"_sf2_meta\";a:3:{s:1:\"u\";i:1562928526;s:1:\"c\";i:1562910179;s:1:\"l\";s:1:\"0\";}}");
        System.out.println("test： " + set.getStatus());

        Object o = memCachedClient.get("laravel:d84536a94e98fe30a1afd17b7182df184bc2716e");
        System.out.println("====================...............");
        if (o != null) {
            System.out.println("====================" + o.toString());
        }

    }


    @Test
    public void testUnserialize() {
        String cacheInfo = "a:7:{s:6:\"_token\";s:40:\"rEpTRWVetnPxOyAPUICwoVqzd9tl4XaEuE6Ml7lj\";s:9:\"_previous\";a:1:{s:3:\"url\";s:71:\"https://shop13290509.wxrrd.com/goods/121303944.json?is_repeatPurchase=1\";}s:5:\"flash\";a:2:{s:3:\"old\";a:0:{}s:3:\"new\";a:0:{}}s:6:\"member\";a:11:{s:2:\"id\";i:204691819;s:14:\"member_account\";i:10204693835;s:13:\"mobile_prefix\";s:3:\"+86\";s:6:\"mobile\";s:11:\"16666666666\";s:4:\"name\";s:19:\"BIGBIGBOAT李齐周\";s:6:\"avatar\";s:71:\"https://ms.wrcdn.com/member/2019/07/13/O42wsevdVWimmhZiH6zfkpksjmnn.jpg\";s:16:\"is_verify_mobile\";i:1;s:6:\"gender\";i:1;s:7:\"country\";s:6:\"中国\";s:8:\"province\";s:6:\"上海\";s:4:\"city\";s:6:\"卢湾\";}s:6:\"guider\";a:1:{s:11:\"gid13290509\";i:21818627;}s:15:\"fromuid13290509\";i:204691819;s:9:\"_sf2_meta\";a:3:{s:1:\"u\";i:1563420626;s:1:\"c\";i:1563420573;s:1:\"l\";s:1:\"0\";}}";
        Map<Object, Object> value = (Map<Object, Object>)Pherialize.unserialize(cacheInfo).getValue();
        System.out.println(value);
        Object member = value.get("member");
        if (member != null){
            System.out.println("member: " + member);
            String substring = member.toString().substring(1, member.toString().length() - 1);
            System.out.println(substring);
            String[] split = substring.split(",");
            for (String s : split) {
                System.out.println("s: " + s);
                String[] split1 = s.trim().split("=");
                for (String s1 : split1) {
                    System.out.println(s1);
                }
            }
        }
        System.out.println("guider: " + value.get("guider"));

    }

    /**
     * 测试根据session信息获取访客身份
     * @throws Exception
     */
    @Test
    public void testCookieDecodeService() throws Exception {
//        Map<Object, Object> map = cookieDecodeService.getCacheInfo("eyJpdiI6InFoS2JjTlwvNzRUTzJmS0FjcDVxWThRPT0iLCJ2YWx1ZSI6IjlDVlwvVWpOQXVEaDdleUh4and0NUVLNmc2R1FcL09cL0FnbVdiXC9yc2VrNEZPd1BXaXgwWUlXb25LWDdVWDhURXcraHVyc1lFeXh0RkVQZjdZbG5SMWNqdz09IiwibWFjIjoiNWE5ZDdhZmIwMTFiOWNiYmM2MDkwMDhmODdjM2E2OTUwMGFhMjkxYTQ0MzM5OTcyMzY2NmFlNTZlNTg4MjJmMyJ9");
        Map<Object, Object> map = cookieDecodeService.getCacheInfo("eyJpdiI6IjdrdFdUdVh3Unp4T01kVjByNTFxVHc9PSIsInZhbHVlIjoiNTlMaGFSamVJQlFjSUxVemR5K0RPNEdNNWtKYW1CVHVxQmE4dm9lNUlTQTFEekpjSUozTXVPTzNTa21VMTRUUVdLQWlmanliWE5ZTU5VRUFFbUNyWWc9PSIsIm1hYyI6IjkzOWE2MDhmYzg3Nzk0NThlZTdkYjY2OTU0NTY1YzViZTY1MDk0MmFjYWY2Y2EwY2ZiNDIxNWUwMjIyZWZjM2MifQ==");
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


    /**
     * 测试详情页结果获取复购价
     */
    @Test
    public void testPhpResultCheck() {
        String str = "{\"is_virtual\":0,\"comment_count\":\"242\",\"original_price\":\"0.00\",\"img\":\"2019/06/28/Fj2GXOdMLoPk_zHuXpEnlfUdgJaL.jpg\",\"csale\":10239,\"repurchase_price\":null,\"description\":\"<p><img src=\\\"https://ms.wrcdn.com/2019/07/09/FrKO7WzsPHozZT6x2fjMfAnfs3V6.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Fr6jIdb6Vij-3CLQScnrGiry7ps3.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Fu5e3J1C24xKCrjrqRdQtKVgCxQa.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/FnZIeB13V_76sfPwELECfSICiT0e.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Ft6MKXTpPjMWaksY_yB4k1fOHSD4.jpg\\\" style=\\\"max-width:100%\\\" /></p>\\n\",\"component_data\":[{\"content\":{\"edit\":true,\"text\":\"<p><img src=\\\"https://ms.wrcdn.com/2019/07/09/FrKO7WzsPHozZT6x2fjMfAnfs3V6.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Fr6jIdb6Vij-3CLQScnrGiry7ps3.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Fu5e3J1C24xKCrjrqRdQtKVgCxQa.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/FnZIeB13V_76sfPwELECfSICiT0e.jpg\\\" style=\\\"max-width:100%\\\" /><img src=\\\"https://ms.wrcdn.com/2019/06/28/Ft6MKXTpPjMWaksY_yB4k1fOHSD4.jpg\\\" style=\\\"max-width:100%\\\" /></p>\\n\"},\"type\":\"goods_info\"}],\"onsale\":1,\"title\":\"Mheiihoo魅护山茶洗护套装组合\",\"has_props\":1,\"is_discount\":0,\"serve_label\":1,\"trip_type\":0,\"combo_info\":[],\"postage_info\":[],\"has_comment\":1,\"price\":\"149.00\",\"comment_list\":[{\"id\":1593979,\"nickname\":\"（*^0^敏）15985015607.1\",\"portrait\":\"https://ms.wrcdn.com/member/2019/07/08/tcbwN4Vjlh3bKAd9mvbtNoQiGdeR.jpg\",\"is_anonymous\":1,\"has_img\":0,\"content\":\" 好评!\",\"reply\":null,\"reply_at\":\"0000-00-00 00:00:00\",\"created_at\":\"2019-07-17 05:35:47\"},{\"id\":1593885,\"nickname\":\"tiger\",\"portrait\":\"https://ms.wrcdn.com/member/2019/07/10/aQhWknyOLxzKMxWHTc1LfAvJZA1v.jpg\",\"is_anonymous\":1,\"has_img\":0,\"content\":\" 好评!\",\"reply\":null,\"reply_at\":\"0000-00-00 00:00:00\",\"created_at\":\"2019-07-16 21:34:39\"}],\"intro\":\"\",\"ump\":{\"alone\":[],\"coexist\":[]},\"id\":121509779,\"stock\":8988,\"total_csale\":10239,\"temp_login\":0,\"shipment_fee\":\"0.00\",\"imgs\":[{\"id\":77159597,\"goods_id\":121509779,\"img\":\"2019/06/28/Fj2GXOdMLoPk_zHuXpEnlfUdgJaL.jpg\",\"listorder\":0,\"created_at\":\"2019-07-11 12:59:27\",\"updated_at\":\"2019-07-11 12:59:27\"},{\"id\":77159598,\"goods_id\":121509779,\"img\":\"2019/06/28/FpnRcULQk9pZFFMp-GEMCye2zg50.jpg\",\"listorder\":1,\"created_at\":\"2019-07-11 12:59:27\",\"updated_at\":\"2019-07-11 12:59:27\"},{\"id\":77159599,\"goods_id\":121509779,\"img\":\"2019/06/28/Ftei7VVlB41T1wgMSQNNdBCefw67.jpg\",\"listorder\":2,\"created_at\":\"2019-07-11 12:59:27\",\"updated_at\":\"2019-07-11 12:59:27\"},{\"id\":77159600,\"goods_id\":121509779,\"img\":\"2019/06/28/FpjjAyTOwrvMl9A914dWDs3Jp_jQ.jpg\",\"listorder\":3,\"created_at\":\"2019-07-11 12:59:27\",\"updated_at\":\"2019-07-11 12:59:27\"},{\"id\":77159601,\"goods_id\":121509779,\"img\":\"2019/06/28/FqjJryko_Hyzxf8Q46LmIcmsvtXj.jpg\",\"listorder\":4,\"created_at\":\"2019-07-11 12:59:27\",\"updated_at\":\"2019-07-11 12:59:27\"}],\"discount_info\":null,\"is_sku\":1,\"goods_cat_id\":303,\"cquota\":0,\"tax\":\"0.00\",\"tax_show\":0,\"shipment_id\":3572,\"goods_label\":[{\"id\":2036,\"title\":\"7天退换\",\"logo\":\"2016/05/11/FqI4SQ31pkBfwUMPVneSl-ME7jLP.png\",\"content\":\"在未损坏商品的情况下，商家支持消费者申请7天无理由退换货\"},{\"id\":18664,\"title\":\"偏远地区不包邮\",\"logo\":\"2016/09/20/FkPvC8XhKtU_U2qbPDy316TwjwGr.png\",\"content\":\"新疆、青海、西藏地区不包邮，不超重情况下运费4元\"},{\"id\":2037,\"title\":\"正品保证\",\"logo\":\"2016/05/11/FseCO-DxTLGWxnOZ1lsTfGuOAy27.png\",\"content\":\"商家承诺，店铺内所有商品都为正品\"}],\"base_csale\":0,\"postage\":\"0.00\",\"design_goods_id\":0,\"max_price\":\"149.00\",\"is_hexiao\":0,\"price_key\":\"\",\"vip_price\":false,\"original_id\":0}";
        JSONObject jsonObject = JSON.parseObject(str);
        System.out.println("repurchase_price: " + jsonObject.get("repurchase_price"));
        System.out.println(jsonObject.get("repurchase_price") == null);
    }


}
