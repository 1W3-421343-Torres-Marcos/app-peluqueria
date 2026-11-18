package com.metanoia.app.ui.reminders;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.BirthdayReminderProjection;
import com.metanoia.app.data.db.projection.TomorrowReminderProjection;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Reminder;
import com.metanoia.domain.model.Settings;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel de Recordatorios: observa pendientes de mañana, cumpleaños del día y registra envíos.
 */
public class RemindersViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final long tomorrowEpochDay;
    private final LocalDate todayDate;
    private final LiveData<List<TomorrowReminderProjection>> tomorrowReminders;
    private final LiveData<List<BirthdayReminderProjection>> todayBirthdays;
    private final MutableLiveData<Settings> currentSettings = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();

    public RemindersViewModel(@NonNull Application application) {
        super(application);
        this.container = ((MetanoiaApp) application).getContainer();
        long todayEpoch = container.clock().todayEpochDay();
        this.tomorrowEpochDay = todayEpoch + 1L;
        this.todayDate = LocalDate.ofEpochDay(todayEpoch);

        this.tomorrowReminders = container.database().reminderDao()
                .observeTomorrowReminders(tomorrowEpochDay);
        this.todayBirthdays = container.database().reminderDao()
                .observeTodayBirthdays(
                        todayDate.getDayOfMonth(),
                        todayDate.getMonthValue(),
                        todayDate.getYear());
        refreshSettings();
    }

    public long tomorrowEpochDay() {
        return tomorrowEpochDay;
    }

    public long todayEpochDay() {
        return todayDate.toEpochDay();
    }

    public int currentYear() {
        return todayDate.getYear();
    }

    public void refreshSettings() {
        container.executors().io().execute(() ->
                currentSettings.postValue(container.settingsRepository().getSettings()));
    }

    public LiveData<List<TomorrowReminderProjection>> observeTomorrowReminders() {
        return tomorrowReminders;
    }

    public LiveData<List<BirthdayReminderProjection>> observeTodayBirthdays() {
        return todayBirthdays;
    }

    public LiveData<Settings> observeSettings() {
        return currentSettings;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public void markAppointmentReminded(
            @NonNull String appointmentId,
            @NonNull String clientId,
            @NonNull Consumer<Reminder> onRecorded) {
        container.executors().io().execute(() -> {
            Result<Reminder> result = container.recordReminderSentUseCase()
                    .recordAppointmentReminder(appointmentId, clientId);
            if (result.isSuccess()) {
                container.executors().mainThread().execute(() -> onRecorded.accept(result.getValue()));
            }
        });
    }

    public void markBirthdayGreeted(
            @NonNull String clientId,
            @NonNull Consumer<Reminder> onRecorded) {
        container.executors().io().execute(() -> {
            Result<Reminder> result = container.recordReminderSentUseCase()
                    .recordBirthdayGreeting(clientId, currentYear());
            if (result.isSuccess()) {
                container.executors().mainThread().execute(() -> onRecorded.accept(result.getValue()));
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
}
