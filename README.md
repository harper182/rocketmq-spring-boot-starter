# spring boot starter for RocketMQ [![Build Status](https://travis-ci.org/maihaoche/rocketmq-spring-boot-starter.svg?branch=master)](https://travis-ci.org/maihaoche/rocketmq-spring-boot-starter) [![Coverage Status](https://coveralls.io/repos/github/maihaoche/rocketmq-spring-boot-starter/badge.svg?branch=master)](https://coveralls.io/github/maihaoche/rocketmq-spring-boot-starter?branch=master)


### 项目介绍

[Rocketmq](https://github.com/apache/rocketmq) 是由阿里巴巴团队开发并捐赠给apache团队的优秀消息中间件，承受过历年双十一大促的考验。

你可以通过本项目轻松的集成Rocketmq到你的SpringBoot项目中。
本项目主要包含以下特性

* [x] 同步发送消息
* [x] 异步发送消息
* [x] 广播发送消息
* [x] 有序发送和消费消息
* [x] 发送延时消息
* [x] 消息tag和key支持
* [x] 自动序列化和反序列化消息体
* [x] 消息补偿机制



### 简单入门实例


##### 1. 添加maven依赖：

```java
<dependency>
    <groupId>com.ideal</groupId>
    <artifactId>spring-boot-starter-rocketmq</artifactId>
    <version>0.0.7</version>
</dependency>
```

##### 2. 添加配置：

```java
spring:
    rocketmq:
      namesrv-addr: 172.21.10.111:9876
      # 可选, 如果无需发送消息则忽略该配置
      producer-group: local_pufang_producer
      # 发送超时配置毫秒数, 可选, 默认3000
      send-msg-timeout: 5000
      # 追溯消息具体消费情况的开关，默认打开
      #trace-enabled: false
      # 是否启用VIP通道，默认打开
      #vip-channel-enabled: false
      #max-reconsume-times:
      #instanceName
      #poll-name-server-interval:
      #heartbeat-broker-interval:
      #persist-consumer-offset-interval:
      //补偿未发送消息前置事件
      #compensate-send-time: 10
      //补偿未处理消息前置事件
      #compensate-receive-time : 10
      //补偿消息时重试次数
      #message-retry-max-time: 4
      //补偿未发送消息一次处理条数
      #compensate-send-limit: 100
      //补偿未处理消息一次处理条数
      #compensate-receive-limit: 100
```
##### 3. 程序入口添加注解开启自动装配

在springboot应用主入口添加`@EnableMQConfiguration`注解开启自动装配：

```java
@SpringBootApplication
@EnableMQConfiguration
@MapperScan(basePackages = {"com.harper.rocketmq.mapper","com.ideal.starter.mq.mapper"})
//com.harper.rocketmq.mapper为自己服务中mapper所在文件夹 
public class SpringRocketMqApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringRocketMqApplication.class, args);
    }
}
```

##### 4. 构建消息体

```java
public class OrderPaidEvent extends DomainEvent {
    private String orderId;
    public OrderPaidEvent(String topic, String tag, String orderId) {
        super(topic, tag);
        this.orderId = orderId;
    }
}
```
##### 5 引入发送方（自主发送）
```java
 @Autowired
 private CommonProducer commonProducer;

 //同步发送
 String topic,tag;
 SendResult sendResult = commonProducer.syncSend( topic+ ":" + tag, message);
 
 //异步发送
 commonProducer.asyncSend(String destination, Object payload, SendCallback sendCallback);
```

##### 6. 创建消费方

```java
@EnableRocketMQListener
public class OrderConsumer {

    @Autowired
    private DomainEventRepository domainEventRepository;
    @Autowired
    private AsyncBusinessService businessService;

    @RocketMQConsumerListener(name = "process",topic = "ORDER", consumerGroup = "local_sucloger_dev", tag = "ORDERPAID", messageType = OrderPaidEvent.class)
    public DomainEvent process(Object object) {
        OrderPaidEvent paidEvent = (OrderPaidEvent) object;
        System.out.println("-----------------in process");
        return paidEvent;
    }
}
```

##### 7. 保存事件并发送消息：

```java
// 注入事件分发器
@Autowired
private DomainEventDispatcherService domainEventDispatcherService;
    
...
    
// 保存事件并在事务成功后发送
domainEventDispatcherService.saveAndDispatcher(orderDomain.getEvents());
    
```




