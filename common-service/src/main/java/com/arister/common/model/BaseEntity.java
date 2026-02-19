package com.arister.common.model;

import jakarta.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 0L;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = ZonedDateTime.now(ZoneId.of("UTC"));
        updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    }
}
