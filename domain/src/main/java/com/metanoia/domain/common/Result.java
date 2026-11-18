package com.metanoia.domain.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado inmutable de un caso de uso: contiene un valor o un error de dominio.
 *
 * @param <T> tipo del valor en caso de éxito.
 */
public final class Result<T> {

    private final T value;
    private final DomainError error;
    private final String errorMessage;

    private Result(T value, DomainError error, String errorMessage) {
        this.value = value;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    /**
     * Crea un resultado exitoso con un valor no nulo.
     */
    public static <T> Result<T> success(T value) {
        Objects.requireNonNull(value, "value");
        return new Result<>(value, null, "");
    }

    /**
     * Crea un resultado fallido con código de error y detalle técnico opcional.
     */
    public static <T> Result<T> failure(DomainError error, String errorMessage) {
        Objects.requireNonNull(error, "error");
        String safeMessage = errorMessage == null ? "" : errorMessage;
        return new Result<>(null, error, safeMessage);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    /**
     * Devuelve el valor de un resultado exitoso; lanza {@link IllegalStateException} si falló.
     */
    public T getValue() {
        if (!isSuccess()) {
            throw new IllegalStateException("No hay valor porque el resultado falló: " + error);
        }
        return value;
    }

    public Optional<T> valueOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * Devuelve el código de error de un resultado fallido; lanza {@link IllegalStateException} si tuvo éxito.
     */
    public DomainError getError() {
        if (isSuccess()) {
            throw new IllegalStateException("No hay error porque el resultado fue exitoso");
        }
        return error;
    }

    public Optional<DomainError> errorOptional() {
        return Optional.ofNullable(error);
    }

    /**
     * Devuelve el mensaje técnico asociado al fallo (vacío si no se especificó).
     */
    public String getErrorMessage() {
        if (isSuccess()) {
            throw new IllegalStateException("No hay mensaje de error porque el resultado fue exitoso");
        }
        return errorMessage;
    }
}
