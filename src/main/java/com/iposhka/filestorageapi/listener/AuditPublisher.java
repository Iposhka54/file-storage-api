package com.iposhka.filestorageapi.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(String username, String action) {
        publisher.publishEvent(new AuditEvent(username, action));
    }
}