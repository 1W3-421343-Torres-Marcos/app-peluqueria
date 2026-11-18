package com.metanoia.domain.common;

import java.util.UUID;

/**
 * Generador de identificadores únicos para entidades creadas en el dominio.
 */
public interface IdGenerator {

    String nextId();

    /**
     * Implementación basada en UUID aleatorio v4.
     */
    static IdGenerator uuid() {
        return UuidGenerator.INSTANCE;
    }

    final class UuidGenerator implements IdGenerator {

        private static final UuidGenerator INSTANCE = new UuidGenerator();

        private UuidGenerator() {
        }

        @Override
        public String nextId() {
            return UUID.randomUUID().toString();
        }
    }
}
