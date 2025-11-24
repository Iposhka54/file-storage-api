package com.iposhka.filestorageapi.repository;

import com.iposhka.filestorageapi.model.UserActionAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionAuditRepository extends JpaRepository<UserActionAudit, UUID> {

}
