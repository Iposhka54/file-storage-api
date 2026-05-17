package com.iposhka.filestorageapi.listener;

import com.iposhka.filestorageapi.model.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(String username, String action, Action actionType) {
        publisher.publishEvent(new AuditEvent(username, action, actionType));
    }
}