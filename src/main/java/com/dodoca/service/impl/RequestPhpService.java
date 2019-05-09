package com.dodoca.service.impl;

import com.alibaba.fastjson.JSONObject;
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

import java.util.UUID;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 10:56
 * @Description:
 */
@Service
public class RequestPhpService {
    private static final Logger logger = LoggerFactory.getLogger(RequestPhpService.class);

    @Autowired
    @Qualifier("php_restTemplate" )
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("stock_restTemplate" )
    private RestTemplate restTemplate_stock;

    @Value("${dodoca_php_stock_interface}")
    String formatPhpStockInterface;

    /**
     * 发送请求到php服务
     * @param cookie
     * @param url
     * @return
     */
    public JSONObject requestPhpServer(String cookie, String url) {
        JSONObject result = new JSONObject();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("cookie", cookie);
            HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
            result = restTemplate.exchange(url, HttpMethod.GET, requestEntity, JSONObject.class).getBody();
        }catch (Exception e) {
            logger.info("请求php服务异常!!!!!!!!!! url: " + url);
            logger.error(e.getMessage(), e);
            return result;
        }
        return result;
    }



    /**
     * 调用php 库存服务
     * 重试1次
     * @param goodsId
     * @return 库存服务异常返回0
     */
    public int stockService(String goodsId, JSONObject jsonLog){

        long startTime = System.currentTimeMillis();

        int stock = 0;

        JSONObject json_stock = null;
        String php_stock_interface = null;
        try {

            php_stock_interface = String.format(formatPhpStockInterface ,UUID.randomUUID().toString(),goodsId);

            json_stock = restTemplate_stock.getForEntity(php_stock_interface,JSONObject.class).getBody();

            if("成功".equals(json_stock.getString("msg"))){
                stock = json_stock.getJSONObject("result")
                        .getJSONObject("goods")
                        .getJSONObject(goodsId)
                        .getIntValue("stock");
            }else
                throw new Exception("获取php库存失败, message: "+json_stock.getString("msg"));
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            try {
                json_stock = restTemplate_stock.getForEntity(php_stock_interface,JSONObject.class).getBody();
                logger.info("json_stock: "+json_stock.toJSONString());

                if(json_stock.getString("msg").equals("成功")){
                    stock = json_stock.getJSONObject("result")
                            .getJSONObject("goods")
                            .getJSONObject(goodsId)
                            .getIntValue("stock");
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(),e2);
                jsonLog.put("error_type", "stockServer ERROR");
                jsonLog.put("error_message", e2.getMessage());
            }
        }
        long end_time = System.currentTimeMillis();
        jsonLog.put("stock_interface_time", (end_time - startTime));
        return stock;

    }

}
