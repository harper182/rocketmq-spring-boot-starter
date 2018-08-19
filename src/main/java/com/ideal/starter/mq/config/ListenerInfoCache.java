package com.ideal.starter.mq.config;

import com.ideal.starter.mq.annotation.RocketMQConsumerListener;
import com.ideal.starter.mq.base.MethodInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ListenerInfoCache {
    private Map<String, Map<String, List<MethodInfo>>> infoCaches = new HashMap<>();

    public Map<String, Map<String, List<MethodInfo>>> getInfoCaches() {
        return infoCaches;
    }

    public void setInfoCaches(Map<String, Map<String, List<MethodInfo>>> infoCaches) {
        this.infoCaches = infoCaches;
    }

    public List<MethodInfo> getMethodInfoByListenerInfo(String consumerGroup, String messageMode, String topic, String tag) {
        Map<String, List<MethodInfo>> messageModeMap = infoCaches.get(messageMode);
        if (!CollectionUtils.isEmpty(messageModeMap)) {
            List<MethodInfo> consumerGroupMap = messageModeMap.get(consumerGroup);
            if(!CollectionUtils.isEmpty(consumerGroupMap)){
                return consumerGroupMap.stream().filter(methodInfo -> {
                    RocketMQConsumerListener annotation = methodInfo.getMethod().getAnnotation(RocketMQConsumerListener.class);
                    return topic.equals(annotation.topic()) && tag.equals(annotation.tag());
                }).collect(Collectors.toList());
            }
        }
        return null;
    }
}
