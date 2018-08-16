package com.ideal.starter.mq.annotation;

import com.ideal.starter.mq.base.MessageExtConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * RocketMQ消费者自动装配注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQConsumerListener {
    String consumerGroup();

    String topic();

    /**
     * 广播模式消费： BROADCASTING
     * 集群模式消费： CLUSTERING
     * @return 消息模式
     */
    String messageMode() default MessageExtConst.MESSAGE_MODE_CLUSTERING;

    String tag() default "*";

    Class messageType() default String.class;
}
