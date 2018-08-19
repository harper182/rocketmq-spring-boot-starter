package com.ideal.starter.mq.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.config.MQProperties;
import com.ideal.starter.mq.mapper.EventReceiveTableMapper;
import com.ideal.starter.mq.model.DomainEvent;
import com.ideal.starter.mq.model.EventReceiveStatus;
import com.ideal.starter.mq.model.EventReceiveTable;
import com.ideal.starter.mq.service.DomainEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SimpleConsumer {
    private static final String TAG_SEPARATOR_REGEX = "\\|\\|";
    private final String charset = "UTF-8";
    private ObjectMapper objectMapper = new ObjectMapper();
    private DefaultMQPushConsumer consumer;

    private String consumerGroup;
    private String messageMode;
    private List<MethodInfo> subscribers = new ArrayList<>();
    private MQProperties mqProperties;
    private DomainEventRepository domainEventRepository;
    private ApplicationContext applicationContext;

    public SimpleConsumer(String messageMode, String consumerGroup, List<MethodInfo> subscribers, MQProperties mqProperties, ApplicationContext applicationContext) {
        this.consumerGroup = consumerGroup;
        this.messageMode = messageMode;
        this.subscribers = subscribers;
        this.mqProperties = mqProperties;
        this.applicationContext = applicationContext;
    }


    public synchronized void init() throws Exception {
        if (Objects.isNull(consumer)) {
            Assert.notNull(messageMode, "group cannot be null");
            Assert.notNull(consumerGroup, "namesrvAddr cannot be null");
            Assert.notEmpty(subscribers, "subscribers cannot be null");

            consumer = new DefaultMQPushConsumer(consumerGroup);
            consumer.setNamesrvAddr(mqProperties.getNamesrvAddr());
            consumer.setMessageModel(MessageModel.valueOf(messageMode));
            consumer.setInstanceName(UUID.randomUUID().toString());
            consumer.setMaxReconsumeTimes(mqProperties.getMaxReconsumeTimes());

            RocketMQConsumerListener mqConsumerListener = null;
            for(MethodInfo methodInfo: subscribers){
                mqConsumerListener = methodInfo.getMethod().getAnnotation(RocketMQConsumerListener.class);
                consumer.subscribe(mqConsumerListener.topic(),mqConsumerListener.tag());
            }
            consumer.setMessageListener(new DefaultMessageListenerConcurrently());
            consumer.start();
            }
    }

    public void destroy() {
        if (Objects.nonNull(consumer)) {
            consumer.shutdown();
        }
        log.info("consumer destroyed, {}", this.toString());
    }


    protected class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            if(domainEventRepository == null){
                domainEventRepository = applicationContext.getBean(DomainEventRepository.class);
            }
            for (MessageExt messageExt : msgs) {
                try {
                    log.debug("received messageExt:{}", messageExt);
                    handleMessage(messageExt);
                } catch (Exception e) {
                    log.error("consume message failed. messageExt:{}", messageExt, e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    protected void handleMessage(MessageExt messageExt) {
        for (MethodInfo methodInfo : subscribers) {
            RocketMQConsumerListener subscriber = methodInfo.getMethod().getAnnotation(RocketMQConsumerListener.class);
            if (isMatch(subscriber, messageExt)) {
                Object object = doConvertMessage(messageExt, subscriber.messageType());
                if(object instanceof DomainEvent){
                    ((DomainEvent)object).setMsgId(messageExt.getMsgId());
                }
                try {
                    EventReceiveTable receiveTable = domainEventRepository.getEventReceiveTableByMsgId(messageExt.getMsgId());
                    if(receiveTable == null){
                        domainEventRepository.saveNeedToProcessEvents(Arrays.asList((DomainEvent) object),subscriber);
                    }else if (receiveTable != null && receiveTable.getEventStatus() == EventReceiveStatus.PROCESSED){
                        return;
                    }
                    methodInfo.invoke(object);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                    log.error("invoke method {} failed ",methodInfo.getMethod().getName());
                }
            }
        }
    }

    protected boolean isMatch(RocketMQConsumerListener subscriber, MessageExt messageExt) {
        if (subscriber.topic().equalsIgnoreCase(messageExt.getTopic())) {
            if(subscriber.tag().equals("*")){
                return true;
            }
            String tags = messageExt.getTags();
            if (Objects.nonNull(tags) && tags.length() > 0) {
                Set<String> messageTags = new HashSet<>(Arrays.asList(messageExt.getTags().split(TAG_SEPARATOR_REGEX)));
                Set<String> subscriberTags = new HashSet<>(Arrays.asList(subscriber.tag().split(TAG_SEPARATOR_REGEX)));
                subscriberTags.retainAll(messageTags);
                return subscriberTags.size() > 0;
            }
        }

        return false;
    }

    private Object doConvertMessage(MessageExt messageExt, Class messageType) {
        if (Objects.equals(messageType, MessageExt.class)) {
            return messageExt;
        }

        String messageBody = new String(messageExt.getBody(), Charset.forName(charset));
        if (Objects.equals(messageType, String.class)) {
            return messageBody;
        }

        try {
            return objectMapper.readValue(messageBody, messageType);
        } catch (Exception e) {
            log.error("convert message failed. msgType:{}, message:{}", messageType, messageBody);
            throw new RuntimeException("cannot convert message to " + messageType, e);
        }
    }

    public DefaultMQPushConsumer getConsumer() {
        return consumer;
    }
}
