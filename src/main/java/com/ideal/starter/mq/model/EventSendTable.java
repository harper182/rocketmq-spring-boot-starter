package com.ideal.starter.mq.model;

import java.util.Date;

public class EventSendTable extends BaseEventTable{
    private EventSendStatus eventStatus;
    private Date sendTime;

    public EventSendStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(EventSendStatus eventStatus) {
        this.eventStatus = eventStatus;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

}
