package com.metanoia.domain.common;

/**
 * Códigos de error de negocio devueltos por los casos de uso del dominio.
 */
public enum DomainError {
    VALIDATION_ERROR,
    NOT_FOUND,
    CONFLICT,
    DATABASE_ERROR,
    UNEXPECTED
}
