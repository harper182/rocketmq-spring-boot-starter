package com.ideal.starter.mq.model;

import java.util.Date;

public class EventReceiveTable extends BaseEventTable{
    private EventReceiveStatus eventStatus;
    private String messageMode;
    private String consumerGroup;
    private String listenerName;
    private Date processTime;

    public EventReceiveStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(EventReceiveStatus eventStatus) {
        this.eventStatus = eventStatus;
    }

    public Date getProcessTime() {
        return processTime;
    }

    public void setProcessTime(Date processTime) {
        this.processTime = processTime;
    }

    public String getMessageMode() {
        return messageMode;
    }

    public void setMessageMode(String messageMode) {
        this.messageMode = messageMode;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getListenerName() {
        return listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }
}
