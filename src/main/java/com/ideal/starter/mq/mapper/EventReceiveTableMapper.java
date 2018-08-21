package com.ideal.starter.mq.mapper;

import com.ideal.starter.mq.model.EventReceiveStatus;
import com.ideal.starter.mq.model.EventReceiveTable;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface EventReceiveTableMapper {
    List<EventReceiveTable> getEventTablesBeforeDate(@Param("createTime") Date createTime, @Param("eventStatus") EventReceiveStatus eventStatus, @Param("retryTime") Integer retryTime, @Param("limitCount") int limitCount);

    int save(EventReceiveTable eventReceiveTable);

    EventReceiveTable getEventTableByListener(@Param("listenerName") String listenerName, @Param("messageMode") String messageMode, @Param("consumerGroup") String consumerGroup, @Param("topic") String topic, @Param("tag") String tag, @Param("msgId") String msgId);

    void updateReceiveStatusToProcessed(@Param("id") Integer id, @Param("eventStatus") EventReceiveStatus eventStatus, @Param("processTime") Date processTime, @Param("isRetry") boolean isRetry);

    void addNonProcessEventRetryTime(@Param("id") Integer id, @Param("lastModifyTime") Date lastModifyTime);
}
