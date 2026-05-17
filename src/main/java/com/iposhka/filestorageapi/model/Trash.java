package com.iposhka.filestorageapi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "trash")
public class Trash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserApp user;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_directory", nullable = false)
    private boolean isDirectory;

    @Column(name = "trash_object_name")
    private String trashObjectName;

    @Column(name = "size")
    private Long size;

    @CreationTimestamp
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;
}
