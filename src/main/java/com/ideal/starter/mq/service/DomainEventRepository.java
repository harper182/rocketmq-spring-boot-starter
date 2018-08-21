package com.ideal.starter.mq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.mapper.EventReceiveTableMapper;
import com.ideal.starter.mq.mapper.EventSendTableMapper;
import com.ideal.starter.mq.model.*;
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

    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus, String msgId) {
        updateSendStatus(eventId, eventStatus, msgId, null);
    }

    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus, String msgId, Integer retryTime) {
        eventSendTableMapper.updateEventTableStatus(eventId, eventStatus, msgId, new Date(), new Date(), retryTime);
    }

    public void updateReceiveStatusToProcessed(Integer id, EventReceiveStatus eventStatus, Date processTime) {
        this.updateReceiveStatusToProcessed(id, eventStatus, processTime, false);
    }

    public void updateReceiveStatusToProcessed(Integer id, EventReceiveStatus eventStatus, Date processTime, boolean isRetry) {
        eventReceiveTableMapper.updateReceiveStatusToProcessed(id, eventStatus, processTime, isRetry);
    }

    public void addNonProcessEventRetryTime(Integer id, Date lastModifyTime) {
        eventReceiveTableMapper.addNonProcessEventRetryTime(id, lastModifyTime);
    }

    public void addSendWaitingEventRetryTime(Integer id, Date lastModifyTime) {
        eventSendTableMapper.addSendWaitingEventRetryTime(id, lastModifyTime);
    }

    public EventReceiveTable getEventTableByListener(String listenerName, String messageMode, String consumerGroup, String topic, String tag, String msgId) {
        return eventReceiveTableMapper.getEventTableByListener(listenerName, messageMode, consumerGroup, topic, tag, msgId);
    }

    public boolean saveNeedToSendEvents(List<DomainEvent> domainEvents) {
        domainEvents.stream().forEach(domainEvent -> {
            EventSendTable eventReceiveTable = this.saveNeedToSendEvent(domainEvent);
            domainEvent.setEventId(eventReceiveTable.getId());
        });
        return true;
    }

    public EventReceiveTable saveNeedToProcessEvents(DomainEvent domainEvent, RocketMQConsumerListener consumerListener, String method) {
        EventReceiveTable eventReceiveTable = this.saveNeedToProcessEvent(domainEvent, consumerListener, method);
        domainEvent.setEventId(eventReceiveTable.getId());
        return eventReceiveTable;
    }

    public List<EventSendTable> getNeedToSendDomainEventList(Date beforeDate, EventSendStatus eventStatus, int retryTime, int limitCount) {
        return eventSendTableMapper.getEventTablesBeforeDate(beforeDate, eventStatus, retryTime, limitCount);
    }

    public List<EventReceiveTable> getNeedToProcessDomainEventList(Date beforeDate, EventReceiveStatus eventStatus, int retryTime, int limitCount) {
        return eventReceiveTableMapper.getEventTablesBeforeDate(beforeDate, eventStatus, retryTime, limitCount);
    }

    private EventSendTable saveNeedToSendEvent(DomainEvent domainEvent) {
        EventSendTable eventSendTable = new EventSendTable();
        updateTableInfo(domainEvent, eventSendTable);
        eventSendTable.setEventStatus(EventSendStatus.SEND_WAITING);
        eventSendTableMapper.save(eventSendTable);
        return eventSendTable;
    }

    private EventReceiveTable saveNeedToProcessEvent(DomainEvent domainEvent, RocketMQConsumerListener consumerListener, String method) {
        EventReceiveTable eventReceiveTable = new EventReceiveTable();
        updateTableInfo(domainEvent, eventReceiveTable);
        eventReceiveTable.setEventStatus(EventReceiveStatus.NON_PROCESSED);
        eventReceiveTable.setConsumerGroup(consumerListener.consumerGroup());
        eventReceiveTable.setMessageMode(consumerListener.messageMode());
        eventReceiveTable.setListenerName(method);
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
