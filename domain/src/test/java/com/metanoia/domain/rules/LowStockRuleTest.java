package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LowStockRuleTest {

    @Test
    public void lowStockRule_triggersAtOrBelowMinimumAndFlagsNegative() {
        assertTrue(LowStockRule.isLowStock(0, 0));
        assertTrue(LowStockRule.isLowStock(2, 2));
        assertTrue(LowStockRule.isLowStock(1, 2));
        assertTrue(LowStockRule.isLowStock(-3, 2));
        assertFalse(LowStockRule.isLowStock(3, 2));

        assertFalse(LowStockRule.isNegativeStock(0));
        assertTrue(LowStockRule.isNegativeStock(-1));
    }

    @Test
    public void calculateStock_sumsSignedMovementsAccurately() {
        List<StockMovement> movements = Arrays.asList(
                movement("m-1", StockMovementType.IN, 10, null),
                movement("m-2", StockMovementType.OUT_USE, -3, null),
                movement("m-3", StockMovementType.OUT_SALE, -2, null),
                movement("m-4", StockMovementType.ADJUSTMENT, -6, "Rotura de envase")
        );

        assertEquals(-1, LowStockRule.calculateStock(movements));
        assertTrue(LowStockRule.isLowStock(LowStockRule.calculateStock(movements), 2));
        assertTrue(LowStockRule.isNegativeStock(LowStockRule.calculateStock(movements)));
    }

    @Test
    public void countLowStockActiveAndSorting_ignoreInactiveAndPutLowStockFirst() {
        Product activeNormal = product("p-a", "Acondicionador", 8, 2, true);
        Product activeLowEqual = product("p-b", "Botox Capilar", 2, 2, true);
        Product activeLowNegative = product("p-c", "Ampolla", -1, 1, true);
        Product inactiveLow = product("p-d", "Decolorante", 0, 5, false);

        List<Product> input = Arrays.asList(activeNormal, inactiveLow, activeLowEqual, activeLowNegative);

        assertEquals(2, LowStockRule.countLowStockActive(input));

        List<Product> sorted = LowStockRule.sortWithLowStockFirst(input);
        assertEquals(Arrays.asList("p-c", "p-b", "p-a", "p-d"),
                Arrays.asList(
                        sorted.get(0).id(),
                        sorted.get(1).id(),
                        sorted.get(2).id(),
                        sorted.get(3).id()));
    }

    private static Product product(String id, String name, int stock, int min, boolean active) {
        return new Product.Builder()
                .id(id)
                .name(name)
                .type(ProductType.BOTH)
                .unit(ProductUnit.UNIT)
                .salePrice(Money.ofCents(1_000_000L))
                .minStock(min)
                .currentStock(stock)
                .active(active)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static StockMovement movement(
            String id,
            StockMovementType type,
            int quantity,
            String reason
    ) {
        return new StockMovement.Builder()
                .id(id)
                .productId("p-1")
                .timestamp(2_000L)
                .type(type)
                .quantity(quantity)
                .reason(reason)
                .operator("Dueño")
                .build();
    }
}
