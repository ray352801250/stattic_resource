package com.dodoca.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 14:34
 * @Description:
 */
public class HandleRequestUtil {

    public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 处理请求的url 去除时间戳/uuid/静态化参数标识
     * @param requestUrl  /shop/uc_center.json?t=15529598241401&uuid=1b5c917b-7cee-49f1-dafc-44fe8e5ac0d9
     * @return
     */
    public static String handleRequestUrl(String requestUrl){
        if(requestUrl == null || !requestUrl.contains("?")) {
            return requestUrl;
        }
        int index = requestUrl.indexOf("?") + 1;
        //获取参数部分
        String urlParam = requestUrl.substring(index, requestUrl.length());
        String[] paramArrs = urlParam.split("&");
        StringBuffer sbUrl = new StringBuffer(requestUrl.substring(0,index));
        for (int i = 0; i < paramArrs.length; i++) {
            if(paramArrs[i].indexOf("t=")==0 || paramArrs[i].indexOf("static_resources_1532507670=")==0
                    || paramArrs[i].indexOf("static_goods_detail=")==0 || paramArrs[i].indexOf("uuid=")==0) {
                continue;
            }
            sbUrl.append(paramArrs[i]);
            sbUrl.append("&");
        }
        String newRequestUrl = sbUrl.toString();
        if(newRequestUrl.lastIndexOf("&") == newRequestUrl.length()-1 || newRequestUrl.lastIndexOf("?") == newRequestUrl.length()-1) {
            newRequestUrl = newRequestUrl.substring(0,newRequestUrl.length()-1);
        }
        return newRequestUrl;
    }


    /**
     * 判定请求结果是否正常
     * @param jsonRestReturn
     * @return
     */
    public static boolean isNormalResult(JSONObject jsonRestReturn) {
        if (jsonRestReturn == null || jsonRestReturn.isEmpty() || !jsonRestReturn.containsKey("data")) {
            return false;
        }
        if(jsonRestReturn.get("data") instanceof JSONObject || jsonRestReturn.get("data") instanceof JSONArray){
            return jsonRestReturn.get("data").toString().trim().length() > 2;
        }
        return true;
    }


    /**
     * 更新秒杀倒计时
     * @param jsonRedis
     */
    public static void updateNowDate(JSONObject jsonRedis){
        if(jsonRedis == null || !jsonRedis.toJSONString().contains("seckill")){
            return;
        }
        if (jsonRedis.containsKey("data") && jsonRedis.get("data") instanceof JSONArray){
            JSONArray arrayData = jsonRedis.getJSONArray("data");
            for (int i = 0; i < arrayData.size(); i++) {
                JSONObject jsonData = arrayData.getJSONObject(i);
                if(jsonData != null && jsonData.containsKey("type") && "seckill".equals(jsonData.getString("type"))){
                    JSONObject jsonContent = jsonData.getJSONObject("content");
                    if(jsonContent == null || !jsonContent.containsKey("seckills")){
                        return;
                    }
                    if(jsonContent.get("seckills") instanceof JSONArray){
                        JSONArray arraySeckills = jsonContent.getJSONArray("seckills");
                        for (int j = 0; j < arraySeckills.size(); j++) {
                            JSONObject jsonSeckills = arraySeckills.getJSONObject(j);
                            jsonSeckills.put("now_at", dateTimeFormat.format(new Date()));
                        }
                    }
                    if(jsonContent.get("seckills") instanceof JSONObject){
                        JSONObject jsonSeckills = jsonContent.getJSONObject("seckills");
                        if(jsonSeckills.containsKey("now_at")){
                            jsonSeckills.put("now_at", dateTimeFormat.format(new Date()));
                        }
                    }
                }
            }
        }
    }


    /**
     * 拼团商品详情页倒计时
     * @param jsonRedis
     */
    public static void tuanUpdate(JSONObject jsonRedis){
        if(jsonRedis != null && jsonRedis.containsKey("ump")  && jsonRedis.get("ump") instanceof JSONObject
                && jsonRedis.getJSONObject("ump") !=null  && jsonRedis.getJSONObject("ump").containsKey("alone")
                && jsonRedis.getJSONObject("ump").get("alone") instanceof JSONObject){

            JSONObject jsonAlone = jsonRedis.getJSONObject("ump").getJSONObject("alone");
            if(jsonAlone != null && jsonAlone.containsKey("tuan") && jsonAlone.get("tuan") instanceof JSONObject){
                JSONObject jsonTuan = jsonAlone.getJSONObject("tuan");
                if(jsonTuan.containsKey("finished_at") && jsonTuan.containsKey("end_time")){
                    long finishedTime = jsonTuan.getLongValue("finished_at");
                    long nowUnixTime = System.currentTimeMillis()/1000;
                    long newEndTime = finishedTime - nowUnixTime;
                    //活动未开始
                    if(newEndTime > 0) {
                        jsonTuan.put("end_time", newEndTime);
                    }
                }
            }
        }

    }


    /**
     * 拍卖倒计时
     * @param jsonRedis
     * @throws ParseException
     */
    public static void auctionUpdate(JSONObject jsonRedis) throws ParseException{
        if(jsonRedis != null && jsonRedis.containsKey("ump")  && jsonRedis.get("ump") instanceof JSONObject
                && jsonRedis.getJSONObject("ump").containsKey("alone")
                && jsonRedis.getJSONObject("ump").get("alone") instanceof JSONObject){

            JSONObject json_alone = jsonRedis.getJSONObject("ump").getJSONObject("alone");

            if(json_alone.containsKey("type")
                    && "auction".equals(json_alone.getString("type"))
                    && json_alone.containsKey("data")
                    && json_alone.get("data") instanceof JSONObject){

                JSONObject json_auction = json_alone.getJSONObject("data");
                if(!json_auction.containsKey("start_at") || !json_auction.containsKey("count_down")){
                    return;
                }
                long startTime = dateTimeFormat.parse(json_auction.getString("start_at")).getTime()/1000;
                long nowUnixTime = System.currentTimeMillis()/1000;
                long newEndTime = startTime-nowUnixTime;
                //活动未开始
                if(newEndTime > 0) {
                    json_auction.put("count_down", newEndTime);
                }
            }
        }
    }



}
