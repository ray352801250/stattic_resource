package com.dodoca.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.common.KafkaSender;
import com.dodoca.config.RedisClient;
import com.dodoca.dao.AloneActivityRecodeMapper;
import com.dodoca.dao.ShopMapper;
import com.dodoca.service.StaticResourceService;
import com.dodoca.service.StaticResourceVersionOneService;
import com.dodoca.utils.DateUtils_java8;
import com.dodoca.utils.HandleRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.time.LocalDateTime;
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

    @Value("${request_http_type}")
    String requestHttpType;

    @Value("${dodoca_php_stock_interface}")
    String formatPhpStockInterface;

    @Value("${static_cache_platform_type}")
    String staticCachePlatformType;

    @Autowired
    @Qualifier("php_restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("stock_restTemplate")
    RestTemplate restTemplateStock;

    @Autowired
    RequestPhpService requestPhpService;

    @Autowired
    StaticResourceVersionOneService staticResourceVersionOneServiceImpl;

    @Autowired
    ShopMapper shopMapper;

    @Autowired
    AloneActivityRecodeMapper aloneActivityRecodeMapper;

    @Autowired
    KafkaSender<JSONObject> kafkaSender;

    @Autowired
    @Qualifier("redisConfigTemplate")
    StringRedisTemplate stringRedisTemplate;


    @Override
    public JSONObject homePageResource(HttpServletRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        JSONObject jsonLog = new JSONObject();
        String domain = request.getHeader("host");
        String requestUri = request.getHeader("request_uri");
        String cookie = request.getHeader("cookie");
        if (cookie == null) {
            cookie = "";
        }
        jsonLog.put("nginx_request_host", domain);
        jsonLog.put("nginx_request_url", requestUri);
        if (domain == null || requestUri == null || !requestUri.contains("?") || !requestUri.contains("static_resources_1532507670=bigdata")) {
            return new JSONObject();
        }
        String trueRequestUri = HandleRequestUtil.handleRequestUrl(requestUri);
        String restUrlRedisKey = requestHttpType + "://" + domain + trueRequestUri;
        try {
            //需要走一期逻辑的商户域名 static_resource_version_one_hosts
            if (stringRedisTemplate.opsForHash().hasKey("static_resource_version_one_hosts", domain)) {
                response.setHeader("resource_from","static_resource_version_one");
                return getHomePageResultFromVersionOne(request, jsonLog, startTime);
            }
            //获取商铺类型 shopPlatformType 1走缓存,2不走缓存
            Object shopPlatformType = stringRedisTemplate.opsForHash().get("shop_platform_type", domain);
            if (StringUtils.isEmpty(shopPlatformType)) {
                cachePlatformType(domain);
                shopPlatformType = stringRedisTemplate.opsForHash().get("shop_platform_type", domain);
            }
            //表示对应的域名不需要走缓存
            if (shopPlatformType == null || shopPlatformType.equals("2")){
                return getResultFromPhp(restUrlRedisKey, cookie, jsonLog, startTime, response);
            }
            if (redisClient.hexists(domain, restUrlRedisKey)) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("domain", domain);
                jsonMessage.put("restUrlRedisKey", restUrlRedisKey);
                jsonMessage.put("cookie", cookie);
                jsonMessage.put("ts", LocalDateTime.now());
                kafkaSender.send(jsonMessage);
                return getHomePageFromRedis(domain,restUrlRedisKey, jsonLog, startTime, response);
            }
            String uuid = UUID.randomUUID().toString();
            //获取分布式锁的持有时间
            String staticResourceLockExpireTime = stringRedisTemplate.opsForValue().get("static_resource_lock_expire_time");
            if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
                stringRedisTemplate.opsForValue().set("static_resource_lock_expire_time", "10000");
                staticResourceLockExpireTime = "10000";
            }
            jsonLog.put("static_resource_lock_expire_time", staticResourceLockExpireTime);
            boolean getLock = redisClient.tryGetDistributedLock("static_distributed_lock_" + restUrlRedisKey, uuid,  new Integer(staticResourceLockExpireTime));
            //没有获取锁就直接返回缓存结果
            if (!getLock) {
                Thread.sleep(2000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getHomePageFromRedis(domain, restUrlRedisKey, jsonLog, startTime, response);
                }
                Thread.sleep(3000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getHomePageFromRedis(domain, restUrlRedisKey, jsonLog, startTime, response);
                }
            }
            JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, restUrlRedisKey);
            redisClient.hset(domain, restUrlRedisKey, jsonObject.toJSONString());
            if (jsonObject.isEmpty()) {
                redisClient.expire(domain, 3);
                logger.info(restUrlRedisKey + " 缓存结果异常,3s后失效");
            }
            jsonLog.put("type", "php");
            long endTime = System.currentTimeMillis();
            response.setHeader("resource_from","php");
            jsonLog.put("interface_time", (endTime - startTime));
            return jsonObject;
        }catch (Exception e) {
            logger.error(e.getMessage(),e);
            jsonLog.put("error_message", e.getMessage());
            return getResultFromPhp(restUrlRedisKey, cookie, jsonLog, startTime, response);
        }finally {
            logger.info(jsonLog.toJSONString());
        }
    }

    @Override
    public JSONObject goodsDetailResource(HttpServletRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        JSONObject jsonLog = new JSONObject();
        String domain = request.getHeader("host");
        String requestUri = request.getHeader("request_uri");
        String cookie = request.getHeader("cookie");
        if (cookie == null) {
            cookie = "";
        }
        jsonLog.put("nginx_request_host", domain);
        jsonLog.put("nginx_request_url", requestUri);
        if (domain == null || requestUri == null || !requestUri.contains("?") || !requestUri.contains("static_goods_detail=bigdata")) {
            return new JSONObject();
        }
        String goodsIdUrl = request.getHeader("request_uri").split("\\?")[0];
        String trueRequestUri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));
        String restUrlRedisKey = requestHttpType + "://" + domain + trueRequestUri;
        try {
            String goodsId = goodsIdUrl.substring(goodsIdUrl.lastIndexOf("/") + 1, goodsIdUrl.indexOf(".json"));
            //需要走一期逻辑的商户域名 static_resource_version_one_hosts
            if (stringRedisTemplate.opsForHash().hasKey("static_resource_version_one_hosts", domain)) {
                response.setHeader("resource_from","static_resource_version_one");
                return getGoodsDetailResultFromVersionOne(request, jsonLog, startTime);
            }
            //获取商铺类型 shopPlatformType 1走缓存,2不走缓存
            Object shopPlatformType = stringRedisTemplate.opsForHash().get("shop_platform_type", domain);
            if (StringUtils.isEmpty(shopPlatformType)) {
                cachePlatformType(domain);
                shopPlatformType = stringRedisTemplate.opsForHash().get("shop_platform_type", domain);
            }
            //表示对应的域名不需要走缓存
            if (shopPlatformType == null || shopPlatformType.equals("2")){
                return getResultFromPhp(restUrlRedisKey, cookie, jsonLog, startTime, response);
            }
            String actType = aloneActivityRecodeMapper.getActType(new Integer(goodsId), DateUtils_java8.formatLoalDateTime(LocalDateTime.now()));
            if (actType != null && !"seckill".equals(actType) && !"tuan".equals(actType) && !"pintuan".equals(actType)) {
                return getResultFromPhp(restUrlRedisKey, cookie, jsonLog, startTime, response);
            }
            if (redisClient.exists(restUrlRedisKey)) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("restUrlRedisKey", restUrlRedisKey);
                jsonMessage.put("cookie", cookie);
                jsonMessage.put("ts", LocalDateTime.now());
                kafkaSender.send(jsonMessage);
                return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime, response);
            }
            String uuid = UUID.randomUUID().toString();
            //获取分布式锁的持有时间
            String staticResourceLockExpireTime = stringRedisTemplate.opsForValue().get("static_resource_lock_expire_time");
            if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
                staticResourceLockExpireTime = "10000";
            }
            jsonLog.put("static_resource_lock_expire_time", staticResourceLockExpireTime);
            boolean getLock = redisClient.tryGetDistributedLock("static_distributed_lock_"+ restUrlRedisKey, uuid,  new Integer(staticResourceLockExpireTime));
            //没有获取锁就直接返回缓存结果
            if (!getLock) {
                Thread.sleep(2000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime, response);
                }
                Thread.sleep(3000);
                if (redisClient.exists(restUrlRedisKey)) {
                    return getGoodsDetailFromRedis(restUrlRedisKey, goodsId, jsonLog, startTime, response);
                }
            }
            JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, restUrlRedisKey);
            redisClient.set(restUrlRedisKey, jsonObject.toJSONString());
            jsonLog.put("type", "php");
            long endTime = System.currentTimeMillis();
            response.setHeader("resource_from","php");
            jsonLog.put("interface_time", (endTime - startTime));
            return jsonObject;
        }catch (Exception e) {
            jsonLog.put("error_message", e.getMessage());
            return getResultFromPhp(restUrlRedisKey, cookie, jsonLog, startTime, response);
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
            String goodsId = goodsIdUrl.substring(goodsIdUrl.lastIndexOf("/") + 1, goodsIdUrl.indexOf(".json"));
            long startTime = System.currentTimeMillis();
            phpStockInterface = String.format(formatPhpStockInterface,UUID.randomUUID().toString(),goodsId);
            jsonStock = restTemplateStock.getForEntity(phpStockInterface,JSONObject.class).getBody();
            long endTime = System.currentTimeMillis();
            logger.info("stock_interface_time: "+ (endTime - startTime));
            return jsonStock;
        } catch (Exception e) {
            logger.error("请求库存服务异常");
            logger.error(e.getMessage(),e);
        }
        return jsonStock;
    }

    @Override
    public JSONObject getKey(String key, Integer database) {
        JSONObject jsonObject = new JSONObject();
        String result;
        if (database == null || database == 0) {
            result = redisClient.get(key);
        }else {
            result = stringRedisTemplate.opsForValue().get(key);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject setKey(String key, String value, Integer database) {
        JSONObject jsonObject = new JSONObject();
        String result = "OK";
        if (database == null || database == 0) {
            result = redisClient.set(key, value);
        }else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject delKey(String key, Integer database) {
        JSONObject jsonObject = new JSONObject();
        Object result = null;
        if (database == null || database == 0) {
            result = redisClient.del(key);
        }else {
            result = stringRedisTemplate.delete(key);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject getExpire(String key, Integer database) {
        JSONObject jsonObject = new JSONObject();
        Long result;
        if (database == null || database == 0) {
            result = redisClient.ttl(key);
        }else {
            result = stringRedisTemplate.getExpire(key);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject hgetKey(String key, String filed, Integer database) {
        JSONObject jsonObject = new JSONObject();
        Object result;
        if (database == null || database == 0) {
            result = redisClient.hget(key, filed);
        }else {
            result = stringRedisTemplate.opsForHash().get(key, filed);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject hsetKey(String key, String filed, String value, Integer database) {
        JSONObject jsonObject = new JSONObject();
        Long result = 0L;
        if (database == null || database == 0) {
            result = redisClient.hset(key, filed, value);
        }else {
            stringRedisTemplate.opsForHash().put(key, filed, value);
            result = 1L;
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    @Override
    public JSONObject hdelKey(String key, String filed, Integer database) {
        JSONObject jsonObject = new JSONObject();
        Long result;
        if (database == null || database == 0) {
            result = redisClient.hdel(key, filed);
        }else {
            result = stringRedisTemplate.opsForHash().delete(key, filed);
        }
        jsonObject.put(key, result);
        return jsonObject;
    }

    /**
     * 将域名需不需要走不走缓存写入redis
     * @param domain
     * @return
     */
    private void putCacheType(String domain, Integer platformType) {
        String[] platformTypes = {"10"};
        if (!StringUtils.isEmpty(staticCachePlatformType)) {
            platformTypes = staticCachePlatformType.split(",");
        }
        if (platformType == null) {
            return ;
        }
        for (String str : platformTypes) {
            if (platformType.equals(new Integer(str))) {
                //需要走缓存
                stringRedisTemplate.opsForHash().put("shop_platform_type", domain, "1");
                return ;
            }
        }
        stringRedisTemplate.opsForHash().put("shop_platform_type", domain, "2");
    }

    /**
     * 查询域名对应商铺的类型查出,判读是否需要走缓存
     * @param domain
     */
    private void cachePlatformType(String domain) {
        if (domain.startsWith("shop")) {
            String substring = domain.substring(0, domain.indexOf("."));
            Integer shopId = new Integer(substring.substring(4));
            //shop开头的就根据shopId获取该商铺的类型
            Integer platformType = shopMapper.getPlatformTypeById(shopId);
            putCacheType(domain, platformType);
        }else {
            String subDomain = domain.substring(0, domain.indexOf("."));
            //非shop开头的就根据domain获取该商铺的类型
            Integer platformType = shopMapper.getPlatformTypeBySubDomain(subDomain);
            putCacheType(domain, platformType);
        }
    }

    /**
     * 更新商品详情页中的活动倒计时时间
     * @param restUrlRedisKey
     * @param goodsId
     * @param jsonLog
     * @throws ParseException
     */
    private JSONObject getGoodsDetailFromRedis(String restUrlRedisKey, String goodsId, JSONObject jsonLog,
                                               Long startTime, HttpServletResponse response) throws ParseException {
        if (redisClient.get(restUrlRedisKey) == null) {
            return new JSONObject();
        }
        JSONObject jsonRedis = JSONObject.parseObject(redisClient.get(restUrlRedisKey));
        //拼团V3倒计时
        HandleRequestUtil.tuanUpdate(jsonRedis);
        //拍卖倒计时
        HandleRequestUtil.auctionUpdate(jsonRedis);
        int stock = requestPhpService.stockService(goodsId,jsonLog);
        jsonRedis.put("stock", stock);
        jsonLog.put("type", "redis");
        long endTime = System.currentTimeMillis();
        jsonLog.put("interface_time", (endTime - startTime));
        response.setHeader("resource_from","redis");
        return jsonRedis;
    }

    /**
     *更新首页中的秒杀活动倒计时时间
     * @param restUrlRedisKey
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getHomePageFromRedis(String domain, String restUrlRedisKey, JSONObject jsonLog, Long startTime, HttpServletResponse response) {
        String hget = redisClient.hget(domain, restUrlRedisKey);
        if (hget == null) {
            return new JSONObject();
        }
        JSONObject jsonRedis = JSONObject.parseObject(redisClient.hget(domain, restUrlRedisKey));
        //更新秒杀倒计时 时间
        HandleRequestUtil.updateNowDate(jsonRedis);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "redis");
        response.setHeader("resource_from","redis");
        jsonLog.put("interface_time", (endTime - startTime));
        return jsonRedis;
    }

    /**
     * 判断是否是要走一期的域名
     * @param domain
     * @return
     */
    private Boolean isVersionOneHost(String domain) {
        String staticResourceVersionOneHosts = stringRedisTemplate.opsForValue().get("static_resource_version_one_hosts");
        if (!StringUtils.isEmpty(staticResourceVersionOneHosts)) {
            String[] versionOneHosts = staticResourceVersionOneHosts.split(",");
            for (String host : versionOneHosts) {
                if (host.equals(domain)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否走缓存
     * @param domain
     * @return
     */
//    private Boolean isNotThroughRedis(String domain) {
//        String staticResourceNotCacheHosts = stringRedisTemplate.opsForValue().get("static_resource_not_cache_hosts");
//        if (!StringUtils.isEmpty(staticResourceNotCacheHosts)) {
//            String[] notCacheHosts = staticResourceNotCacheHosts.split(",");
//            for (String host : notCacheHosts) {
//                if (host.equals(domain)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }


    /**
     * 获取首页结果从一期逻辑
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

    /**
     * 获取详情页结果从一期逻辑
     * @param request
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getGoodsDetailResultFromVersionOne(HttpServletRequest request, JSONObject jsonLog, Long startTime) {
        JSONObject resource = staticResourceVersionOneServiceImpl.resourceGoods(request);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "version-1.0");
        jsonLog.put("interface_time", (endTime - startTime));
        return resource;
    }

    /**
     * 从php获取结果
     * @param restUrlRedisKey
     * @param cookie
     * @param jsonLog
     * @param startTime
     * @return
     */
    private JSONObject getResultFromPhp(String restUrlRedisKey, String cookie, JSONObject jsonLog, Long startTime, HttpServletResponse response) {
        JSONObject jsonObject = requestPhpService.requestPhpServer(cookie, restUrlRedisKey);
        long endTime = System.currentTimeMillis();
        jsonLog.put("type", "php");
        jsonLog.put("interface_time", (endTime - startTime));
        response.setHeader("resource_from","php");
        return jsonObject;
    }




}
