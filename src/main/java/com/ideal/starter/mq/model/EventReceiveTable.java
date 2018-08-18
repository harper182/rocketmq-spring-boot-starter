package com.ideal.starter.mq.model;

import java.util.Date;

public class EventReceiveTable extends BaseEventTable{
    private EventReceiveStatus eventStatus;
    private Date receiveTime;

    public EventReceiveStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(EventReceiveStatus eventStatus) {
        this.eventStatus = eventStatus;
    }

    public Date getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(Date receiveTime) {
        this.receiveTime = receiveTime;
    }

}
