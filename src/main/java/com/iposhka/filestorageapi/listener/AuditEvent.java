package com.iposhka.filestorageapi.listener;

import com.iposhka.filestorageapi.model.Action;

public record AuditEvent(String username, String action, Action actionType) {

}