package com.metanoia.app.ui.stock.movements;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.data.db.entity.StockMovementEntity;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.usecase.RecordStockMovementUseCase.Outcome;
import com.metanoia.domain.usecase.RecordStockMovementUseCase.Request;

import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel para ficha de producto e historial inmutable de movimientos de stock.
 */
public class ProductDetailViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<String> productId = new MutableLiveData<>();
    private final LiveData<ProductEntity> product;
    private final LiveData<List<StockMovementEntity>> movements;
    private final MutableLiveData<Event<Integer>> messageEvent = new MutableLiveData<>();

    public ProductDetailViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        this.product = Transformations.switchMap(
                productId, id -> container.database().stockDao().observeProductById(id));
        this.movements = Transformations.switchMap(
                productId, id -> container.database().stockDao().observeMovementsByProductId(id));
    }

    public void setProductId(@NonNull String id) {
        if (!id.equals(productId.getValue())) {
            productId.setValue(id);
        }
    }

    public LiveData<ProductEntity> observeProduct() {
        return product;
    }

    public LiveData<List<StockMovementEntity>> observeMovements() {
        return movements;
    }

    public LiveData<Event<Integer>> observeMessage() {
        return messageEvent;
    }

    public void recordMovement(
            @NonNull String targetProductId,
            @NonNull StockMovementType type,
            int signedDelta,
            @Nullable String reason,
            @NonNull Consumer<Boolean> callback
    ) {
        container.executors().io().execute(() -> {
            Result<Outcome> result = container.recordStockMovementUseCase()
                    .execute(new Request(targetProductId, type, signedDelta, reason));
            if (result.isSuccess()) {
                int msgRes = result.getValue().negativeStockWarning()
                        ? R.string.stock_movement_saved_negative
                        : R.string.stock_movement_saved;
                messageEvent.postValue(new Event<>(msgRes));
                container.executors().mainThread().execute(() -> callback.accept(true));
                return;
            }
            int msgRes;
            if (result.getError() == DomainError.CONFLICT) {
                msgRes = R.string.stock_movement_error_inactive;
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                msgRes = R.string.stock_movement_error_validation;
            } else {
                msgRes = R.string.stock_movement_error_unexpected;
            }
            messageEvent.postValue(new Event<>(msgRes));
            container.executors().mainThread().execute(() -> callback.accept(false));
        });
    }
}
