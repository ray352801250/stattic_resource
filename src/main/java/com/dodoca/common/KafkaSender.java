package com.dodoca.common;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/28 15:24
 * @Description:
 */
@Component
public class KafkaSender<T> {

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * kafka 发送消息
     *
     * @param obj 消息对象
     */
    public void send(T obj) {
        String jsonObj = JSON.toJSONString(obj);
        //发送消息
        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, jsonObj);
//        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
//            @Override
//            public void onFailure(Throwable throwable) {
//                logger.info("Produce: The message failed to be sent:" + throwable.getMessage());
//            }
//            @Override
//            public void onSuccess(SendResult<String, Object> stringObjectSendResult) {
//                //TODO 业务处理
//                logger.info("Produce: The message was sent successfully:");
//                logger.info("Produce: _+_+_+_+_+_+_+ result: " + stringObjectSendResult.toString());
//            }
//        });
    }
}
