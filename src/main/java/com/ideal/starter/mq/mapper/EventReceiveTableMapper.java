package com.ideal.starter.mq.mapper;

import com.ideal.starter.mq.model.EventReceiveStatus;
import com.ideal.starter.mq.model.EventReceiveTable;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface EventReceiveTableMapper {
    List<EventReceiveTable> findAll();

    List<EventReceiveTable> getEventTablesBeforeDate(@Param("receiveTime") Date receiveTime, @Param("eventStatus") EventReceiveStatus eventStatus);

    int save(EventReceiveTable eventReceiveTable);

    EventReceiveTable getByMsgId(@Param("msgId") String msgId);

    void updateEventTableStatus(@Param("msgId") String msgId, @Param("eventStatus") EventReceiveStatus eventStatus, @Param("receiveTime") Date receiveTime,
                                @Param("lastModifyTime") Date lastModifyTime, @Param("retryTime") Integer retryTime);

}
