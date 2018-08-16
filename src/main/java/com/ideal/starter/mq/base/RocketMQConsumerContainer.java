package com.ideal.starter.mq.base;

import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.config.MQProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Order
@Configuration
public class RocketMQConsumerContainer implements DisposableBean {
    private String consumerGroup;
    private String messageMode;
    private List<MethodInfo> subscribers = new ArrayList<>();
    private MQProperties mqProperties;
    private SimpleConsumer consumer;

    @Override
    public void destroy() throws Exception {
        if (consumer != null) {
            try {
                consumer.destroy();
            } catch (Exception e) {
                log.error("failed to destroy consumer.", e);
            }
        }
    }

    public void start() {
        initConsumer();
    }

    private synchronized void initConsumer() {
        if (consumer == null) {
            SimpleConsumer simpleConsumer = new SimpleConsumer(messageMode,consumerGroup,subscribers,mqProperties);

            consumer = simpleConsumer;
            try {
                consumer.init();
            } catch (Exception e) {
                log.error("failed to init consumer", e);
                throw new RuntimeException("failed to init consumer");
            }
        }
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getMessageMode() {
        return messageMode;
    }

    public void setMessageMode(String messageMode) {
        this.messageMode = messageMode;
    }

    public List<MethodInfo> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<MethodInfo> subscribers) {
        this.subscribers = subscribers;
    }

    public MQProperties getMqProperties() {
        return mqProperties;
    }

    public void setMqProperties(MQProperties mqProperties) {
        this.mqProperties = mqProperties;
    }

    public SimpleConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(SimpleConsumer consumer) {
        this.consumer = consumer;
    }
}

