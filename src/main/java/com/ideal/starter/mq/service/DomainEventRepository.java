package com.ideal.starter.mq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.mapper.EventReceiveTableMapper;
import com.ideal.starter.mq.mapper.EventSendTableMapper;
import com.ideal.starter.mq.model.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@Qualifier("domainEventRepository")
public class DomainEventRepository {
    @Autowired
    private EventSendTableMapper eventSendTableMapper;
    @Autowired
    private EventReceiveTableMapper eventReceiveTableMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus) {
        updateSendStatus(eventId, eventStatus, null);
    }

    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus, Integer retryTimess) {
        eventSendTableMapper.updateEventTableStatus(eventId, eventStatus, null, new Date(), new Date(), retryTimess);
    }

    public void updateReceiveStatusToProcessed(String listenerName ,String messageMode,String consumerGroup,String topic, String tag, String msgId){
        eventReceiveTableMapper.updateReceiveStatusToProcessed(listenerName ,messageMode, consumerGroup, topic, tag, msgId,EventReceiveStatus.PROCESSED,new Date());
    }

    public void updateReceiveStatus(String msgId, EventReceiveStatus eventStatus, Integer retryTimess) {
        eventReceiveTableMapper.updateEventTableStatus(msgId, eventStatus, new Date(), new Date(), retryTimess);
    }

    public EventReceiveTable getEventReceiveTableByMsgId(String msgId) {
        return eventReceiveTableMapper.getByMsgId(msgId);
    }
    public EventReceiveTable getEventTableByListener(String listenerName ,String messageMode,String consumerGroup,String topic, String tag, String msgId){
        return eventReceiveTableMapper.getEventTableByListener(listenerName ,messageMode, consumerGroup, topic, tag, msgId);
    }
    public boolean saveNeedToSendEvents(List<DomainEvent> domainEvents) {
        domainEvents.stream().forEach(domainEvent -> {
            EventSendTable eventReceiveTable = this.saveNeedToSendEvent(domainEvent);
            domainEvent.setEventId(eventReceiveTable.getId());
        });
        return true;
    }

    public boolean saveNeedToProcessEvents(List<DomainEvent> domainEvents, RocketMQConsumerListener consumerListener) {
        domainEvents.stream().forEach(domainEvent -> {
            EventReceiveTable eventReceiveTable = this.saveNeedToProcessEvent(domainEvent,consumerListener);
            domainEvent.setEventId(eventReceiveTable.getId());
        });
        return true;
    }

    public List<EventSendTable> getNeedToSendDomainEventList(Date beforeDate, EventSendStatus eventStatus) {
        return eventSendTableMapper.getEventTablesBeforeDate(beforeDate, eventStatus);
    }

    public List<EventReceiveTable> getNeedToProcessDomainEventList(Date beforeDate, EventReceiveStatus eventStatus) {
        return eventReceiveTableMapper.getEventTablesBeforeDate(beforeDate, eventStatus);
    }

    private EventSendTable saveNeedToSendEvent(DomainEvent domainEvent) {
        EventSendTable eventSendTable = new EventSendTable();
        updateTableInfo(domainEvent, eventSendTable);
        eventSendTable.setEventStatus(EventSendStatus.SEND_WAITING);
        eventSendTableMapper.save(eventSendTable);
        return eventSendTable;
    }

    private EventReceiveTable saveNeedToProcessEvent(DomainEvent domainEvent,RocketMQConsumerListener consumerListener) {
        EventReceiveTable eventReceiveTable = new EventReceiveTable();
        updateTableInfo(domainEvent, eventReceiveTable);
        eventReceiveTable.setEventStatus(EventReceiveStatus.NON_PROCESSED);
        eventReceiveTable.setConsumerGroup(consumerListener.consumerGroup());
        eventReceiveTable.setMessageMode(consumerListener.messageMode());
        eventReceiveTable.setListenerName(consumerListener.name());
        eventReceiveTableMapper.save(eventReceiveTable);
        return eventReceiveTable;
    }

    private void updateTableInfo(DomainEvent domainEvent, BaseEventTable eventTable) {
        eventTable.setTopic(domainEvent.getTopic());
        eventTable.setTag(domainEvent.getTag());
        eventTable.setLastModifyTime(new Date());
        eventTable.setCreateTime(new Date());
        eventTable.setMsgId(domainEvent.getMsgId());
        try {
            eventTable.setMessage(objectMapper.writeValueAsString(domainEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("convert domain event to string failed");
        }
    }

    public EventSendTableMapper getEventSendTableMapper() {
        return eventSendTableMapper;
    }

    public void setEventSendTableMapper(EventSendTableMapper eventSendTableMapper) {
        this.eventSendTableMapper = eventSendTableMapper;
    }

    public EventReceiveTableMapper getEventReceiveTableMapper() {
        return eventReceiveTableMapper;
    }

    public void setEventReceiveTableMapper(EventReceiveTableMapper eventReceiveTableMapper) {
        this.eventReceiveTableMapper = eventReceiveTableMapper;
    }
}
