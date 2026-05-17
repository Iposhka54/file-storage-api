package com.iposhka.filestorageapi.repository;

import com.iposhka.filestorageapi.model.UserActionAudit;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionAuditRepository extends JpaRepository<UserActionAudit, UUID> {

    Page<UserActionAudit> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<UserActionAudit> findByActionType(Integer actionType, Pageable pageable);

    Page<UserActionAudit> findByUsernameContainingIgnoreCaseAndActionType(String username, Integer actionType,
            Pageable pageable);
}
