package com.ideal.starter.mq.config;

import com.ideal.starter.mq.annotation.EnableRocketMQListener;
import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.base.MethodInfo;
import com.ideal.starter.mq.base.RocketMQConsumerContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@EnableConfigurationProperties(MQProperties.class)
public class RocketMQListenerConfiguration implements ApplicationContextAware, InitializingBean {

    private AtomicLong counter = new AtomicLong(0);
    private ConfigurableApplicationContext applicationContext;

    protected MQProperties mqProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(EnableRocketMQListener.class);
        Map<String, Map<String, List<MethodInfo>>> messageModeMap = groupByMessageMode(beans);
        for (Map.Entry<String, Map<String, List<MethodInfo>>> entry : messageModeMap.entrySet()) {
            String messageMode = entry.getKey();
            for (Map.Entry<String, List<MethodInfo>> consumeGroupEntry : entry.getValue().entrySet()) {
                String consumeGroup = consumeGroupEntry.getKey();
                createRocketMQContainer(messageMode, consumeGroup, consumeGroupEntry.getValue());
            }
        }

    }

    private void createRocketMQContainer(String messageMode, String consumerGroup, List<MethodInfo> methodInfolist) {
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(RocketMQConsumerContainer.class);

        beanBuilder.addPropertyValue("consumerGroup", consumerGroup);
        beanBuilder.addPropertyValue("messageMode", messageMode);
        beanBuilder.addPropertyValue("subscribers", methodInfolist);
        beanBuilder.addPropertyValue("mqProperties", mqProperties);
        beanBuilder.setDestroyMethodName("destroy");

        String containerBeanName = String.format("%s_%s", RocketMQConsumerContainer.class.getName(), counter.incrementAndGet());
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());

        RocketMQConsumerContainer container = beanFactory.getBean(containerBeanName, RocketMQConsumerContainer.class);

        try {
            container.start();
        } catch (Exception e) {
            log.error("started rocketmq listener container failed. {}", container, e);
            throw new RuntimeException(e);
        }

        log.info("register rocketMQ listener to container, containerBeanName:{}", containerBeanName);
    }

    //messageMode-methodInfo
    private Map<String, Map<String, List<MethodInfo>>> groupByMessageMode(Map<String, Object> beans) {

        Map<String, Map<String, List<MethodInfo>>> consumerGroupMap = new HashMap<>();
        for (Map.Entry<String, Object> beanEntry : beans.entrySet()) {
            Object bean = beanEntry.getValue();
            List<Method> methods = MethodUtils.getMethodsListWithAnnotation(bean.getClass(), RocketMQConsumerListener.class);
            for (Method method : methods) {
                RocketMQConsumerListener rocketMQConsumerListener = method.getAnnotation(RocketMQConsumerListener.class);
                String consumerGroup = rocketMQConsumerListener.consumerGroup();
                String messageMode = rocketMQConsumerListener.messageMode();
                Map<String, List<MethodInfo>> messageModeMap = consumerGroupMap.get(messageMode);
                if (messageModeMap == null) {
                    messageModeMap = new HashMap<>();
                    consumerGroupMap.put(messageMode, messageModeMap);
                }

                List<MethodInfo> methodInfos = messageModeMap.get(consumerGroup);
                if (methodInfos == null) {
                    methodInfos = new ArrayList<>();
                    messageModeMap.put(consumerGroup, methodInfos);
                }
                methodInfos.add(new MethodInfo(method, bean));
            }
        }
        return consumerGroupMap;
    }

    @Autowired
    public void setMqProperties(MQProperties mqProperties) {
        this.mqProperties = mqProperties;
    }
}

