package com.iposhka.filestorageapi.repository;

import com.iposhka.filestorageapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
