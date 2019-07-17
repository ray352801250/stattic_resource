package com.dodoca.config;

import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @description:
 * @author: TianGuangHui
 * @create: 2019-07-15 10:52
 **/
@Component
public class MemcachedRunner implements CommandLineRunner {
    private Logger logger =  LoggerFactory.getLogger(this.getClass());

    @Resource
    private  MemcacheSource memcacheSource;

    private MemcachedClient client = null;

    private MemcachedClient client2 = null;

    @Override
    public void run(String... args) throws Exception {
        try {
            DefaultConnectionFactory defaultConnectionFactory = new DefaultConnectionFactory();
            client = new MemcachedClient(new InetSocketAddress(memcacheSource.getIp(), memcacheSource.getPort()));
            client2 = new MemcachedClient(new InetSocketAddress(memcacheSource.getIp2(), memcacheSource.getPort2()));

        } catch (IOException e) {
            logger.error("inint MemcachedClient failed ",e);
            logger.error(e.getMessage(), e);
        }
    }


    public MemcachedClient getClient() {
        return client;
    }

    public MemcachedClient getClient2() {
        return client2;
    }
}
