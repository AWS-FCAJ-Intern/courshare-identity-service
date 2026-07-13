package com.courshare.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    private String id;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String action;

    protected Permission() {
    }

    public Permission(String id, String resource, String action) {
        this.id = id;
        this.resource = resource;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}
