package com.dodoca.consumer;

import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.RedisClient;
import com.dodoca.service.impl.RequestPhpService;
import com.dodoca.utils.HandleRequestUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * 消费请求
 * @Author: TianGuangHui
 * @Date: 2019/4/28 15:47
 * @Description:
 */
@Component
public class RequestConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RequestConsumer.class);


    @Autowired
    private RedisClient redisClient;

    @Autowired
    RequestPhpService requestPhpService;

    @KafkaListener(topics = "wxrrd_static_resource_server_request")
    public void listen(ConsumerRecord<?, ?> record, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        //判断是否NULL
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            //获取消息
            String message = kafkaMessage.get().toString();
            JSONObject json = JSONObject.parseObject(message);
            String uuid = UUID.randomUUID().toString();
            //获取分布式锁的持有时间
            String staticResourceLockExpireTime = redisClient.get("static_resource_lock_expire_time");
            if (StringUtils.isEmpty(staticResourceLockExpireTime)) {
                staticResourceLockExpireTime = "180000";
            }
            boolean getLock = redisClient.tryGetDistributedLock(message + "_distributed_lock", uuid,  new Integer(staticResourceLockExpireTime));
            //没有获取锁就直接返回
            if (!getLock) {
                return;
            }
            JSONObject jsonObject = requestPhpService.requestPhpServer("", message);
            if (HandleRequestUtil.isNormalResult(jsonObject)) {
                redisClient.set(message, jsonObject.toJSONString());
                logger.info(message + " 缓存更新完毕");
            }
        }
    }
}
