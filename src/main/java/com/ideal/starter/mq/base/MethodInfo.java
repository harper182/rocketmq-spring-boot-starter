package com.ideal.starter.mq.base;

import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import org.apache.rocketmq.common.message.MessageExt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInfo {
    private Method method;
    private Object bean;
    public MethodInfo(){

    }

    public void invoke(Object object,MessageExt messageExt) throws InvocationTargetException, IllegalAccessException {
        method.invoke(bean,new Object[]{object, messageExt});
    }
    public MethodInfo(Method method, Object bean) {
        this.method = method;
        this.bean = bean;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }
}
