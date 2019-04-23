package com.dodoca.service;

import com.dodoca.utils.RedisTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/23 18:50
 * @Description:
 */
@Service
public class TestService {

    @Autowired
    Jedis jedis;

    public void test() {
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
                        boolean getResult = RedisTool.tryGetDistributedLock(jedis, key, uuid.toString(), 20000);
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
