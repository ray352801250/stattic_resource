package com.dodoca.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.RedisClient;
import com.dodoca.service.StaticResourceVersionOneService;
import com.dodoca.utils.DateUtils_java8;
import com.dodoca.utils.HandleRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 09:49
 * @Description:
 */
@Service
public class StaticResourceVersionOneServiceImpl implements StaticResourceVersionOneService {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceVersionOneServiceImpl.class);

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static SimpleDateFormat sdf_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    @Qualifier("php_restTemplate" )
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("stock_restTemplate" )
    private RestTemplate restTemplate_stock;

    @Autowired
    private RedisClient redisClient;

    @Value("${request_http_type}")
    String http_type;

    @Value("${dodoca_log_level}")
    String log_level;

    @Value("${dodoca_redis_key_expire_time}")
    int redisSession;

    @Autowired
    RequestPhpService requestPhpService;



    @Override
    public JSONObject resource(HttpServletRequest request) {

        long start_time = System.currentTimeMillis();

        JSONObject json_log = new JSONObject();

        try {

            String domain = request.getHeader("host");

            json_log.put("nginx_request_url", request.getHeader("request_uri"));

            json_log.put("nginx_request_host", domain);

            json_log.put("time_local", getESTime(new Date()));

            /**截取掉：1. static_resources_1532507670=bigdata
             *     2.  url时间戳参数 -- t=unix时间戳
             *
             */
            String true_request_uri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));

            String rest_url_redis_key = http_type+"://"+domain + true_request_uri;

            json_log.put("redis_key",domain);
            json_log.put("redis_hashkey",rest_url_redis_key);

            try {

                /** 检查redis服务器连接, 如果redis down机捕捉异常*/
                if(!redisClient.exists(domain)){

                    /**失效时间 -- 每10分钟 */

                    redisClient.hset(domain, "expire_init", "expire_init");

                    redisClient.expire(domain, 600);

                    //long a = redisTemplate.getExpire(domain, TimeUnit.SECONDS);

                }else{
                    if(redisClient.ttl(domain) == -1) {
                        redisClient.expire(domain, 600);
                    }
                }
            } catch (Exception e) {
                logger.error("redis异常 , 告警 -----------------");
                logger.error(e.getMessage(),e);
                json_log.put("error_type", "redis_down");
                json_log.put("error_message", e.getMessage());
                /** 直接请求php服务 并且response回vue json*/
                return restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();

            }

            if(!redisClient.hexists(domain, rest_url_redis_key)){
                HttpHeaders headers = new HttpHeaders();
                headers.add("cookie", request.getHeader("cookie"));

                HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
                JSONObject jsonRestReturn = restTemplate.exchange(rest_url_redis_key, HttpMethod.GET,requestEntity, JSONObject.class).getBody();

//				JSONObject jsonRestReturn = restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();

                if(jsonRestReturn != null && !jsonRestReturn.isEmpty()){

                    redisClient.hset(domain,rest_url_redis_key, jsonRestReturn.toJSONString());

                    if(redisClient.ttl(domain) == -1) {
                        redisClient.expire(domain, 600);
                    }
                    json_log.put("type", "php");

                    json_log.put("php_json_staus", 1);

                }else{
                    json_log.put("php_json_staus", 0);
                }

                json_log.put("php_return_json", jsonRestReturn);

                long end_time = System.currentTimeMillis();

                json_log.put("interface_time", (end_time - start_time));

                return jsonRestReturn;

            }else{
                JSONObject jsonRedis = JSONObject.parseObject(redisClient.hget(domain, rest_url_redis_key).toString());
                //更新秒杀倒计时时间
                HandleRequestUtil.updateNowDate(jsonRedis);
                long end_time = System.currentTimeMillis();
                json_log.put("type", "redis");
                json_log.put("interface_time", (end_time - start_time));
                if(log_level.equals("1"))
                    json_log.put("redis_return_json", jsonRedis);
                return jsonRedis;
            }

        } catch (Exception e) {

            logger.error(e.getMessage(),e);

            json_log.put("error_type", "other");
            json_log.put("error_message", e.getMessage());

            return new JSONObject();

        }finally{
            logger.info(json_log.toJSONString());
        }

    }


    @Override
    public JSONObject resourceGoods(HttpServletRequest request) {

        long start_time = System.currentTimeMillis();

        JSONObject json_log = new JSONObject();

        try {

            String domain = request.getHeader("host");

            json_log.put("nginx_request_url", request.getHeader("request_uri"));

            json_log.put("nginx_request_host", domain);

            json_log.put("time_local", getESTime(new Date()));

            /**截取掉：1. static_resources_1532507670=bigdata
             *     2.  url时间戳参数 -- t=unix时间戳
             *
             */
            String true_request_uri = HandleRequestUtil.handleRequestUrl(request.getHeader("request_uri"));

            String goods_id_url = request.getHeader("request_uri").split("\\?")[0];
            String goods_id = goods_id_url.substring(goods_id_url.lastIndexOf("/")+1,goods_id_url.indexOf(".json"));
            String rest_url_redis_key = http_type+"://"+domain+true_request_uri;

            json_log.put("redis_key",domain);
            json_log.put("redis_hashkey",rest_url_redis_key);

            try {

                /** 检查redis服务器连接, 如果redis down机捕捉异常*/
                redisClient.get(rest_url_redis_key);

            } catch (Exception e) {

                logger.error("redis异常 , 告警 -----------------");
                logger.error(e.getMessage(),e);

                json_log.put("error_type", "redis_down");
                json_log.put("error_message", e.getMessage());

                /** 直接请求php服务 并且response回vue json*/
                return restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();

            }

            //long s2 = System.currentTimeMillis();
            if(redisClient.get(rest_url_redis_key) == null){

                //JSONObject jsonRestReturn = restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();

                HttpHeaders headers = new HttpHeaders();
                headers.add("cookie", request.getHeader("cookie"));

                HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
                JSONObject jsonRestReturn = restTemplate.exchange(rest_url_redis_key, HttpMethod.GET,requestEntity, JSONObject.class).getBody();

                int stock = requestPhpService.stockService(goods_id,json_log);
                jsonRestReturn.put("stock", stock);
                redisClient.setex(rest_url_redis_key, redisSession, jsonRestReturn.toJSONString());

                json_log.put("type", "php");

                json_log.put("php_return_json", jsonRestReturn);

                long end_time = System.currentTimeMillis();

                json_log.put("interface_time", (end_time - start_time));

                return jsonRestReturn;

            }else{


                JSONObject jsonRedis = JSONObject.parseObject(redisClient.get(rest_url_redis_key));

                //拼团V3倒计时
                HandleRequestUtil.tuanUpdate(jsonRedis);

                //拍卖倒计时
                HandleRequestUtil.auctionUpdate(jsonRedis);

                int stock = requestPhpService.stockService(goods_id,json_log);
                jsonRedis.put("stock", stock);

                long end_time = System.currentTimeMillis();

                json_log.put("type", "redis");
                json_log.put("interface_time", (end_time - start_time));

                if(log_level.equals("1"))
                    json_log.put("redis_return_json", jsonRedis);

                return jsonRedis;
            }

        } catch (Exception e) {

            logger.error(e.getMessage(),e);

            json_log.put("error_type", "other");
            json_log.put("error_message", e.getMessage());

            return new JSONObject();

        }finally{
            logger.info(json_log.toJSONString());
        }

    }


    public static String getESTime(Date date){
        df.setTimeZone(tz);
        return df.format(date);
    }



}
