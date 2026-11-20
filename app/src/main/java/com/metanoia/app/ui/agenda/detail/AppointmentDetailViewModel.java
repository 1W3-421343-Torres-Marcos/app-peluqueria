package com.metanoia.app.ui.agenda.detail;

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
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.data.db.entity.ReminderEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.mapper.AppointmentMapper;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader;
import com.metanoia.app.ui.agenda.SlotAvailabilityLoader.SlotLoadResult;
import com.metanoia.app.ui.common.Event;
import com.metanoia.app.util.ReceiptPdfBuilder;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;
import com.metanoia.domain.rules.TemplateRenderer;
import com.metanoia.domain.rules.TemplateRenderer.TemplateContext;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;
import com.metanoia.domain.rules.WaitlistMatcher.MatchedCandidate;
import com.metanoia.domain.usecase.AssignWaitlistEntryUseCase.AssignResult;
import com.metanoia.domain.usecase.CancelRecurringSeriesUseCase;
import com.metanoia.domain.usecase.OfferWaitlistEntryUseCase.OfferPreview;
import com.metanoia.domain.usecase.OfferWaitlistEntryUseCase.OfferResult;
import com.metanoia.domain.usecase.RescheduleAppointmentUseCase;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ViewModel de detalle de turno: resuelve participantes, transiciones de estado y reprogramación.
 */
public class AppointmentDetailViewModel extends AndroidViewModel {

    public static final class DetailUiState {
        public final Appointment appointment;
        public final String clientName;
        public final String clientPhone;
        public final String professionalName;

        public DetailUiState(
                Appointment appointment,
                String clientName,
                String clientPhone,
                String professionalName
        ) {
            this.appointment = appointment;
            this.clientName = clientName;
            this.clientPhone = clientPhone;
            this.professionalName = professionalName;
        }
    }

    public static final class WaitlistSuggestionsState {
        public final FreedSlot freedSlot;
        public final String professionalName;
        public final List<MatchedCandidate> candidates;
        public final Set<String> offeredEntryIds;

        public WaitlistSuggestionsState(
                FreedSlot freedSlot,
                String professionalName,
                List<MatchedCandidate> candidates,
                Set<String> offeredEntryIds
        ) {
            this.freedSlot = freedSlot;
            this.professionalName = professionalName;
            this.candidates = candidates;
            this.offeredEntryIds = offeredEntryIds;
        }
    }

    private final AppContainer container;
    private final MutableLiveData<DetailUiState> detailState = new MutableLiveData<>();
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final MutableLiveData<SlotLoadResult> rescheduleSlots = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeRescheduleEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<File>> shareReceiptEvent = new MutableLiveData<>();
    private final MutableLiveData<WaitlistSuggestionsState> waitlistSuggestionsState =
            new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> openWaitlistSuggestionsEvent =
            new MutableLiveData<>();
    private final MutableLiveData<Event<String>> navigateToAssignedAppointmentEvent =
            new MutableLiveData<>();

    public AppointmentDetailViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);
    }

    public LiveData<AppointmentWithItems> observeRawAppointment(@NonNull String appointmentId) {
        return container.database().appointmentDao().observeByIdWithItems(appointmentId);
    }

    public LiveData<List<CashMovementEntity>> observeAppointmentMovements(
            @NonNull String appointmentId
    ) {
        return container.database().cashDao().observeByAppointmentId(appointmentId);
    }

    public LiveData<ReminderEntity> observeAppointmentReminder(@NonNull String appointmentId) {
        return container.database().reminderDao().observeByAppointmentId(appointmentId);
    }

    public LiveData<DetailUiState> observeDetailState() {
        return detailState;
    }

    public LiveData<List<ProfessionalWithDetails>> observeActiveProfessionals() {
        return activeProfessionals;
    }

    public LiveData<SlotLoadResult> observeRescheduleSlots() {
        return rescheduleSlots;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeCloseReschedule() {
        return closeRescheduleEvent;
    }

    public LiveData<Event<File>> observeShareReceipt() {
        return shareReceiptEvent;
    }

    public LiveData<WaitlistSuggestionsState> observeWaitlistSuggestionsState() {
        return waitlistSuggestionsState;
    }

    public LiveData<Event<Boolean>> observeOpenWaitlistSuggestions() {
        return openWaitlistSuggestionsEvent;
    }

    public LiveData<Event<String>> observeNavigateToAssignedAppointment() {
        return navigateToAssignedAppointmentEvent;
    }

    public void generateReceiptPdf(
            @NonNull DetailUiState state,
            @NonNull List<CashMovement> movements) {
        container.executors().io().execute(() -> {
            try {
                Settings settings = container.settingsRepository().getSettings();
                File pdfFile = ReceiptPdfBuilder.buildPdfFile(
                        getApplication(),
                        settings,
                        state.appointment,
                        state.clientName,
                        state.professionalName,
                        movements);
                shareReceiptEvent.postValue(new Event<>(pdfFile));
            } catch (IOException ex) {
                feedbackEvent.postValue(new Event<>(R.string.receipt_share_failed));
            }
        });
    }

    public void resolveDetail(@Nullable AppointmentWithItems withItems) {
        if (withItems == null || withItems.appointment == null) {
            detailState.setValue(null);
            return;
        }
        Appointment appointment = AppointmentMapper.toDomain(withItems);
        container.executors().io().execute(() -> {
            Optional<Client> clientOpt = container.clientRepository()
                    .findById(appointment.clientId());
            Optional<Professional> proOpt = container.professionalRepository()
                    .findById(appointment.professionalId());
            String clientName = clientOpt.map(Client::name).orElse("");
            String clientPhone = clientOpt.map(Client::phone).orElse("");
            String proName = proOpt.map(Professional::name).orElse("");
            detailState.postValue(new DetailUiState(appointment, clientName, clientPhone, proName));
        });
    }

    public void buildAppointmentReminderMessage(
            @NonNull DetailUiState state,
            @NonNull Consumer<String> onBuilt
    ) {
        container.executors().io().execute(() -> {
            Settings settings = container.settingsRepository().getSettings();
            StringBuilder services = new StringBuilder();
            for (AppointmentItem item : state.appointment.items()) {
                if (services.length() > 0) {
                    services.append(", ");
                }
                services.append(item.serviceNameSnapshot());
            }
            TemplateContext context = new TemplateContext(
                    state.clientName,
                    state.appointment.day(),
                    state.appointment.startMin(),
                    services.toString(),
                    state.professionalName,
                    settings.businessDisplayName());
            String message = TemplateRenderer.render(settings.reminderTemplate(), context)
                    .renderedText();
            container.executors().mainThread().execute(() -> onBuilt.accept(message));
        });
    }

    public void markAppointmentReminded(
            @NonNull String appointmentId,
            @NonNull String clientId,
            @NonNull Consumer<Reminder> onRecorded
    ) {
        container.executors().io().execute(() -> {
            Result<Reminder> result = container.recordReminderSentUseCase()
                    .recordAppointmentReminder(appointmentId, clientId);
            if (result.isSuccess()) {
                container.executors().mainThread()
                        .execute(() -> onRecorded.accept(result.getValue()));
            }
        });
    }

    public void undoReminder(@NonNull String reminderId) {
        container.executors().io().execute(() -> {
            Result<Boolean> result = container.recordReminderSentUseCase().undoReminder(reminderId);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.reminder_undone));
            }
        });
    }

    public void changeStatus(
            @NonNull String appointmentId,
            @NonNull AppointmentStatus targetStatus,
            @Nullable String cancelReason
    ) {
        container.executors().io().execute(() -> {
            Result<Appointment> result;
            try {
                long paidCents = AppointmentPaymentCalculator.netPaid(
                        container.cashRepository().findMovementsByAppointmentId(appointmentId)
                ).cents();
                result = container.changeAppointmentStatusUseCase()
                        .execute(appointmentId, targetStatus, cancelReason, paidCents);
            } catch (RuntimeException ex) {
                result = Result.failure(DomainError.DATABASE_ERROR,
                        "No se pudo cambiar el estado del turno");
            }
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_status_updated));
                if (targetStatus == AppointmentStatus.CANCELLED) {
                    Appointment cancelled = result.getValue();
                    evaluateFreedSlotCandidates(new FreedSlot(
                            cancelled.professionalId(),
                            cancelled.day(),
                            cancelled.startMin(),
                            cancelled.endMin()));
                }
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_cancel_reason_required));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_status_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void cancelRecurring(
            @NonNull String appointmentId,
            @NonNull CancelRecurringSeriesUseCase.Scope scope,
            @Nullable String cancelReason
    ) {
        container.executors().io().execute(() -> {
            Optional<Appointment> beforeOpt = container.appointmentRepository()
                    .findById(appointmentId);
            Result<List<Appointment>> result = container.cancelRecurringSeriesUseCase()
                    .execute(appointmentId, scope, cancelReason);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_status_updated));
                if (beforeOpt.isPresent()) {
                    Appointment apt = beforeOpt.get();
                    evaluateFreedSlotCandidates(new FreedSlot(
                            apt.professionalId(), apt.day(), apt.startMin(), apt.endMin()));
                }
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_cancel_reason_required));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_status_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void reverseMovement(@NonNull String movementId, @NonNull String reason) {
        container.executors().io().execute(() -> {
            Result<CashMovement> result = container.reverseMovementUseCase()
                    .execute(movementId, reason);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.cash_reverse_success));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.cash_reverse_reason_required));
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.cash_reverse_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void loadRescheduleSlots(
            @NonNull String professionalId,
            long dayEpoch,
            int durationMin,
            @NonNull String appointmentId
    ) {
        container.executors().io().execute(() -> rescheduleSlots.postValue(
                SlotAvailabilityLoader.load(
                        container, professionalId, dayEpoch, durationMin, appointmentId)));
    }

    public void reschedule(
            @NonNull String appointmentId,
            @NonNull String professionalId,
            long dayEpoch,
            @Nullable Integer startMin
    ) {
        if (startMin == null) {
            feedbackEvent.setValue(new Event<>(R.string.appointment_error_validation));
            return;
        }
        container.executors().io().execute(() -> {
            Optional<Appointment> beforeOpt = container.appointmentRepository()
                    .findById(appointmentId);
            Result<Appointment> result = container.rescheduleAppointmentUseCase().execute(
                    new RescheduleAppointmentUseCase.Request(
                            appointmentId, professionalId, dayEpoch, startMin));
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_rescheduled));
                closeRescheduleEvent.postValue(new Event<>(true));
                if (beforeOpt.isPresent()) {
                    Appointment old = beforeOpt.get();
                    Appointment updated = result.getValue();
                    boolean slotMoved = !old.professionalId().equals(updated.professionalId())
                            || old.day() != updated.day()
                            || old.startMin() != updated.startMin()
                            || old.endMin() != updated.endMin();
                    if (slotMoved) {
                        evaluateFreedSlotCandidates(new FreedSlot(
                                old.professionalId(), old.day(), old.startMin(), old.endMin()));
                    }
                }
            } else if (result.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_conflict));
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void prepareWaitlistOffer(
            @NonNull String entryId,
            @NonNull FreedSlot slot,
            @NonNull Consumer<OfferPreview> onPrepared
    ) {
        container.executors().io().execute(() -> {
            Result<OfferPreview> res = container.offerWaitlistEntryUseCase()
                    .prepareOffer(entryId, slot);
            if (res.isSuccess()) {
                container.executors().mainThread().execute(() -> onPrepared.accept(res.getValue()));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_not_found));
            }
        });
    }

    public void markWaitlistOfferSent(@NonNull String entryId, @NonNull FreedSlot slot) {
        container.executors().io().execute(() -> {
            Result<OfferResult> res = container.offerWaitlistEntryUseCase().execute(entryId, slot);
            if (res.isSuccess()) {
                WaitlistSuggestionsState current = waitlistSuggestionsState.getValue();
                if (current != null) {
                    Set<String> offered = new LinkedHashSet<>(current.offeredEntryIds);
                    offered.add(entryId);
                    waitlistSuggestionsState.postValue(new WaitlistSuggestionsState(
                            current.freedSlot, current.professionalName,
                            current.candidates, offered));
                }
                feedbackEvent.postValue(new Event<>(R.string.waitlist_offer_sent));
            }
        });
    }

    public void assignWaitlistEntry(
            @NonNull String entryId,
            @NonNull FreedSlot slot,
            @NonNull Runnable onAssignedClose
    ) {
        container.executors().io().execute(() -> {
            Result<AssignResult> res = container.assignWaitlistEntryUseCase().execute(entryId, slot);
            if (res.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_assign_success));
                navigateToAssignedAppointmentEvent.postValue(
                        new Event<>(res.getValue().appointment().id()));
                container.executors().mainThread().execute(onAssignedClose);
            } else if (res.getError() == DomainError.CONFLICT) {
                feedbackEvent.postValue(new Event<>(R.string.waitlist_assign_conflict));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    private void evaluateFreedSlotCandidates(@NonNull FreedSlot freedSlot) {
        if (freedSlot.dayEpoch() < container.clock().todayEpochDay()) {
            return;
        }
        Result<List<MatchedCandidate>> res = container.findWaitlistCandidatesUseCase()
                .execute(freedSlot);
        if (!res.isSuccess() || res.getValue().isEmpty()) {
            return;
        }
        String proName = container.professionalRepository()
                .findById(freedSlot.professionalId())
                .map(Professional::name)
                .orElse("");
        waitlistSuggestionsState.postValue(new WaitlistSuggestionsState(
                freedSlot, proName, res.getValue(), Collections.emptySet()));
        openWaitlistSuggestionsEvent.postValue(new Event<>(true));
    }
}
