package com.iposhka.filestorageapi.listener;

import com.iposhka.filestorageapi.model.UserActionAudit;
import com.iposhka.filestorageapi.repository.UserActionAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final UserActionAuditRepository userActionAuditRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditEvent event) {
        Integer actionTypeOrdinal = event.actionType() != null ? event.actionType().ordinal() : null;

        UserActionAudit log = UserActionAudit.builder()
                .username(event.username())
                .action(event.action())
                .actionType(actionTypeOrdinal)
                .build();
        userActionAuditRepository.save(log);
    }
}
