package com.ideal.starter.mq.mapper;

import com.ideal.starter.mq.model.EventReceiveStatus;
import com.ideal.starter.mq.model.EventReceiveTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;
import java.util.List;

@Mapper
public interface EventReceiveTableMapper {
    List<EventReceiveTable> findAll();

    List<EventReceiveTable> getEventTablesBeforeDate(@Param("receiveTime") Date receiveTime, @Param("eventStatus") EventReceiveStatus eventStatus);

    int save(EventReceiveTable eventReceiveTable);

    EventReceiveTable getById(@Param("id") Integer id);

    void updateEventTableStatus(@Param("id") Integer id, @Param("eventStatus") EventReceiveStatus eventStatus, @Param("msgId") String msgId, @Param("receiveTime") Date receiveTime,
                                @Param("lastModifyTime") Date lastModifyTime, @Param("retryTime") Integer retryTime);

}
