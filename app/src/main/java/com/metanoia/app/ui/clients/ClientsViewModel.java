package com.metanoia.app.ui.clients;

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
import com.metanoia.app.data.db.dao.ClientSearchFilter;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.usecase.SaveClientUseCase;

import java.util.List;
import java.util.Objects;

/**
 * ViewModel para búsqueda reactiva liviana, alta/edición, baja lógica y detalle de clientes.
 */
public class ClientsViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<SearchState> searchState =
            new MutableLiveData<>(new SearchState("", false));
    private final LiveData<List<ClientRowProjection>> clients;
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Client>> savedClientEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> duplicateClientIdEvent = new MutableLiveData<>();

    public ClientsViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        clients = Transformations.switchMap(searchState, state -> {
            ClientSearchFilter filter = ClientSearchFilter.fromRawQuery(
                    state.query, state.includeInactive);
            return container.database().clientDao().observeSummaries(
                    filter.query,
                    filter.prefixPattern,
                    filter.wordPrefixPattern,
                    filter.phoneContainsPattern,
                    filter.includeInactive
            );
        });
    }

    public LiveData<List<ClientRowProjection>> observeClients() {
        return clients;
    }

    public LiveData<ClientEntity> observeClientById(@NonNull String clientId) {
        return container.database().clientDao().observeById(clientId);
    }

    public LiveData<List<AppointmentCardProjection>> observeClientVisits(@NonNull String clientId) {
        return container.database().appointmentDao().observeClientVisitCards(clientId);
    }

    public LiveData<Long> observeClientPendingBalanceCents(@NonNull String clientId) {
        return container.database().appointmentDao().observeClientPendingBalanceCents(clientId);
    }

    public LiveData<Long> observeClientTotalSpentCents(@NonNull String clientId) {
        return container.database().cashDao().observeClientTotalSpentCents(clientId);
    }

    public LiveData<List<CashMovementEntity>> observeClientMovements(@NonNull String clientId) {
        return container.database().cashDao().observeByClientId(clientId);
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Client>> observeSavedClient() {
        return savedClientEvent;
    }

    public LiveData<Event<String>> observeDuplicateClientId() {
        return duplicateClientIdEvent;
    }

    @NonNull
    public String currentQuery() {
        SearchState current = searchState.getValue();
        return current == null ? "" : current.query;
    }

    public void setQuery(@Nullable String query) {
        String safe = query == null ? "" : query;
        SearchState current = searchState.getValue();
        boolean includeInactive = current != null && current.includeInactive;
        if (current == null || !Objects.equals(current.query, safe)) {
            searchState.setValue(new SearchState(safe, includeInactive));
        }
    }

    public void setIncludeInactive(boolean includeInactive) {
        SearchState current = searchState.getValue();
        String query = current == null ? "" : current.query;
        if (current == null || current.includeInactive != includeInactive) {
            searchState.setValue(new SearchState(query, includeInactive));
        }
    }

    public void saveClient(
            @Nullable String id,
            String name,
            String phone,
            @Nullable Integer birthDay,
            @Nullable Integer birthMonth,
            @Nullable Integer birthYear,
            String notes,
            boolean active
    ) {
        container.executors().io().execute(() -> {
            SaveClientUseCase.Request request = new SaveClientUseCase.Request(
                    id, name, phone, birthDay, birthMonth, birthYear, notes, active);
            Result<Client> result = container.saveClientUseCase().execute(request);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.client_saved));
                savedClientEvent.postValue(new Event<>(result.getValue()));
            } else if (result.getError() == DomainError.CONFLICT) {
                String existingId = result.getErrorMessage();
                if (!existingId.isEmpty()) {
                    duplicateClientIdEvent.postValue(new Event<>(existingId));
                } else {
                    feedbackEvent.postValue(new Event<>(R.string.client_reactivate_conflict));
                }
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.client_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.client_error_unexpected));
            }
        });
    }

    public void setClientActive(@NonNull String clientId, boolean active) {
        container.executors().io().execute(() -> {
            Result<Client> result = container.setClientActiveUseCase().execute(clientId, active);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(
                        active ? R.string.client_reactivated : R.string.client_deactivated));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.client_reactivate_conflict));
            } else if (result.getError() == DomainError.NOT_FOUND) {
                feedbackEvent.postValue(new Event<>(R.string.client_detail_not_found));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.client_error_unexpected));
            }
        });
    }

    private static final class SearchState {
        final String query;
        final boolean includeInactive;

        SearchState(String query, boolean includeInactive) {
            this.query = query;
            this.includeInactive = includeInactive;
        }
    }
}
