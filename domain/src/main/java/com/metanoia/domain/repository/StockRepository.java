package com.metanoia.domain.repository;

import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar catálogo de productos y movimientos inmutables de stock.
 */
public interface StockRepository {

    Optional<Product> findProductById(String id);

    List<Product> findAllProducts(boolean includeInactive);

    List<Product> findActiveSaleProducts();

    List<StockMovement> findMovementsByProductId(String productId);

    int sumMovementsByProductId(String productId);

    void saveProduct(Product product);

    void saveMovement(StockMovement movement);

    void recordMovementAndUpdateProduct(StockMovement movement, Product updatedProduct);

    void runInTransaction(Runnable action);
}
