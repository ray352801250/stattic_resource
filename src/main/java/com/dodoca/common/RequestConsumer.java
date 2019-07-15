package com.dodoca.common;


import com.dodoca.config.RedisClient;
import com.dodoca.service.impl.AsyncConsumerService;
import com.dodoca.service.impl.RequestPhpService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Optional;

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

    @Autowired
    @Qualifier("redisConfigTemplate")
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AsyncConsumerService asyncConsumerService;

//    @KafkaListener(topics = {"${spring.kafka.template.default-topic}"})
    public void listen(ConsumerRecord<?, ?> record) {
        //判断是否NULL
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            //获取消息
            String message = kafkaMessage.get().toString();
            asyncConsumerService.asyncConsumer(message);
        }
    }

}
