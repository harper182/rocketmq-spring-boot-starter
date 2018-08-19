package com.ideal.starter.mq.model;

import java.util.Date;

public class BaseEventTable {
    private Integer id;
    private String topic;
    private String tag;
    private Date createTime;
    private Date lastModifyTime;
    private Integer retryTime = 0;
    private Integer modifyNumber = 0;
    private String message;
    private String msgId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }


    public Date getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public Integer getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(Integer retryTime) {
        this.retryTime = retryTime;
    }

    public Integer getModifyNumber() {
        return modifyNumber;
    }

    public void setModifyNumber(Integer modifyNumber) {
        this.modifyNumber = modifyNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
