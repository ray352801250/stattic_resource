package com.dodoca.utils;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/23 17:05
 * @Description:
 */
public class RedisTool {
    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;


    /**
     * 尝试获取分布式锁
     * @param jedis Redis客户端
     * @param lockKey 锁
     * @param requestId 请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, String requestId, int expireTime) {
        String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        if (LOCK_SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }

    /**
     * 释放分布式锁
     * @param jedis Redis客户端
     * @param lockKey 锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean releaseDistributedLock(Jedis jedis, String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        if (RELEASE_SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }


    public static void main(String[] args) {
        //要创建的线程的数量
        CountDownLatch looker = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(10);
        final String key = "lockKey";
        for (int i = 0; i < latch.getCount(); i++) {
            Jedis jedis = new Jedis("127.0.0.1",6379);
            jedis.auth("123456");
            UUID uuid = UUID.randomUUID();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        looker.await();
                        System.out.println(Thread.currentThread().getName() + "竞争资源，尝试获取锁");
                        boolean getResult = tryGetDistributedLock(jedis, key, uuid.toString(), 20000);
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

}
