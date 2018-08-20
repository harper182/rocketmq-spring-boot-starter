package com.ideal.starter.mq.service;

import com.ideal.starter.mq.component.AfterCommitExecutor;
import com.ideal.starter.mq.component.CommonProducer;
import com.ideal.starter.mq.model.DomainEvent;
import com.ideal.starter.mq.model.EventSendStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DomainEventDispatcherService {
    @Autowired
    private DomainEventRepository domainEventRepository;

    @Autowired
    private CommonProducer commonProducer;

    @Autowired
    private AfterCommitExecutor afterCommitExecutor;

    public void saveAndDispatcher(List<DomainEvent> domainEvents) {
        domainEventRepository.saveNeedToSendEvents(domainEvents);
        dispatchEvents(domainEvents);
    }

    public void dispatchEvents(List<DomainEvent> events) {
        for(DomainEvent domainEvent : events) {
            dispatch(domainEvent);
        }
    }

    private void dispatch(DomainEvent event) {
        System.out.println("sent msg :"+event.getTopic() +":"+event.getTag());
        Runnable runnable = () -> {
            SendResult sendResult = commonProducer.syncSend(event.getTopic() + ":" + event.getTag(), event);
            log.info("sent msg : {}:{}",event.getTopic(),event.getTag());
            if(sendResult.getSendStatus() == SendStatus.SEND_OK){
                domainEventRepository.updateSendStatus(event.getEventId(), EventSendStatus.SENT,sendResult.getMsgId());
            }
        };
        afterCommitExecutor.execute(runnable);
    }
}
