package com.metanoia.domain.model;

/**
 * Clasificación de un producto respecto a su destino en el salón.
 */
public enum ProductType {
    USE,
    SALE,
    BOTH;

    public boolean isForUse() {
        return this == USE || this == BOTH;
    }

    public boolean isForSale() {
        return this == SALE || this == BOTH;
    }
}
