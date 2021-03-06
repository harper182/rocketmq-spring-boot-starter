package com.ideal.starter.mq.model;

public class DomainEvent {
    private String topic;
    private String tag;
    private String msgId;
    private Integer eventId;


    public DomainEvent(String topic, String tag) {
        this.topic = topic;
        this.tag = tag;
    }

    public DomainEvent() {
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }
}
