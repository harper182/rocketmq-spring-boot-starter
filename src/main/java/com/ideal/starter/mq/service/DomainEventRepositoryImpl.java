package com.ideal.starter.mq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class DomainEventRepositoryImpl implements DomainEventRepository {
    @Autowired
    private EventSendTableMapper eventSendTableMapper;
    @Autowired
    private EventReceiveTableMapper eventReceiveTableMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus) {
        updateSendStatus(eventId, eventStatus, null);
    }

    @Override
    public void updateSendStatus(Integer eventId, EventSendStatus eventStatus, Integer retryTimess) {
        eventSendTableMapper.updateEventTableStatus(eventId, eventStatus, null, new Date(), new Date(), retryTimess);
    }

    @Override
    public void updateReceiveStatus(Integer eventId, EventReceiveStatus eventStatus) {
        this.updateReceiveStatus(eventId, eventStatus, null);
    }

    @Override
    public void updateReceiveStatus(Integer eventId, EventReceiveStatus eventStatus, Integer retryTimess) {
        eventReceiveTableMapper.updateEventTableStatus(eventId, eventStatus, null, new Date(), new Date(), retryTimess);
    }

    @Override
    public boolean saveNeedToSendEvents(List<DomainEvent> domainEvents) {
        domainEvents.stream().forEach(domainEvent -> {
            EventSendTable eventReceiveTable = this.saveNeedToSendEvent(domainEvent);
            domainEvent.setEventId(eventReceiveTable.getId());
        });
        return true;
    }

    @Override
    public boolean saveNeedToProcessEvents(List<DomainEvent> domainEvents) {
        domainEvents.stream().forEach(domainEvent -> {
            EventReceiveTable eventReceiveTable = this.saveNeedToProcessEvent(domainEvent);
            domainEvent.setEventId(eventReceiveTable.getId());
        });
        return true;
    }

    @Override
    public List<EventSendTable> getNeedToSendDomainEventList(Date beforeDate, EventSendStatus eventStatus) {
        return eventSendTableMapper.getEventTablesBeforeDate(beforeDate, eventStatus);
    }

    @Override
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

    private EventReceiveTable saveNeedToProcessEvent(DomainEvent domainEvent) {
        EventReceiveTable eventReceiveTable = new EventReceiveTable();
        updateTableInfo(domainEvent, eventReceiveTable);
        eventReceiveTable.setEventStatus(EventReceiveStatus.NON_PROCESSED);
        eventReceiveTableMapper.save(eventReceiveTable);
        return eventReceiveTable;
    }

    private void updateTableInfo(DomainEvent domainEvent, BaseEventTable eventTable) {
        eventTable.setTopic(domainEvent.getTopic());
        eventTable.setTag(domainEvent.getTag());
        eventTable.setLastModifyTime(new Date());
        try {
            eventTable.setMessage(objectMapper.writeValueAsString(domainEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("convert domain event to string failed");
        }
    }
}
