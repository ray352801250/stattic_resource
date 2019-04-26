package com.dodoca.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.RedisClient;
import com.dodoca.service.StaticResourceService;
import com.dodoca.service.StaticResourceVersionOneService;
import com.dodoca.utils.HandleRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 10:26
 * @Description:
 */
@Service
public class StaticResourceServiceImpl implements StaticResourceService {
    private static final Logger logger = LoggerFactory.getLogger(StaticResourceServiceImpl.class);

    @Autowired
    private RedisClient redisClient;

    /**
     * 配置不走缓存的host
     */
    @Value("${static_resource_not_cache_hosts}")
    String staticResourceNotCacheHosts;

    @Value("${request_http_type}")
    String requestHttpType;

    @Value("${dodoca_php_stock_interface}")
    String format_php_stock_interface;

    @Autowired
    @Qualifier("php_restTemplate" )
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("stock_restTemplate" )
    RestTemplate restTemplate_stock;

    @Autowired
    RequestPhpService requestPhpService;

    @Autowired
    StaticResourceVersionOneService staticResourceVersionOneServiceImpl;


    @Override
    public JSONObject homePageResource(HttpServletRequest request) {
        JSONObject json_log = new JSONObject();
        String domain = request.getHeader("host");
        String requestUri = request.getHeader("request_uri");
        String cookie = request.getHeader("cookie");
        json_log.put("nginx_request_host", domain);
        json_log.put("nginx_request_url", requestUri);
        String oldRequestUri = requestHttpType + "://" + domain + requestUri;
        String trueRequestUri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));
        String restUrlRedisKey = requestHttpType + "://" + domain + trueRequestUri;
        //需要走一期逻辑的商户域名 static_resource_version_one_hosts
        String staticResourceVersionOneHosts = redisClient.get("static_resource_version_one_hosts");
        if (!StringUtils.isEmpty(staticResourceVersionOneHosts)) {
            String[] versionOneHosts = staticResourceVersionOneHosts.split(",");
            for (String host : versionOneHosts) {
                if (domain.equals(host)) {
                    return staticResourceVersionOneServiceImpl.resource(request);
                }
            }
        }
        //配置不走缓存的host
        if (!StringUtils.isEmpty(staticResourceNotCacheHosts)) {
            String[] notCacheHosts = staticResourceNotCacheHosts.split(",");
            for (String host : notCacheHosts) {
                if (domain.equals(host)) {
                    return requestPhpService.requestPhpServer(cookie, oldRequestUri);
                }
            }
        }
        //shop开头的就根据shopId获取该商铺的类型,如果是平台板/商超版 不走缓存
        if (domain != null && domain.startsWith("shop")) {
            String substring = domain.substring(0, domain.indexOf("."));
            String shopId = substring.substring(4);
            logger.info("shopId: " + shopId);
            //
            return requestPhpService.requestPhpServer(cookie, oldRequestUri);
        }
        String uuid = UUID.randomUUID().toString();
        //获取分布式锁的持有时间
        String staticResourceLockExpireTime = redisClient.get("static_resource_lock_expire_time");
        if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
            //默认分布式锁为三分钟
            staticResourceLockExpireTime = "180000";
        }
        json_log.put("static_resource_lock_expire_time", staticResourceLockExpireTime);
        boolean getLock = redisClient.tryGetDistributedLock(restUrlRedisKey + "_distributed_lock", uuid,  new Integer(staticResourceLockExpireTime));
        //没有获取锁就直接返回缓存结果
        if (!getLock) {
            return JSONObject.parseObject(redisClient.hget(domain, restUrlRedisKey));
        }
        JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, oldRequestUri);
        if (HandleRequestUtil.isNormalResult(jsonObject)) {
            redisClient.hset(domain, trueRequestUri, jsonObject.toJSONString());
        }
        logger.info(json_log.toJSONString());
        return jsonObject;
    }

    @Override
    public JSONObject goodsDetailResource(HttpServletRequest request) {
        return null;
    }

    @Override
    public JSONObject phpStockInterface(HttpServletRequest request) {
        JSONObject jsonStock = null;
        String phpStockInterface = null;
        try {
            String goodsIdUrl = request.getHeader("request_uri").split("\\?")[0];
            String goodsId = goodsIdUrl.substring(goodsIdUrl.lastIndexOf("/")+1,goodsIdUrl.indexOf(".json"));
            long startTime = System.currentTimeMillis();
            phpStockInterface = String.format(format_php_stock_interface,UUID.randomUUID().toString(),goodsId);
            jsonStock = restTemplate_stock.getForEntity(phpStockInterface,JSONObject.class).getBody();
            long endTime = System.currentTimeMillis();
            logger.info("stock_interface_time: "+(endTime - startTime));
            return jsonStock;
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return jsonStock;
    }





}
