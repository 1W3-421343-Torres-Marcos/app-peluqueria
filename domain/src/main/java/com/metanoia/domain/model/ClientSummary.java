package com.metanoia.domain.model;

import java.util.Objects;

/**
 * Proyección liviana de cliente para listados y búsqueda rápida.
 */
public final class ClientSummary {

    private final String id;
    private final String name;
    private final String phone;
    private final int noShowCount;
    private final boolean active;

    public ClientSummary(String id, String name, String phone, int noShowCount, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.noShowCount = noShowCount;
        this.active = active;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }

    public int noShowCount() {
        return noShowCount;
    }

    public boolean isActive() {
        return active;
    }
}
