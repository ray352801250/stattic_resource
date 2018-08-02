package com.dodoca.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

@Controller
@RequestMapping("/static")
public class ServiceController {
	private static Logger logger = Logger.getLogger(ServiceController.class);
	

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
    private RedisTemplate<String, ?> redisTemplate;
	
	
	private static SimpleDateFormat sdf_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	
	@RequestMapping("/resource")
	@ResponseBody
	public JSONObject resourceController(HttpServletRequest request) {
		
		long start_time = System.currentTimeMillis();
		
		/**JSONObject jsonHeader = new JSONObject();*/

		try {
			
/**			Enumeration<?> enu = request.getHeaderNames();
			while (enu.hasMoreElements()) {
				String headerName = (String) enu.nextElement();
				String headerValue = request.getHeader(headerName);
				jsonHeader.put(headerName, headerValue);
			}*/
			
			/**String domain = jsonHeader.getString("host");*/
			
			String domain = request.getHeader("host");
			
			String shopId = domain.substring(4,domain.indexOf("."));
			
			
			/**截取掉：1. static_resources_1532507670=bigdata
			  *     2.  url时间戳参数 -- t=unix时间戳
			  *	
			 */
			String true_request_uri = request_url_util(request.getHeader("request_uri"));
			
			
			String rest_url_redis_key = "http://"+domain+true_request_uri;
			
			try {
				
				/** 检查redis服务器连接, 如果redis down机捕捉异常*/
				redisTemplate.opsForHash().hasKey(shopId,rest_url_redis_key);
				
			} catch (Exception e) {
				
				logger.error("redis异常 , 告警 -----------------");
				logger.error(e.getMessage(),e);
				
				/** 直接请求php服务 并且response回vue json*/
				return restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
								
			}
			
			if(!redisTemplate.opsForHash().hasKey(shopId,rest_url_redis_key)){
				
				logger.info("reqeust php interface: "+rest_url_redis_key);
				
				JSONObject jsonRestReturn = restTemplate.getForEntity(rest_url_redis_key,JSONObject.class).getBody();
				
				logger.info("php interface return : "+jsonRestReturn);
				
				redisTemplate.opsForHash().put(shopId,rest_url_redis_key,jsonRestReturn.toJSONString());
				
				/**失效时间永远为第二天的*/
				redisTemplate.expireAt(shopId, expire_time(1));
				
				long end_time = System.currentTimeMillis();
				
				logger.info("php interface execute time : "+((end_time - start_time))+"  sss");
				
				return jsonRestReturn;
				
			}else{
				
				long end_time = System.currentTimeMillis();
				
				logger.info("redis interface execute time : "+((end_time - start_time))+"  sss");
				
				return JSONObject.parseObject(redisTemplate.opsForHash().get(shopId, rest_url_redis_key).toString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		
		
		return new JSONObject();
	}
	
	 public static Date expire_time(int num) throws ParseException{
		          
		    Calendar ca = Calendar.getInstance();

		    ca.add(Calendar.DATE, 1);
		    
		    Date date  = ca.getTime();
		    
		    String date_str = format.format(date);
		    
		    String new_date = date_str+" 02:00:00";
		    
		    
		    return sdf_format.parse(new_date);
		       
	 }
	 
	 public static String request_url_util(String request_url){
		 
		 
		 //goods.json?t=1532934769395
		 if(request_url.indexOf("?")<0)
			 return request_url;
		 
		 int index = request_url.indexOf("?")+1;
		 
		 String url_Param = request_url.substring(index,request_url.length());
		 
		 String param_arrs[] = url_Param.split("&");
		 
		 StringBuffer sbUrl = new StringBuffer(request_url.substring(0,index));
		 for (int i = 0; i < param_arrs.length; i++) {
			 if(param_arrs[i].indexOf("t=")==0 
					 || param_arrs[i].indexOf("static_resources_1532507670=")==0)
				 continue;
			
			 sbUrl.append(param_arrs[i]);
			 sbUrl.append("&");
		}
		 
		 String new_request_url = sbUrl.toString();
		 if(new_request_url.lastIndexOf("&")==new_request_url.length()-1 
				 ||new_request_url.lastIndexOf("?")==new_request_url.length()-1)
			 new_request_url = new_request_url.substring(0,new_request_url.length()-1);
		 
		 
		 return new_request_url;
		 
	 }
	 
	 public static void main(String[] args) throws ParseException {
		//System.out.println(sdf_format.format(expire_time(1)));
		System.out.println(request_url_util("goods.json?&static_resources_1532507670=xx"));
	}	 
}
