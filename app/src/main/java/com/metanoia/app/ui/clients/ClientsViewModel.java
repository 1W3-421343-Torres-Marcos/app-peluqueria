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
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.data.mapper.ColorFormulaMapper;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.rules.ColorFormulaRules;
import com.metanoia.domain.rules.ColorFormulaRules.ComponentDraft;
import com.metanoia.domain.usecase.DuplicateColorFormulaUseCase;
import com.metanoia.domain.usecase.SaveClientUseCase;
import com.metanoia.domain.usecase.SaveColorFormulaUseCase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private final MutableLiveData<LoadedFormulaState> loadedColorFormula = new MutableLiveData<>();
    private final MutableLiveData<Event<ColorFormula>> savedColorFormulaEvent =
            new MutableLiveData<>();

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

    public LiveData<List<ColorFormula>> observeClientColorFormulas(@NonNull String clientId) {
        return Transformations.map(
                container.database().colorFormulaDao().observeActiveByClientId(clientId),
                ColorFormulaMapper::toDomainList);
    }

    public LiveData<List<String>> observeClosedFormulaIdsByClient(@NonNull String clientId) {
        return container.database().colorFormulaDao().observeClosedFormulaIdsByClient(clientId);
    }

    public LiveData<List<ProfessionalWithDetails>> observeProfessionals(boolean includeInactive) {
        return container.database().professionalDao().observeAllWithDetails(includeInactive);
    }

    public LiveData<LoadedFormulaState> observeLoadedColorFormula() {
        return loadedColorFormula;
    }

    public LiveData<Event<ColorFormula>> observeSavedColorFormula() {
        return savedColorFormulaEvent;
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
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

    public void loadColorFormula(@NonNull String formulaId) {
        container.executors().io().execute(() -> {
            Optional<ColorFormula> found = container.colorFormulaRepository().findById(formulaId);
            if (!found.isPresent() || found.get().isDeleted()) {
                feedbackEvent.postValue(new Event<>(R.string.color_formula_not_found));
                return;
            }
            ColorFormula formula = found.get();
            boolean locked = container.colorFormulaRepository()
                    .isLinkedToClosedAppointment(formula.id());
            if (!locked && formula.appointmentId().isPresent()) {
                Optional<Appointment> apt = container.appointmentRepository()
                        .findById(formula.appointmentId().get());
                locked = ColorFormulaRules.isLockedByAppointmentStatus(apt.map(Appointment::status));
            }
            loadedColorFormula.postValue(new LoadedFormulaState(formula, locked));
        });
    }

    public void saveColorFormula(
            @Nullable String id,
            @NonNull String clientId,
            @Nullable String appointmentId,
            @Nullable String professionalId,
            long day,
            int exposureMin,
            @Nullable String developerVolume,
            @Nullable String notes,
            @Nullable String resultText,
            @NonNull List<ComponentDraft> components
    ) {
        container.executors().io().execute(() -> {
            Result<ColorFormula> result = container.saveColorFormulaUseCase().execute(
                    new SaveColorFormulaUseCase.Request(
                            id,
                            clientId,
                            appointmentId,
                            professionalId,
                            day,
                            exposureMin,
                            developerVolume,
                            notes,
                            resultText,
                            components
                    )
            );
            handleColorFormulaWriteResult(result, R.string.color_formula_saved);
        });
    }

    public void duplicateColorFormula(@NonNull String formulaId) {
        container.executors().io().execute(() -> {
            Result<ColorFormula> result = container.duplicateColorFormulaUseCase().execute(
                    new DuplicateColorFormulaUseCase.Request(formulaId));
            handleColorFormulaWriteResult(result, R.string.color_formula_duplicated);
        });
    }

    public void deleteColorFormula(@NonNull String formulaId) {
        container.executors().io().execute(() -> {
            Result<Boolean> result = container.deleteColorFormulaUseCase().execute(formulaId);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.color_formula_deleted));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.color_formula_error_locked));
            } else if (result.getError() == DomainError.NOT_FOUND) {
                feedbackEvent.postValue(new Event<>(R.string.color_formula_not_found));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.color_formula_error_unexpected));
            }
        });
    }

    private void handleColorFormulaWriteResult(Result<ColorFormula> result, int successMsgRes) {
        if (result.isSuccess()) {
            feedbackEvent.postValue(new Event<>(successMsgRes));
            savedColorFormulaEvent.postValue(new Event<>(result.getValue()));
        } else if (result.getError() == DomainError.VALIDATION_ERROR) {
            feedbackEvent.postValue(new Event<>(R.string.color_formula_error_validation));
        } else if (result.getError() == DomainError.CONFLICT) {
            feedbackEvent.postValue(new Event<>(R.string.color_formula_error_locked));
        } else if (result.getError() == DomainError.NOT_FOUND) {
            feedbackEvent.postValue(new Event<>(R.string.color_formula_not_found));
        } else {
            feedbackEvent.postValue(new Event<>(R.string.color_formula_error_unexpected));
        }
    }

    public static final class LoadedFormulaState {
        public final ColorFormula formula;
        public final boolean locked;

        public LoadedFormulaState(@NonNull ColorFormula formula, boolean locked) {
            this.formula = Objects.requireNonNull(formula, "formula");
            this.locked = locked;
        }
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
