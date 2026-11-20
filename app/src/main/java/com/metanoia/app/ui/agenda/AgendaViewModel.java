package com.metanoia.app.ui.agenda;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel de la agenda diaria y semanal con filtro reactivo por fecha y profesional.
 */
public class AgendaViewModel extends AndroidViewModel {

    public enum ViewMode {
        DAY,
        WEEK
    }

    private final AppContainer container;
    private final MutableLiveData<AgendaFilter> filterState;
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final LiveData<List<AppointmentCardProjection>> dayAppointments;
    private final LiveData<List<ScheduleBlockEntity>> dayBlocks;
    private final LiveData<List<AppointmentCardProjection>> weekAppointments;
    private final LiveData<List<ScheduleBlockEntity>> weekBlocks;
    private final LiveData<Integer> lowStockCount;
    private final LiveData<Integer> activeClientsCount;

    public AgendaViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        long today = container.clock().todayEpochDay();
        filterState = new MutableLiveData<>(new AgendaFilter(today, null, ViewMode.DAY));

        activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);
        lowStockCount = container.database().stockDao().observeLowStockCount();
        activeClientsCount = container.database().clientDao().observeActiveCount();

        dayAppointments = Transformations.switchMap(filterState, filter ->
                container.database().appointmentDao().observeDayCards(
                        filter.selectedDayEpoch,
                        filter.professionalId
                ));

        dayBlocks = Transformations.switchMap(filterState, filter ->
                container.database().scheduleBlockDao().observeActiveOverlappingRange(
                        filter.selectedDayEpoch,
                        filter.selectedDayEpoch
                ));

        weekAppointments = Transformations.switchMap(filterState, filter -> {
            long monday = mondayOf(filter.selectedDayEpoch);
            return container.database().appointmentDao().observeRangeCards(
                    monday, monday + 6L, filter.professionalId);
        });

        weekBlocks = Transformations.switchMap(filterState, filter -> {
            long monday = mondayOf(filter.selectedDayEpoch);
            return container.database().scheduleBlockDao().observeActiveOverlappingRange(
                    monday, monday + 6L);
        });
    }

    public LiveData<AgendaFilter> observeFilter() {
        return filterState;
    }

    public LiveData<List<ProfessionalWithDetails>> observeActiveProfessionals() {
        return activeProfessionals;
    }

    public LiveData<List<AppointmentCardProjection>> observeDayAppointments() {
        return dayAppointments;
    }

    public LiveData<List<ScheduleBlockEntity>> observeDayBlocks() {
        return dayBlocks;
    }

    public LiveData<List<AppointmentCardProjection>> observeWeekAppointments() {
        return weekAppointments;
    }

    public LiveData<List<ScheduleBlockEntity>> observeWeekBlocks() {
        return weekBlocks;
    }

    public LiveData<Integer> observeActiveClientsCount() {
        return activeClientsCount;
    }

    public LiveData<Integer> observeLowStockCount() {
        return lowStockCount;
    }

    public long currentSelectedDay() {
        AgendaFilter current = filterState.getValue();
        return current == null ? container.clock().todayEpochDay() : current.selectedDayEpoch;
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
    }

    @Nullable
    public String currentProfessionalId() {
        AgendaFilter current = filterState.getValue();
        return current == null ? null : current.professionalId;
    }

    public void setSelectedDay(long epochDay) {
        AgendaFilter current = filterState.getValue();
        if (current != null && current.selectedDayEpoch != epochDay) {
            filterState.setValue(new AgendaFilter(
                    epochDay, current.professionalId, current.viewMode));
        }
    }

    public void shiftDate(int direction) {
        AgendaFilter current = filterState.getValue();
        if (current == null) {
            return;
        }
        long stepDays = current.viewMode == ViewMode.WEEK ? 7L : 1L;
        setSelectedDay(current.selectedDayEpoch + direction * stepDays);
    }

    public void jumpToToday() {
        setSelectedDay(container.clock().todayEpochDay());
    }

    public void selectDayAndOpenDayMode(long epochDay) {
        AgendaFilter current = filterState.getValue();
        String proId = current == null ? null : current.professionalId;
        filterState.setValue(new AgendaFilter(epochDay, proId, ViewMode.DAY));
    }

    public void setViewMode(@NonNull ViewMode mode) {
        AgendaFilter current = filterState.getValue();
        if (current != null && current.viewMode != mode) {
            filterState.setValue(new AgendaFilter(
                    current.selectedDayEpoch, current.professionalId, mode));
        }
    }

    public void setSelectedProfessional(@Nullable String professionalId) {
        AgendaFilter current = filterState.getValue();
        if (current != null && !Objects.equals(current.professionalId, professionalId)) {
            filterState.setValue(new AgendaFilter(
                    current.selectedDayEpoch, professionalId, current.viewMode));
        }
    }

    public static long mondayOf(long epochDay) {
        return LocalDate.ofEpochDay(epochDay)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toEpochDay();
    }

    public static final class AgendaFilter {
        public final long selectedDayEpoch;
        @Nullable
        public final String professionalId;
        public final ViewMode viewMode;

        public AgendaFilter(
                long selectedDayEpoch,
                @Nullable String professionalId,
                ViewMode viewMode
        ) {
            this.selectedDayEpoch = selectedDayEpoch;
            this.professionalId = professionalId;
            this.viewMode = viewMode;
        }
    }
}
