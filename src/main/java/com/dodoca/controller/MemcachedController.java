package com.dodoca.controller;

import com.dodoca.config.MemcachedRunner;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.transcoders.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.SocketAddress;
import java.util.Collection;


/**
 * @description:
 * @author: TianGuangHui
 * @create: 2019-07-16 18:00
 **/
@RestController
@RequestMapping("/static")
public class MemcachedController {
    private static final Logger logger = LoggerFactory.getLogger(MemcachedController.class);

    @Autowired
    private MemcachedRunner memcachedRunner;


    @GetMapping("/getMemcacheKey")
    public Object getMemcacheKey(String key) {
        logger.info("key============ " + key);
        logger.info("hashCode============ " + key.hashCode());
        int aa = key.hashCode()%2;
        logger.info("aa============ " + aa);
        MemcachedClient memcachedClient = memcachedRunner.getClient();
        MemcachedClient memcachedClient2 = memcachedRunner.getClient2();
        logger.info("client1: " + memcachedClient.get(key));
        logger.info("client2: " + memcachedClient2.get(key));
        return memcachedClient.get(key);
    }


    @GetMapping("/setMemcacheKey")
    public String setMemcacheKey(String key, String value) {
        logger.info("key============ " + key);
        logger.info("hashCode============ " + key.hashCode());
        int aa = key.hashCode()%2;
        logger.info("aa============ " + aa);
        MemcachedClient memcachedClient = memcachedRunner.getClient();
        memcachedClient.set(key,10000, value);
        return "success";
    }
}
