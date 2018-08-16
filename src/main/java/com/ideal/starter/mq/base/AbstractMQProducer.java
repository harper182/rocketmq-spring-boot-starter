package com.ideal.starter.mq.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

/**
 * RocketMQ的生产者的抽象基类
 */
@Slf4j
public abstract class AbstractMQProducer extends AbstractMessageSendingTemplate<String>{

    private final String charset = "UTF-8";
    private ObjectMapper objectMapper = new ObjectMapper();

    public AbstractMQProducer() {
    }

    @Autowired
    private DefaultMQProducer producer;


    @Override
    public void doSend(String destination, Message<?> message) {
        syncSend(destination, message);
    }

    public SendResult syncSend(String destination, Object payload) {
        return syncSend(destination, payload, producer.getSendMsgTimeout());
    }

    public SendResult syncSend(String destination, Object payload, long timeout) {
        Message<?> message = this.doConvert(payload, null, null);
        return syncSend(destination, message, timeout, null, null);
    }

    public void asyncSend(String destination, Object payload, SendCallback sendCallback) {
        asyncSend(destination, payload, producer.getSendMsgTimeout(), null, null, sendCallback);
    }

    public void asyncSend(String destination, Object payload, long timeout, MessageQueueSelector messageQueueSelector, Object object, SendCallback sendCallback) {
        Message<?> message = this.doConvert(payload, null, null);
        asyncSend(destination, message, timeout, messageQueueSelector, object, sendCallback);
    }

    public SendResult syncSend(String destination, Object payload, long timeout, MessageQueueSelector messageQueueSelector, Object object) {
        Message<?> message = this.doConvert(payload, null, null);
        return syncSend(destination, message, timeout, messageQueueSelector, object);
    }

    protected void asyncSend(String destination, Message<?> message, long timeout, MessageQueueSelector messageQueueSelector, Object object, SendCallback sendCallback) {
        checkParameterValid(destination, message);
        try {
            long now = System.currentTimeMillis();
            org.apache.rocketmq.common.message.Message rocketMsg = convertToRocketMsg(destination, message);
            if (messageQueueSelector != null && object != null) {
                producer.send(rocketMsg, messageQueueSelector, object, sendCallback, timeout);
            } else {
                producer.send(rocketMsg, sendCallback, timeout);
            }
            long costTime = System.currentTimeMillis() - now;
            log.debug("asyncSend message cost: {} ms, message: {}", costTime, rocketMsg);
        } catch (Exception e) {
            log.error("asyncSend failed. destination:{}, message:{},error: ", destination, message,e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected SendResult syncSend(String destination, Message<?> message, long timeout, MessageQueueSelector messageQueueSelector, Object object) {
        checkParameterValid(destination, message);
        try {
            long now = System.currentTimeMillis();
            org.apache.rocketmq.common.message.Message rocketMsg = convertToRocketMsg(destination, message);
            SendResult sendResult = null;
            if (messageQueueSelector != null && object != null) {
                sendResult = producer.send(rocketMsg, messageQueueSelector, object, timeout);
            } else {
                sendResult = producer.send(rocketMsg, timeout);
            }
            long costTime = System.currentTimeMillis() - now;
            log.debug("send message cost: {} ms, msg status: {}, msgId:{}, message: {}", costTime, sendResult.getSendStatus(), sendResult.getMsgId(), rocketMsg);
            return sendResult;
        } catch (Exception e) {
            log.error("syncSend failed. destination:{}, message:{}, error:{} ", destination, message ,e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }

    private void checkParameterValid(String destination, Message<?> message) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.info("syncSend failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }
    }

    public org.apache.rocketmq.common.message.Message convertToRocketMsg(String destination, Message<?> message) {
        Object payloadObj = message.getPayload();
        byte[] payloads;

        if (payloadObj instanceof String) {
            payloads = ((String) payloadObj).getBytes(Charset.forName(charset));
        } else {
            try {
                String jsonObj = this.objectMapper.writeValueAsString(payloadObj);
                payloads = jsonObj.getBytes(Charset.forName(charset));
            } catch (Exception e) {
                throw new RuntimeException("convert to RocketMQ message failed.", e);
            }
        }

        String[] tempArr = destination.split(":", 2);
        String topic = tempArr[0];
        String tags = "";
        if (tempArr.length > 1) {
            tags = tempArr[1];
        }

        org.apache.rocketmq.common.message.Message rocketMsg = new org.apache.rocketmq.common.message.Message(topic, tags, payloads);

        MessageHeaders headers = message.getHeaders();
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            Object keys = headers.get(MessageConst.PROPERTY_KEYS);
            if (!StringUtils.isEmpty(keys)) { // if headers has 'KEYS', set rocketMQ message key
                rocketMsg.setKeys(keys.toString());
            }

            // set rocketMQ message flag
            Object flagObj = headers.getOrDefault("FLAG", "0");
            int flag = 0;
            try {
                flag = Integer.parseInt(flagObj.toString());
            } catch (NumberFormatException e) {
                // ignore
                log.info("flag must be integer, flagObj:{}", flagObj);
            }
            rocketMsg.setFlag(flag);
            // set rocketMQ message waitStoreMsgOkObj
            Object waitStoreMsgOkObj = headers.getOrDefault("WAIT_STORE_MSG_OK", "true");
            boolean waitStoreMsgOK = Boolean.TRUE.equals(waitStoreMsgOkObj);
            rocketMsg.setWaitStoreMsgOK(waitStoreMsgOK);
            headers.entrySet().stream()
                    .filter(entry -> !Objects.equals(entry.getKey(), MessageConst.PROPERTY_KEYS)
                            && !Objects.equals(entry.getKey(), "FLAG")
                            && !Objects.equals(entry.getKey(), "WAIT_STORE_MSG_OK"))
                    .forEach(entry -> {
                        rocketMsg.putUserProperty("USERS_" + entry.getKey(), String.valueOf(entry.getValue())); // add other properties with prefix "USERS_"
                    });

        }

        return rocketMsg;
    }

    @Override
    protected Message<?> doConvert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
        String content;
        if (payload instanceof String) {
            content = (String) payload;
        } else {
            // if payload not as string, use objectMapper change it.
            try {
                content = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.info("convert payload to String failed. payload:{}", payload);
                throw new RuntimeException("convert to payload to String failed.", e);
            }
        }

        org.springframework.messaging.support.MessageBuilder<?> builder = MessageBuilder.withPayload(content);
        if (headers != null) {
            builder.copyHeaders(headers);
        }
        builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);

        Message<?> message = builder.build();
        if (postProcessor != null) {
            message = postProcessor.postProcessMessage(message);
        }
        return message;
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }

        log.info("producer destroyed, {}", this.toString());
    }

}
