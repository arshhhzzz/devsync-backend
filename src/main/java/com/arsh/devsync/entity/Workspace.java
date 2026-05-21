package com.arsh.devsync.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
@SQLDelete(sql = "UPDATE workspaces SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Workspace() {}

    public Workspace(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
