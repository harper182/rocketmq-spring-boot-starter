package com.ideal.starter.mq.service;


import com.ideal.starter.mq.model.*;

import java.util.Date;
import java.util.List;


public interface DomainEventRepository {

    void updateSendStatus(Integer eventId, EventSendStatus eventStatus);

    void updateReceiveStatus(Integer eventId, EventReceiveStatus eventStatus);

    void updateSendStatus(Integer eventId, EventSendStatus eventStatus, Integer retryTimess);

    void updateReceiveStatus(Integer eventId, EventReceiveStatus eventStatus, Integer retryTimess);

    boolean saveNeedToSendEvents(List<DomainEvent> domainEvents);

    boolean saveNeedToProcessEvents(List<DomainEvent> domainEvents);

    List<EventSendTable> getNeedToSendDomainEventList(Date beforeDate, EventSendStatus eventStatus);

    List<EventReceiveTable> getNeedToProcessDomainEventList(Date beforeDate, EventReceiveStatus eventStatus);

}
