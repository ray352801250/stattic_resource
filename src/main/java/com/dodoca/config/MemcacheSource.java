package com.dodoca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: TianGuangHui
 * @create: 2019-07-15 10:51
 **/
@Component
@ConfigurationProperties(prefix = "memcache")
public class MemcacheSource {

    private String ip;

    private int port;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
