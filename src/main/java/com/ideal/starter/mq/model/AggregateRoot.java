package com.ideal.starter.mq.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AggregateRoot {
    private List<DomainEvent> events = new ArrayList<>();

    public void addEvent(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
