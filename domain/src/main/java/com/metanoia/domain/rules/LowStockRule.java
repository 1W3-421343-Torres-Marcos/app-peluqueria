package com.metanoia.domain.rules;

import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas puras para calcular existencias, detectar stock bajo/negativo y ordenar productos.
 */
public final class LowStockRule {

    private LowStockRule() {
    }

    public static boolean isLowStock(int currentStock, int minStock) {
        return currentStock <= minStock;
    }

    public static boolean isLowStock(Product product) {
        return product != null && isLowStock(product.currentStock(), product.minStock());
    }

    public static boolean isNegativeStock(int currentStock) {
        return currentStock < 0;
    }

    public static boolean isNegativeStock(Product product) {
        return product != null && isNegativeStock(product.currentStock());
    }

    public static int calculateStock(List<StockMovement> movements) {
        if (movements == null || movements.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (StockMovement movement : movements) {
            if (movement != null) {
                total = Math.addExact(total, movement.quantity());
            }
        }
        return total;
    }

    public static int countLowStockActive(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Product product : products) {
            if (product != null && product.active() && isLowStock(product)) {
                count++;
            }
        }
        return count;
    }

    public static List<Product> sortWithLowStockFirst(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<Product> sorted = new ArrayList<>();
        for (Product product : products) {
            if (product != null) {
                sorted.add(product);
            }
        }
        sorted.sort(Comparator
                .comparing((Product p) -> !p.active())
                .thenComparing((Product p) -> !(p.active() && isLowStock(p)))
                .thenComparing(Product::nameNormalized)
                .thenComparing(Product::id));
        return Collections.unmodifiableList(sorted);
    }
}
