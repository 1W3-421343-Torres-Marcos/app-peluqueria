package com.metanoia.app.ui.waitlist;

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
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.data.mapper.WaitlistMapper;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.usecase.SaveWaitlistEntryUseCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * ViewModel para gestionar la lista de espera, búsqueda liviana de clientes y alta/edición/cancelación.
 */
public class WaitlistViewModel extends AndroidViewModel {

    private static final int CLIENT_PICKER_LIMIT = 12;

    private final AppContainer container;
    private final MutableLiveData<Boolean> onlyOpen = new MutableLiveData<>(Boolean.TRUE);
    private final MutableLiveData<String> clientQuery = new MutableLiveData<>("");
    private final LiveData<List<WaitlistEntry>> entries;
    private final LiveData<List<ClientRowProjection>> clientsForEntries;
    private final LiveData<List<ClientRowProjection>> clientSearchResults;
    private final LiveData<List<SalonServiceEntity>> services;
    private final LiveData<List<ProfessionalWithDetails>> professionals;
    private final LiveData<Integer> waitlistExpiryDays;
    private final MutableLiveData<LoadedEntryState> loadedEntry = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<WaitlistEntry>> savedEntryEvent = new MutableLiveData<>();

    public WaitlistViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        container.executors().io().execute(() -> container.expireWaitlistEntriesUseCase().execute());

        entries = Transformations.switchMap(onlyOpen, openOnly -> {
            if (Boolean.TRUE.equals(openOnly)) {
                return Transformations.map(
                        container.database().waitlistDao().observeByStatusesWithServices(
                                Arrays.asList(
                                        WaitlistStatus.ACTIVE.name(),
                                        WaitlistStatus.OFFERED.name())),
                        WaitlistMapper::toDomainList);
            }
            return Transformations.map(
                    container.database().waitlistDao().observeAllWithServices(),
                    WaitlistMapper::toDomainList);
        });

        clientsForEntries = Transformations.switchMap(entries, list -> {
            if (list == null || list.isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            Set<String> ids = new LinkedHashSet<>();
            for (WaitlistEntry entry : list) {
                ids.add(entry.clientId());
            }
            return container.database().clientDao().observeSummariesByIds(new ArrayList<>(ids));
        });

        clientSearchResults = Transformations.switchMap(clientQuery, q -> {
            ClientSearchFilter filter = ClientSearchFilter.fromRawQuery(q, false);
            return container.database().clientDao().observeTopActiveSummaries(
                    filter.query,
                    filter.prefixPattern,
                    filter.wordPrefixPattern,
                    filter.phoneContainsPattern,
                    CLIENT_PICKER_LIMIT);
        });

        services = container.database().salonServiceDao().observeAll(true);
        professionals = container.database().professionalDao().observeAllWithDetails(true);
        waitlistExpiryDays = Transformations.map(
                container.database().settingsDao().observeSettings(),
                entity -> entity == null
                        ? Settings.DEFAULT_WAITLIST_EXPIRY_DAYS
                        : entity.waitlistExpiryDays);
    }

    public LiveData<List<WaitlistEntry>> observeEntries() {
        return entries;
    }

    public LiveData<List<ClientRowProjection>> observeClientsForEntries() {
        return clientsForEntries;
    }

    public LiveData<List<ClientRowProjection>> observeClientSearchResults() {
        return clientSearchResults;
    }

    public LiveData<List<SalonServiceEntity>> observeServices() {
        return services;
    }

    public LiveData<List<ProfessionalWithDetails>> observeProfessionals() {
        return professionals;
    }

    public LiveData<Integer> observeWaitlistExpiryDays() {
        return waitlistExpiryDays;
    }

    public LiveData<LoadedEntryState> observeLoadedEntry() {
        return loadedEntry;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<WaitlistEntry>> observeSavedEntry() {
        return savedEntryEvent;
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
    }

    public void setOnlyOpen(boolean onlyOpenFilter) {
        Boolean target = Boolean.valueOf(onlyOpenFilter);
        if (!Objects.equals(onlyOpen.getValue(), target)) {
            onlyOpen.setValue(target);
        }
    }

    public void setClientQuery(@Nullable String query) {
        String safe = query == null ? "" : query;
        if (!Objects.equals(clientQuery.getValue(), safe)) {
            clientQuery.setValue(safe);
        }
    }

    public void loadEntry(@NonNull String entryId) {
        container.executors().io().execute(() -> {
            Optional<WaitlistEntry> found = container.waitlistRepository().findById(entryId);
            if (!found.isPresent()) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_not_found));
                return;
            }
            WaitlistEntry entry = found.get();
            String clientName = container.clientRepository().findById(entry.clientId())
                    .map(Client::name)
                    .orElse("");
            loadedEntry.postValue(new LoadedEntryState(entry, clientName));
        });
    }

    public void saveEntry(
            @Nullable String id,
            @Nullable String clientId,
            @NonNull List<String> serviceIds,
            @Nullable String professionalId,
            @Nullable Long specificDay,
            @NonNull Set<Integer> preferredDaysOfWeek,
            int fromMin,
            int toMin,
            @Nullable Long validUntilDay,
            @Nullable String notes
    ) {
        container.executors().io().execute(() -> {
            Result<WaitlistEntry> result = container.saveWaitlistEntryUseCase().execute(
                    new SaveWaitlistEntryUseCase.Request(
                            id,
                            clientId,
                            serviceIds,
                            professionalId,
                            specificDay,
                            preferredDaysOfWeek,
                            fromMin,
                            toMin,
                            validUntilDay,
                            notes
                    ));
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_saved));
                savedEntryEvent.postValue(new Event<>(result.getValue()));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_error_duplicate));
            } else if (result.getError() == DomainError.NOT_FOUND) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_not_found));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_error_unexpected));
            }
        });
    }

    public void cancelEntry(@NonNull String entryId) {
        container.executors().io().execute(() -> {
            Result<WaitlistEntry> result = container.cancelWaitlistEntryUseCase().execute(entryId);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_cancelled));
            } else if (result.getError() == DomainError.NOT_FOUND) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_not_found));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_error_unexpected));
            }
        });
    }

    public static final class LoadedEntryState {
        public final WaitlistEntry entry;
        public final String clientName;

        public LoadedEntryState(@NonNull WaitlistEntry entry, @NonNull String clientName) {
            this.entry = Objects.requireNonNull(entry, "entry");
            this.clientName = Objects.requireNonNull(clientName, "clientName");
        }
    }
}
