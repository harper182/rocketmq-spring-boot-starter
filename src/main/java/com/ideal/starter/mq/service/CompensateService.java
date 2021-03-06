package com.ideal.starter.mq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.base.MethodInfo;
import com.ideal.starter.mq.component.CommonProducer;
import com.ideal.starter.mq.config.ListenerInfoCache;
import com.ideal.starter.mq.config.MQProperties;
import com.ideal.starter.mq.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

    @Autowired
    private MQProperties mqProperties;

    public void compensateMessageNeedToProcess() {
        List<EventReceiveTable> needToProcessDomainEventList = domainEventRepository.getNeedToProcessDomainEventList(DateUtils.addMinutes(new Date(), -1 * mqProperties.getCompensateSendTime()), EventReceiveStatus.NON_PROCESSED, mqProperties.getMessageRetryMaxTime(), mqProperties.getCompensateReceiveLimit());
        for (EventReceiveTable eventTable : needToProcessDomainEventList) {
            int retryTime = eventTable.getRetryTime();
            List<MethodInfo> methodInfos = listenerInfoCache.getMethodInfoByListenerInfo(eventTable.getConsumerGroup(), eventTable.getMessageMode(), eventTable.getTopic(), eventTable.getTag(), eventTable.getListenerName());
            if (CollectionUtils.isEmpty(methodInfos)) {
                continue;
            }
            String message = eventTable.getMessage();
            boolean retrySuccess = false;
            while (retryTime <= mqProperties.getMessageRetryMaxTime()) {
                for (MethodInfo methodInfo : methodInfos) {
                    Method method = methodInfo.getMethod();
                    RocketMQConsumerListener annotation = method.getAnnotation(RocketMQConsumerListener.class);
                    try {
                        DomainEvent messageObj = (DomainEvent) objectMapper.readValue(message, annotation.messageType());
                        method.invoke(methodInfo.getBean(), new Object[]{messageObj});
                        domainEventRepository.updateReceiveStatusToProcessed(eventTable.getId(), EventReceiveStatus.PROCESSED, new Date(), true);
                        retrySuccess = true;
                        log.info("success to compensate message msgId: {},retryTime: {}", eventTable.getMsgId(), retryTime);
                        break;
                    } catch (Exception e) {
                        domainEventRepository.addNonProcessEventRetryTime(eventTable.getId(), new Date());
                        log.error("compensate message needed to process ,retry to process msgId: {} failed,retryTime:{},error:{}", eventTable.getMsgId(), retryTime, e.getMessage());
                    }
                }
                if (retrySuccess) {
                    break;
                }
                retryTime++;
            }
            if (retryTime > mqProperties.getMessageRetryMaxTime()) {
                log.error("compensate message needed to process ,retry to process eventId: {} failed,retryTime:{}", eventTable.getId(), retryTime);
            }
        }
    }

    public void compensateMessageNeedToSend() {
        List<EventSendTable> needToSendDomainEventList = domainEventRepository.getNeedToSendDomainEventList(DateUtils.addMinutes(new Date(), -1 * mqProperties.getCompensateReceiveTime()), EventSendStatus.SEND_WAITING, mqProperties.getMessageRetryMaxTime(), mqProperties.getCompensateSendLimit());
        needToSendDomainEventList.forEach(domainEvent -> {
            int retryTime = domainEvent.getRetryTime();
            while (retryTime <= mqProperties.getMessageRetryMaxTime()) {
                try {
                    SendResult sendResult = commonProducer.syncSend(domainEvent.getTopic() + ":" + domainEvent.getTag(), domainEvent.getMessage());
                    if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                        domainEventRepository.updateSendStatus(domainEvent.getId(), EventSendStatus.SENT, sendResult.getMsgId(), retryTime);
                        log.info("compensate message to send successfully,eventId:{},retryTime:{}", domainEvent.getId(), retryTime);
                        break;
                    } else {
                        domainEventRepository.addSendWaitingEventRetryTime(domainEvent.getId(), new Date());
                        retryTime++;
                        continue;
                    }
                } catch (Exception e) {
                    domainEventRepository.addSendWaitingEventRetryTime(domainEvent.getId(), new Date());
                    log.error("compensate message needed to send ,retry to send message: {} failed,retryTime:{}", domainEvent.getId(), retryTime);
                    retryTime++;
                }
            }
        });
    }
}
