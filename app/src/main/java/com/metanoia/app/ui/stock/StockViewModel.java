package com.metanoia.app.ui.stock;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.rules.LowStockRule;
import com.metanoia.domain.rules.TextNormalizer;
import com.metanoia.domain.usecase.SaveProductUseCase.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel para catálogo de stock: filtros, búsqueda, alertas bajas y CRUD de productos.
 */
public class StockViewModel extends AndroidViewModel {

    public enum TypeFilter {
        ALL,
        USE,
        SALE
    }

    private final AppContainer container;
    private final MutableLiveData<TypeFilter> typeFilter = new MutableLiveData<>(TypeFilter.ALL);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> includeInactive = new MutableLiveData<>(Boolean.FALSE);
    private final MutableLiveData<Boolean> onlyLowStock = new MutableLiveData<>(Boolean.FALSE);
    private final LiveData<List<ProductEntity>> rawProducts;
    private final LiveData<Integer> lowStockCount;
    private final MediatorLiveData<List<ProductEntity>> filteredProducts = new MediatorLiveData<>();
    private final MutableLiveData<Event<Integer>> messageEvent = new MutableLiveData<>();

    public StockViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        this.rawProducts = Transformations.switchMap(
                includeInactive,
                include -> container.database().stockDao().observeProducts(Boolean.TRUE.equals(include))
        );
        this.lowStockCount = container.database().stockDao().observeLowStockCount();

        filteredProducts.addSource(rawProducts, ignored -> recompute());
        filteredProducts.addSource(typeFilter, ignored -> recompute());
        filteredProducts.addSource(searchQuery, ignored -> recompute());
        filteredProducts.addSource(onlyLowStock, ignored -> recompute());
    }

    public LiveData<List<ProductEntity>> observeProducts() {
        return filteredProducts;
    }

    public LiveData<Integer> observeLowStockCount() {
        return lowStockCount;
    }

    public LiveData<Boolean> observeOnlyLowStock() {
        return onlyLowStock;
    }

    public LiveData<Event<Integer>> observeMessage() {
        return messageEvent;
    }

    public boolean isFilteringActive() {
        TypeFilter type = typeFilter.getValue();
        String query = searchQuery.getValue();
        return (type != null && type != TypeFilter.ALL)
                || (query != null && !query.trim().isEmpty())
                || Boolean.TRUE.equals(onlyLowStock.getValue());
    }

    public void setTypeFilter(@NonNull TypeFilter filter) {
        if (typeFilter.getValue() != filter) {
            typeFilter.setValue(filter);
        }
    }

    public void setSearchQuery(@Nullable String query) {
        String safe = query == null ? "" : query;
        if (!safe.equals(searchQuery.getValue())) {
            searchQuery.setValue(safe);
        }
    }

    public void setIncludeInactive(boolean include) {
        if (!Boolean.valueOf(include).equals(includeInactive.getValue())) {
            includeInactive.setValue(include);
        }
    }

    public void toggleOnlyLowStock() {
        onlyLowStock.setValue(!Boolean.TRUE.equals(onlyLowStock.getValue()));
    }

    public void clearOnlyLowStock() {
        if (Boolean.TRUE.equals(onlyLowStock.getValue())) {
            onlyLowStock.setValue(Boolean.FALSE);
        }
    }

    public void saveProduct(
            @Nullable String existingId,
            @NonNull String name,
            @Nullable String brand,
            @NonNull ProductType type,
            @NonNull ProductUnit unit,
            @Nullable Money salePrice,
            int minStock,
            int initialStock,
            @NonNull Consumer<Boolean> callback
    ) {
        container.executors().io().execute(() -> {
            Result<Product> result = container.saveProductUseCase().execute(new Request(
                    existingId,
                    name,
                    brand,
                    type,
                    unit,
                    salePrice,
                    minStock,
                    initialStock
            ));
            if (result.isSuccess()) {
                messageEvent.postValue(new Event<>(R.string.product_saved));
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            int msgRes = mapError(result.getError());
            messageEvent.postValue(new Event<>(msgRes));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }

    public void setProductActive(
            @NonNull String productId,
            boolean active,
            @NonNull Consumer<Boolean> callback
    ) {
        container.executors().io().execute(() -> {
            Result<Product> result = container.setProductActiveUseCase().execute(productId, active);
            if (result.isSuccess()) {
                messageEvent.postValue(new Event<>(
                        active ? R.string.product_reactivated : R.string.product_deactivated));
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            messageEvent.postValue(new Event<>(mapError(result.getError())));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }

    private static int mapError(@Nullable DomainError error) {
        if (error == DomainError.NOT_FOUND) {
            return R.string.product_error_not_found;
        }
        if (error == DomainError.VALIDATION_ERROR) {
            return R.string.product_error_validation;
        }
        return R.string.product_error_unexpected;
    }

    private void recompute() {
        List<ProductEntity> source = rawProducts.getValue();
        if (source == null || source.isEmpty()) {
            filteredProducts.setValue(Collections.emptyList());
            return;
        }
        TypeFilter filter = typeFilter.getValue();
        if (filter == null) {
            filter = TypeFilter.ALL;
        }
        String normalizedQuery = TextNormalizer.normalize(searchQuery.getValue());
        boolean filterLowOnly = Boolean.TRUE.equals(onlyLowStock.getValue());

        List<ProductEntity> result = new ArrayList<>();
        for (ProductEntity entity : source) {
            ProductType type = StockUiLabels.parseType(entity.type);
            if (filter == TypeFilter.USE && !type.isForUse()) {
                continue;
            }
            if (filter == TypeFilter.SALE && !type.isForSale()) {
                continue;
            }
            if (filterLowOnly && (!entity.active
                    || !LowStockRule.isLowStock(entity.currentStock, entity.minStock))) {
                continue;
            }
            if (!normalizedQuery.isEmpty()) {
                String brandNorm = entity.brand == null
                        ? ""
                        : TextNormalizer.normalize(entity.brand);
                if (!entity.nameNormalized.contains(normalizedQuery)
                        && !brandNorm.contains(normalizedQuery)) {
                    continue;
                }
            }
            result.add(entity);
        }
        filteredProducts.setValue(result);
    }
}
