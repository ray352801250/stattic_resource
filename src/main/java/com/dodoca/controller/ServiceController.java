//package com.dodoca.controller;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Set;
//import java.util.TimeZone;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import javax.servlet.http.HttpServletRequest;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.client.RestTemplate;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//@Controller
//@RequestMapping("/static/")
//public class ServiceController {
//	private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
//
//	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//
//	private static TimeZone tz = TimeZone.getTimeZone("UTC");
//
//	@Autowired
//	@Qualifier("php_restTemplate" )
//	private RestTemplate restTemplate;
//
//	@Autowired
//	@Qualifier("stock_restTemplate" )
//	private RestTemplate restTemplate_stock;
//
//	@Autowired
//    private RedisTemplate<String,String> redisTemplate;
//
//	@Value("${dodoca.request.http.type}")
//	String http_type;
//
//	@Value("${dodoca.log.level}")
//	String log_level;
//
//	@Value("${dodoca.php.stock.interface}")
//	String format_php_stock_interface;
//
//
//	@Value("${dodoca.redis.session.time}")
//	int redis_session;
//
//	private static SimpleDateFormat sdf_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//
//
//	@RequestMapping("/resource")
//	@ResponseBody
//	public JSONObject resourceController(HttpServletRequest request) {
//
//		long start_time = System.currentTimeMillis();
//
//		JSONObject json_log = new JSONObject();
//
//		try {
//
//			String domain = request.getHeader("host");
//
//			json_log.put("nginx_request_url", request.getHeader("request_uri"));
//
//			json_log.put("nginx_request_host", domain);
//
//			json_log.put("time_local", getESTime(new Date()));
//
//			/**截取掉：1. static_resources_1532507670=bigdata
//			  *     2.  url时间戳参数 -- t=unix时间戳
//			  *
//			 */
//			String true_request_uri = request_url_util(request.getHeader("request_uri"));
//
//			String rest_url_redis_key = http_type+"://"+domain + true_request_uri;
//
//			json_log.put("redis_key",domain);
//			json_log.put("redis_hashkey",rest_url_redis_key);
//
//			try {
//
//				/** 检查redis服务器连接, 如果redis down机捕捉异常*/
//				if(!redisTemplate.hasKey(domain)){
//
//					/**失效时间 -- 每10分钟 */
//
//					redisTemplate.opsForHash().put(domain, "expire_init", "expire_init");
//
//					redisTemplate.expire(domain, 10, TimeUnit.MINUTES);
//
//					//long a = redisTemplate.getExpire(domain, TimeUnit.SECONDS);
//
//				}else{
//
//					if(redisTemplate.getExpire(domain, TimeUnit.SECONDS)==-1)
//						redisTemplate.expire(domain, 10, TimeUnit.MINUTES);
//				}
//
//			} catch (Exception e) {
//
//				logger.error("redis异常 , 告警 -----------------");
//				logger.error(e.getMessage(),e);
//
//				json_log.put("error_type", "redis_down");
//				json_log.put("error_message", e.getMessage());
//
//				/** 直接请求php服务 并且response回vue json*/
//				return restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
//
//			}
//
//			if(!redisTemplate.opsForHash().hasKey(domain,rest_url_redis_key)){
//				HttpHeaders headers = new HttpHeaders();
//				headers.add("cookie", request.getHeader("cookie"));
//
//				HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
//				JSONObject jsonRestReturn = restTemplate.exchange(rest_url_redis_key, HttpMethod.GET,requestEntity, JSONObject.class).getBody();
//
////				JSONObject jsonRestReturn = restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
//
//				if(compute_json(jsonRestReturn)){
//
//					redisTemplate.opsForHash().put(domain,rest_url_redis_key,jsonRestReturn.toJSONString());
//
//					if(redisTemplate.getExpire(domain, TimeUnit.SECONDS)==-1)
//						redisTemplate.expire(domain, 10, TimeUnit.MINUTES);
//
//					json_log.put("type", "php");
//
//					json_log.put("php_json_staus", 1);
//
//				}else{
//					json_log.put("php_json_staus", 0);
//				}
//
//				json_log.put("php_return_json", jsonRestReturn);
//
//				long end_time = System.currentTimeMillis();
//
//				json_log.put("interface_time", (end_time - start_time));
//
//				return jsonRestReturn;
//
//			}else{
//
//				JSONObject jsonRedis = JSONObject.parseObject(redisTemplate.opsForHash().get(domain, rest_url_redis_key).toString());
//
//				//更新秒杀倒计时时间
//				updateNowDate(jsonRedis);
//
//				long end_time = System.currentTimeMillis();
//
//				json_log.put("type", "redis");
//				json_log.put("interface_time", (end_time - start_time));
//
//				if(log_level.equals("1"))
//					json_log.put("redis_return_json", jsonRedis);
//
//				return jsonRedis;
//			}
//
//		} catch (Exception e) {
//
//			logger.error(e.getMessage(),e);
//
//			json_log.put("error_type", "other");
//			json_log.put("error_message", e.getMessage());
//
//			return new JSONObject();
//
//		}finally{
//			logger.info(json_log.toJSONString());
//		}
//
//	}
//
//	/**
//	 * 更新秒杀倒计时
//	 * @param jsonRedis
//	 */
//	public void updateNowDate(JSONObject jsonRedis){
//
//		try {
//
//			if(jsonRedis.toJSONString().indexOf("seckill")>=0){
//
//				if (jsonRedis.containsKey("data")
//						&& jsonRedis.get("data") instanceof JSONArray){
//
//						JSONArray array_data = jsonRedis.getJSONArray("data");
//
//						for (int i = 0; i < array_data.size(); i++) {
//
//							JSONObject j_data = array_data.getJSONObject(i);
//
//							if(j_data!=null
//									&& j_data.containsKey("type")
//									&& j_data.getString("type").equals("seckill")){
//
//								JSONObject j_cotent = j_data.getJSONObject("content");
//
//								if(j_cotent!=null && j_cotent.containsKey("seckills")){
//
//									if(j_cotent.get("seckills")instanceof JSONArray){
//
//										JSONArray array_seckills = j_cotent.getJSONArray("seckills");
//
//										for (int j = 0; j < array_seckills.size(); j++) {
//
//											JSONObject json_seckills = array_seckills.getJSONObject(j);
//
//											json_seckills.put("now_at", sdf.format(new Date()));
//										}//for
//
//									}//[seckills]
//									if(j_cotent.get("seckills")instanceof JSONObject){
//										JSONObject json_seckills = j_cotent.getJSONObject("seckills");
//										if(json_seckills.containsKey("now_at")){
//											json_seckills.put("now_at", sdf.format(new Date()));
//										}
//									}//{seckills}
//								}
//
//							}
//						}
//
//				}
//			}
//
//		} catch (Exception e) {
//			logger.error(e.getMessage(),e);
//		}
//	}
//
//	@RequestMapping("/resource_redis")
//	@ResponseBody
//	public JSONObject resource_redis(HttpServletRequest request) {
//
//		long s1 = System.currentTimeMillis();
//		/** 检查redis服务器连接, 如果redis down机捕捉异常*/
//		redisTemplate.opsForValue().get("https://hldd.wxrrd.com/goods/112925815.json");
//		long e1 = System.currentTimeMillis();
//		logger.info("redis_time_1: "+(e1 - s1));
//
//		return new JSONObject();
//	}
//
//	@RequestMapping("/resource_goods")
//	@ResponseBody
//	public JSONObject resource_goodsController(HttpServletRequest request) {
//
//		long start_time = System.currentTimeMillis();
//
//		JSONObject json_log = new JSONObject();
//
//		try {
//
//			String domain = request.getHeader("host");
//
//			json_log.put("nginx_request_url", request.getHeader("request_uri"));
//
//			json_log.put("nginx_request_host", domain);
//
//			json_log.put("time_local", getESTime(new Date()));
//
//			/**截取掉：1. static_resources_1532507670=bigdata
//			  *     2.  url时间戳参数 -- t=unix时间戳
//			  *
//			 */
//			String true_request_uri = request_url_util(request.getHeader("request_uri"));
//
//			String goods_id_url = request.getHeader("request_uri").split("\\?")[0];
//			String goods_id = goods_id_url.substring(goods_id_url.lastIndexOf("/")+1,goods_id_url.indexOf(".json"));
//			String rest_url_redis_key = http_type+"://"+domain+true_request_uri;
//
//			json_log.put("redis_key",domain);
//			json_log.put("redis_hashkey",rest_url_redis_key);
//
//			try {
//
//				/** 检查redis服务器连接, 如果redis down机捕捉异常*/
//				redisTemplate.opsForValue().get(rest_url_redis_key);
//
//			} catch (Exception e) {
//
//				logger.error("redis异常 , 告警 -----------------");
//				logger.error(e.getMessage(),e);
//
//				json_log.put("error_type", "redis_down");
//				json_log.put("error_message", e.getMessage());
//
//				/** 直接请求php服务 并且response回vue json*/
//				return restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
//
//			}
//
//			//long s2 = System.currentTimeMillis();
//			if(redisTemplate.opsForValue().get(rest_url_redis_key)==null){
//
//				//JSONObject jsonRestReturn = restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
//
//				HttpHeaders headers = new HttpHeaders();
//				headers.add("cookie", request.getHeader("cookie"));
//
//				HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
//				JSONObject jsonRestReturn = restTemplate.exchange(rest_url_redis_key, HttpMethod.GET,requestEntity, JSONObject.class).getBody();
//
//				int stock = stock_service(goods_id,json_log);
//				jsonRestReturn.put("stock", stock);
//				redisTemplate.opsForValue().set(rest_url_redis_key //key
//						, jsonRestReturn.toJSONString() 	  //value
//						, redis_session, TimeUnit.MINUTES);   //失效时间
//
//				json_log.put("type", "php");
//
//				json_log.put("php_return_json", jsonRestReturn);
//
//				long end_time = System.currentTimeMillis();
//
//				json_log.put("interface_time", (end_time - start_time));
//
//				return jsonRestReturn;
//
//			}else{
//
//
//				JSONObject jsonRedis = JSONObject.parseObject(redisTemplate.opsForValue().get(rest_url_redis_key).toString());
//
//				//拼团V3倒计时
//				tuan_update(jsonRedis);
//
//				//拍卖倒计时
//				auction_update(jsonRedis);
//
//
//				int stock = stock_service(goods_id,json_log);
//				jsonRedis.put("stock", stock);
//
//				long end_time = System.currentTimeMillis();
//
//				json_log.put("type", "redis");
//				json_log.put("interface_time", (end_time - start_time));
//
//				if(log_level.equals("1"))
//					json_log.put("redis_return_json", jsonRedis);
//
//				return jsonRedis;
//			}
//
//		} catch (Exception e) {
//
//			logger.error(e.getMessage(),e);
//
//			json_log.put("error_type", "other");
//			json_log.put("error_message", e.getMessage());
//
//			return new JSONObject();
//
//		}finally{
//			logger.info(json_log.toJSONString());
//		}
//
//	}
//
//	/**
//	 * 拍卖倒计时
//	 * @param jsonRedis
//	 * @throws ParseException
//	 */
//	public void auction_update(JSONObject jsonRedis) throws ParseException{
//
//		if(jsonRedis.containsKey("ump")
//				&& jsonRedis.get("ump") instanceof JSONObject
//				&& jsonRedis.getJSONObject("ump").containsKey("alone")
//				&& jsonRedis.getJSONObject("ump").get("alone") instanceof JSONObject){
//
//			JSONObject json_alone = jsonRedis.getJSONObject("ump").getJSONObject("alone");
//
//			if(json_alone.containsKey("type")
//					&& json_alone.getString("type").equals("auction")
//					&& json_alone.containsKey("data")
//					&& json_alone.get("data") instanceof JSONObject){
//
//				JSONObject json_auction = json_alone.getJSONObject("data");
//
//				if(json_auction.containsKey("start_at") && json_auction.containsKey("count_down")){
//
//					long s_time = sdf.parse(json_auction.getString("start_at")).getTime()/1000;
//
//					long now_unix_time = System.currentTimeMillis()/1000;
//
//					long new_endtime = s_time-now_unix_time;
//
//					//活动未开始
//					if(new_endtime>0)
//						json_auction.put("count_down", new_endtime);
//				}
//			}
//		}
//	}
//
//	/**
//	 * 拼团商品详情页倒计时
//	 * @param jsonRedis
//	 */
//	public void tuan_update(JSONObject jsonRedis){
//
//		if(jsonRedis.containsKey("ump")
//				&& jsonRedis.get("ump") instanceof JSONObject
//				&& jsonRedis.getJSONObject("ump")!=null
//				&& jsonRedis.getJSONObject("ump").containsKey("alone")
//				&& jsonRedis.getJSONObject("ump").get("alone") instanceof JSONObject){
//
//			JSONObject json_alone = jsonRedis.getJSONObject("ump").getJSONObject("alone");
//
//			if(json_alone!=null
//					&& json_alone.containsKey("tuan")
//					&& json_alone.get("tuan") instanceof JSONObject){
//
//				JSONObject json_tuan = json_alone.getJSONObject("tuan");
//
//				if(json_tuan.containsKey("finished_at")
//						&& json_tuan.containsKey("end_time")){
//
//					long f_time = json_tuan.getLongValue("finished_at");
//
//					long now_unix_time = System.currentTimeMillis()/1000;
//
//					long new_endtime = f_time-now_unix_time;
//
//					//活动未开始
//					if(new_endtime>0)
//						json_tuan.put("end_time", new_endtime);
//				}
//			}
//		}
//
//	}
//
//
//	 public static Date expire_time(int num) throws ParseException{
//
//		    Calendar ca = Calendar.getInstance();
//
//		    ca.add(Calendar.DATE, 1);
//
//		    Date date  = ca.getTime();
//
//		    String date_str = format.format(date);
//
//		    String new_date = date_str+" 02:00:00";
//
//
//		    return sdf_format.parse(new_date);
//
//	 }
//
//
//
//
//
//	 public static String getESTime(Date date){
//
//			df.setTimeZone(tz);
//
//			return df.format(date);
//
//	}
//
//
//
//}
