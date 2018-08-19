package com.ideal.starter.mq.config;

import com.ideal.starter.mq.annotation.EnableRocketMQListener;
import com.ideal.starter.mq.annotation.MQProducer;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.base.AbstractMQProducer;
import com.ideal.starter.mq.base.RocketMQConsumerContainer;
import com.ideal.starter.mq.base.SimpleConsumer;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertEquals;

public class RocketMQConsumerListenerAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    private void prepareApplicationContext() {
        this.context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(this.context, "spring.rocketmq.namesrv-addr:127.0.0.1:9876");
//        EnvironmentTestUtils.addEnvironment(this.context, "mapper-locations:classpath*:mapper/*.xml");
        this.context.register(TestConsumer.class);
        this.context.register(RocketMQListenerConfiguration.class,MQBaseAutoConfiguration.class, MQProducerAutoConfiguration.class, DefaultSqlSessionFactory.class,PooledDataSource.class, DefaultSqlSessionFactory.class,Configuration.class);
        this.context.refresh();
    }


    private void prepareApplicationContextMissingNS() {
        this.context = new AnnotationConfigApplicationContext();
        this.context.register(TestConsumer.class);
        this.context.register(RocketMQListenerConfiguration.class);
        this.context.refresh();
    }

    @After
    public void close() {
        this.context.close();
    }

    @Test
    public void testConsumerConfiguration() throws Exception {
        prepareApplicationContext();
        String containerBeanName = String.format("%s_%s", RocketMQConsumerContainer.class.getName(), 1);
        RocketMQConsumerContainer mqConsumerContainer = context.getBean(containerBeanName, RocketMQConsumerContainer.class);
        DefaultMQPushConsumer consumer = mqConsumerContainer.getConsumer().getConsumer();
        assertEquals(consumer.getNamesrvAddr(), "127.0.0.1:9876");

    }

    @Component
    @EnableRocketMQListener
    static class TestConsumer {

        @RocketMQConsumerListener(topic = "test_topic", consumerGroup = "test_consumer_group", tag = "test_tag")
        public boolean process(Object object) {
            return true;
        }
    }

}
