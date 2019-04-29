package com.dodoca.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.common.KafkaSender;
import com.dodoca.config.RedisClient;
import com.dodoca.dao.ShopMapper;
import com.dodoca.service.StaticResourceService;
import com.dodoca.service.StaticResourceVersionOneService;
import com.dodoca.utils.HandleRequestUtil;
import org.apache.kafka.common.protocol.types.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
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

    @Autowired
    ShopMapper shopMapper;

    @Autowired
    KafkaSender<JSONObject> kafkaSender;


    @Override
    public JSONObject homePageResource(HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        JSONObject jsonLog = new JSONObject();
        try {
            String domain = request.getHeader("host");
            String requestUri = request.getHeader("request_uri");
            String cookie = request.getHeader("cookie");
            jsonLog.put("nginx_request_host", domain);
            jsonLog.put("nginx_request_url", requestUri);
            String oldRequestUrl = requestHttpType + "://" + domain + requestUri;
            String trueRequestUri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));
            String restUrlRedisKey = requestHttpType + "://" + domain + trueRequestUri;
            //需要走一期逻辑的商户域名 static_resource_version_one_hosts
            if (isVersionOneHost(domain)) {
                return getHomePageResultFromVersionOne(request, jsonLog, startTime);
            }
            //配置不走缓存的host
            if (isNotThroughRedis(domain)) {
                return getResultFromPhp(oldRequestUrl, cookie, jsonLog, startTime);
            }
            //shop开头的就根据shopId获取该商铺的类型,如果是平台板/商超版 不走缓存
            if (domain != null && domain.startsWith("shop")) {
                if (!isCacheType(domain)) {
                    return getResultFromPhp(oldRequestUrl, cookie, jsonLog, startTime);
                }
            }
            if (redisClient.exists(restUrlRedisKey)) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("restUrlRedisKey", restUrlRedisKey);
                jsonMessage.put("cookie", cookie);
                kafkaSender.send(jsonMessage);
                return getHomePageFromRedis(restUrlRedisKey, jsonLog, startTime);
            }
            String uuid = UUID.randomUUID().toString();
            //获取分布式锁的持有时间
            String staticResourceLockExpireTime = redisClient.get("static_resource_lock_expire_time");
            if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
                staticResourceLockExpireTime = "180000";
            }
            jsonLog.put("static_resource_lock_expire_time", staticResourceLockExpireTime);
            boolean getLock = redisClient.tryGetDistributedLock(restUrlRedisKey + "_distributed_lock", uuid,  new Integer(staticResourceLockExpireTime));
            //没有获取锁就直接返回缓存结果
            if (!getLock) {
                Thread.sleep(2000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getHomePageFromRedis(restUrlRedisKey, jsonLog, startTime);
                }
                Thread.sleep(3000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getHomePageFromRedis(restUrlRedisKey, jsonLog, startTime);
                }
            }
            JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, oldRequestUrl);
            if (HandleRequestUtil.isNormalResult(jsonObject)) {
                redisClient.set(restUrlRedisKey, jsonObject.toJSONString());
            }
            return jsonObject;
        }catch (Exception e) {
            logger.error(e.getMessage(),e);
            jsonLog.put("error_type", "other");
            jsonLog.put("error_message", e.getMessage());
            return new JSONObject();
        }finally {
            logger.info(jsonLog.toJSONString());
        }
    }

    @Override
    public JSONObject goodsDetailResource(HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        JSONObject jsonLog = new JSONObject();
        try {
            String domain = request.getHeader("host");
            String requestUri = request.getHeader("request_uri");
            String cookie = request.getHeader("cookie");
            String goodsIdUrl = request.getHeader("request_uri").split("\\?")[0];
            String goodsId = goodsIdUrl.substring(goodsIdUrl.lastIndexOf("/")+1, goodsIdUrl.indexOf(".json"));
            jsonLog.put("nginx_request_host", domain);
            jsonLog.put("nginx_request_url", requestUri);
            String oldRequestUrl = requestHttpType + "://" + domain + requestUri;
            String trueRequestUri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));
            String restUrlRedisKey = requestHttpType + "://" + domain + trueRequestUri;
            //需要走一期逻辑的商户域名 static_resource_version_one_hosts
            if (isVersionOneHost(domain)) {
                return getGoodsDetailResultFromVersionOne(request, jsonLog, startTime);
            }
            //配置不走缓存的host
            if (isNotThroughRedis(domain)) {
                return getResultFromPhp(oldRequestUrl, cookie, jsonLog, startTime);
            }
            //shop开头的就根据shopId获取该商铺的类型,如果是平台板/商超版 不走缓存
            if (domain != null && domain.startsWith("shop")) {
                if (!isCacheType(domain)) {
                    return getResultFromPhp(oldRequestUrl, cookie, jsonLog, startTime);
                }
            }
            if (redisClient.exists(restUrlRedisKey)) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("restUrlRedisKey", restUrlRedisKey);
                jsonMessage.put("cookie", cookie);
                kafkaSender.send(jsonMessage);
                return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime);
            }
            String uuid = UUID.randomUUID().toString();
            //获取分布式锁的持有时间
            String staticResourceLockExpireTime = redisClient.get("static_resource_lock_expire_time");
            if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
                staticResourceLockExpireTime = "180000";
            }
            jsonLog.put("static_resource_lock_expire_time", staticResourceLockExpireTime);
            boolean getLock = redisClient.tryGetDistributedLock(restUrlRedisKey + "_distributed_lock", uuid,  new Integer(staticResourceLockExpireTime));
            //没有获取锁就直接返回缓存结果
            if (!getLock) {
                Thread.sleep(2000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime);
                }
                Thread.sleep(3000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime);
                }
            }
            JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, oldRequestUrl);
            redisClient.set(restUrlRedisKey, jsonObject.toJSONString());
            return jsonObject;
        }catch (Exception e) {
            logger.error(e.getMessage(),e);
            jsonLog.put("error_type", "other");
            jsonLog.put("error_message", e.getMessage());
            return new JSONObject();
        }finally {
            logger.info(jsonLog.toJSONString());
        }
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


    /**
     * 更新商品详情页中的活动倒计时时间
     * @param restUrlRedisKey
     * @param goodsId
     * @param jsonLog
     * @throws ParseException
     */
    private JSONObject getGoodsDetailFromRedis(String restUrlRedisKey, String goodsId, JSONObject jsonLog, Long startTime) throws ParseException {
        JSONObject jsonRedis = JSONObject.parseObject(redisClient.get(restUrlRedisKey));
        //拼团V3倒计时
        HandleRequestUtil.tuanUpdate(jsonRedis);
        //拍卖倒计时
        HandleRequestUtil.auctionUpdate(jsonRedis);
        int stock = requestPhpService.stock_service(goodsId,jsonLog);
        jsonRedis.put("stock", stock);
        jsonLog.put("type", "redis");
        long endTime = System.currentTimeMillis();
        jsonLog.put("interface_time", (endTime - startTime));
        return jsonRedis;
    }

    /**
     *更新首页中的秒杀活动倒计时时间
     * @param restUrlRedisKey
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getHomePageFromRedis(String restUrlRedisKey, JSONObject jsonLog, Long startTime) {
        JSONObject jsonRedis = JSONObject.parseObject(redisClient.get(restUrlRedisKey));
        //更新秒杀倒计时 时间
        HandleRequestUtil.updateNowDate(jsonRedis);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "redis");
        jsonLog.put("interface_time", (endTime - startTime));
        return jsonRedis;
    }

    /**
     * 判断是否是要走一期的域名
     * @param domain
     * @return
     */
    private Boolean isVersionOneHost(String domain) {
        try {
            String staticResourceVersionOneHosts = redisClient.get("static_resource_version_one_hosts");
            if (!StringUtils.isEmpty(staticResourceVersionOneHosts)) {
                String[] versionOneHosts = staticResourceVersionOneHosts.split(",");
                for (String host : versionOneHosts) {
                    if (host.equals(domain)) {
                        return true;
                    }
                }
            }
        }catch (Exception e) {
            logger.error("redis异常 , 告警 -----------------");
            logger.error(e.getMessage(),e);
            return true;
        }
        return false;
    }

    /**
     * 判断是否走缓存
     * @param domain
     * @return
     */
    private Boolean isNotThroughRedis(String domain) {
        if (!StringUtils.isEmpty(staticResourceNotCacheHosts)) {
            String[] notCacheHosts = staticResourceNotCacheHosts.split(",");
            for (String host : notCacheHosts) {
                if (host.equals(domain)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断商铺类型是否走缓存
     * @param domain
     * @return
     */
    private Boolean isCacheType(String domain) {
        String substring = domain.substring(0, domain.indexOf("."));
        Integer shopId = new Integer(substring.substring(4));
        Integer platformType = shopMapper.getPlatformTypeById(shopId);
        if (platformType == null || platformType != 10) {
            return false;
        }
        return true;
    }





    /**
     * 获取结果从一期逻辑
     * @param request
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getHomePageResultFromVersionOne(HttpServletRequest request, JSONObject jsonLog, Long startTime) {
        JSONObject resource = staticResourceVersionOneServiceImpl.resource(request);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "version-1.0");
        jsonLog.put("interface_time", (endTime - startTime));
        return resource;
    }

    private JSONObject getGoodsDetailResultFromVersionOne(HttpServletRequest request, JSONObject jsonLog, Long startTime) {
        JSONObject resource = staticResourceVersionOneServiceImpl.resourceGoods(request);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "version-1.0");
        jsonLog.put("interface_time", (endTime - startTime));
        return resource;
    }

    /**
     * 从php获取结果
     * @param oldRequestUrl
     * @param cookie
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getResultFromPhp(String oldRequestUrl, String cookie, JSONObject jsonLog, Long startTime) {
        JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, oldRequestUrl);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "php");
        jsonLog.put("interface_time", (endTime - startTime));
        return jsonObject;
    }




}
