package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // CREATE, UPDATE, DELETE, APPROVE, REJECT, LOCK, UNLOCK
    private String entityName; // User, Topic, Registration...
    private String entityId; // ID of the entity
    private String actorUsername; // Who did it
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON or description

    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String action, String entityName, String entityId, String actorUsername, String details) {
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.actorUsername = actorUsername;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
