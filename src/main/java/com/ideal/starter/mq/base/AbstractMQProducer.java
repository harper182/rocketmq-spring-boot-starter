package com.ideal.starter.mq.base;

import com.ideal.starter.mq.MQException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RocketMQ的生产者的抽象基类
 */
@Slf4j
public abstract class AbstractMQProducer {

    private static MessageQueueSelector messageQueueSelector = new SelectMessageQueueByHash();

    public AbstractMQProducer() {
    }

    @Autowired
    private DefaultMQProducer producer;

    /**
     * 同步发送消息
     * @param message  消息体
     * @throws MQException 消息异常
     */
    public SendResult syncSend(Message message) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        return producer.send(message);
    }


    /**
     * 同步发送消息
     * @param message  消息体
     * @param hashKey  用于hash后选择queue的key
     * @throws MQException 消息异常
     */
    public SendResult syncSendOrderly(Message message, String hashKey) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if(StringUtils.isEmpty(hashKey)) {
            // fall back to normal
            return syncSend(message);
        }else {
            throw new MQException("hashKey is empty");
        }

    }

    /**
     * 异步发送消息
     * @param message msgObj
     * @param sendCallback 回调
     * @throws MQException 消息异常
     */
    public void asyncSend(Message message, SendCallback sendCallback) throws RemotingException, MQClientException, InterruptedException {
        producer.send(message, sendCallback);
    }

    public SendResult send(Message msg, MessageQueue mq) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return producer.send(msg, mq);
    }

    public SendResult send(Message msg, MessageQueue mq, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return producer.send(msg, mq, timeout);
    }

    public void send(Message msg, MessageQueue mq, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        producer.send(msg, mq, sendCallback);
    }

    public void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout) throws MQClientException, RemotingException, InterruptedException {
        producer.send(msg, mq, sendCallback, timeout);
    }

    public void sendOneway(Message msg, MessageQueue mq) throws MQClientException, RemotingException, InterruptedException {
        producer.sendOneway(msg, mq);
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return producer.send(msg, selector, arg);
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return producer.send(msg, selector, arg, timeout);
    }

    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback) throws MQClientException, RemotingException, InterruptedException {
        producer.send(msg, selector, arg, sendCallback);
    }

    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback, long timeout) throws MQClientException, RemotingException, InterruptedException {
        producer.send(msg, selector, arg, sendCallback, timeout);
    }

    public void sendOneway(Message msg, MessageQueueSelector selector, Object arg) throws MQClientException, RemotingException, InterruptedException {
        producer.sendOneway(msg, selector, arg);
    }

    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecuter tranExecuter, Object arg) throws MQClientException {
        throw new RuntimeException("sendMessageInTransaction not implement, please use TransactionMQProducer class");
    }
}
