package com.metanoia.domain.model;

import java.util.Optional;

/**
 * Cumpleaños validado extraído de un contacto, con año opcional.
 */
public final class ContactBirthday {

    private final int day;
    private final int month;
    private final Integer year;

    public ContactBirthday(int day, int month, Integer year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public int day() {
        return day;
    }

    public int month() {
        return month;
    }

    public Optional<Integer> year() {
        return Optional.ofNullable(year);
    }
}
