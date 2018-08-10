package com.ideal.starter.mq.config;

import lombok.Data;
import org.apache.rocketmq.client.ClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ的配置参数
 */
@Data
@ConfigurationProperties(prefix = "spring.rocketmq")
public class MQProperties extends ClientConfig{

    /**
     * config producer group , default to DPG+RANDOM UUID like DPG-fads-3143-123d-1111
     */
    private String producerGroup;
    /**
     * config send message timeout
     */
    private Integer sendMsgTimeout = 3000;
    /**
     * switch of trace message consumer: send message consumer info to topic: rmq_sys_TRACE_DATA
     */
    private Boolean traceEnabled = Boolean.TRUE;

    private int maxReconsumeTimes = 16;

}
