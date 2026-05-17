package com.iposhka.filestorageapi.repository;

import com.iposhka.filestorageapi.model.Trash;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrashRepository extends JpaRepository<Trash, Long> {

    List<Trash> findAllByUserId(Long userId);

    Optional<Trash> findByPathAndUserId(String path, Long userId);
}
