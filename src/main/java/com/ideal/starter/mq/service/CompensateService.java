package com.ideal.starter.mq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.base.MethodInfo;
import com.ideal.starter.mq.component.CommonProducer;
import com.ideal.starter.mq.config.ListenerInfoCache;
import com.ideal.starter.mq.model.EventReceiveStatus;
import com.ideal.starter.mq.model.EventReceiveTable;
import com.ideal.starter.mq.model.EventSendStatus;
import com.ideal.starter.mq.model.EventSendTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class CompensateService {
    @Autowired
    private DomainEventRepository domainEventRepository;
    @Autowired
    private CommonProducer commonProducer;
    @Autowired
    private ListenerInfoCache listenerInfoCache;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.rocketmq.max-retry-times}")
    private int messageRetryMaxTime;

    public void compensateMessageNeedToProcess() {
        List<EventReceiveTable> needToProcessDomainEventList = domainEventRepository.getNeedToProcessDomainEventList(DateUtils.addMinutes(new Date(), -1), EventReceiveStatus.NON_PROCESSED);
        needToProcessDomainEventList.forEach(eventTable -> {
            int retryTime = 1;
            List<MethodInfo> methodInfos = listenerInfoCache.getMethodInfoByListenerInfo(eventTable.getConsumerGroup(), eventTable.getMessageMode(), eventTable.getTopic(), eventTable.getTag());
            String message = eventTable.getMessage();
            while (retryTime <= messageRetryMaxTime) {
                methodInfos.stream().forEach(methodInfo -> {
                    Method method = methodInfo.getMethod();
                    RocketMQConsumerListener annotation = method.getAnnotation(RocketMQConsumerListener.class);
                    try {
                        Object messageObj = objectMapper.readValue(message, annotation.messageType());
                        method.invoke(method.getDeclaringClass(), message);
                    } catch (Exception e) {
                        log.error("compensate message needed to process ,retry to send eventId: {} failed,error:{}", eventTable.getId(), e.getMessage());
                    }
                });
                retryTime++;
            }
            if (retryTime > messageRetryMaxTime) {
                log.error("compensate message needed to process ,retry to send eventId: {} failed,retryTime:{}", eventTable.getId(), retryTime);
            }
        });
    }

    public void compensateMessageNeedToSend() {
        List<EventSendTable> needToSendDomainEventList = domainEventRepository.getNeedToSendDomainEventList(DateUtils.addMinutes(new Date(), -1), EventSendStatus.SEND_WAITING);
        needToSendDomainEventList.forEach(domainEvent -> {
            int retryTime = 1;
            while (retryTime <= messageRetryMaxTime) {
                try {
                    SendResult sendResult = commonProducer.syncSend(domainEvent.getTopic() + ":" + domainEvent.getTag(), domainEvent);
                    if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                        domainEventRepository.updateSendStatus(domainEvent.getId(), EventSendStatus.SENT, retryTime);
                        break;
                    } else {
                        retryTime++;
                        continue;
                    }
                } catch (Exception e) {
                    log.error("compensate message needed to send ,retry to send message: {} failed,retryTime:{}", domainEvent.getId(), retryTime);
                    retryTime++;
                }
            }
        });
    }
}
