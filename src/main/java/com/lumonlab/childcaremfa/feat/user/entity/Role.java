package com.lumonlab.childcaremfa.feat.user.entity;


public enum Role {
    PARENT,
    ADMIN,
    MASTER;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}