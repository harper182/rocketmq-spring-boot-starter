package com.ideal.starter.mq.mapper;

import com.ideal.starter.mq.model.EventSendStatus;
import com.ideal.starter.mq.model.EventSendTable;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface EventSendTableMapper {
    List<EventSendTable> findAll();

    List<EventSendTable> getEventTablesBeforeDate(@Param("sendTime") Date sendTime, @Param("eventStatus") EventSendStatus eventStatus);

    int save(EventSendTable eventReceiveTable);

    EventSendTable getById(@Param("id") Integer id);

    void updateEventTableStatus(@Param("id") Integer id, @Param("eventStatus") EventSendStatus eventStatus, @Param("msgId") String msgId, @Param("sendTime") Date sendTime,
                                @Param("lastModifyTime") Date lastModifyTime, @Param("retryTime") Integer retryTime);

}
