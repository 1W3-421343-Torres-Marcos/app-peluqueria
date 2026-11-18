package com.metanoia.app.ui.agenda.blocks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.metanoia.app.AppContainer;
import com.metanoia.app.MetanoiaApp;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.ui.common.Event;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;
import com.metanoia.domain.usecase.CreateScheduleBlockUseCase;
import com.metanoia.domain.usecase.CreateScheduleBlockUseCase.CreateResult;

import java.util.List;

/**
 * ViewModel para listar, crear y desactivar bloqueos generales o por profesional.
 */
public class ScheduleBlocksViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final LiveData<List<ScheduleBlockEntity>> activeBlocks;
    private final LiveData<List<ProfessionalWithDetails>> activeProfessionals;
    private final MutableLiveData<Event<Integer>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeFormEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> affectedCountEvent = new MutableLiveData<>();

    public ScheduleBlocksViewModel(@NonNull Application application) {
        super(application);
        container = ((MetanoiaApp) application).getContainer();
        activeBlocks = container.database().scheduleBlockDao().observeAll(false);
        activeProfessionals = container.database().professionalDao().observeAllWithDetails(false);
    }

    public LiveData<List<ScheduleBlockEntity>> observeActiveBlocks() {
        return activeBlocks;
    }

    public LiveData<List<ProfessionalWithDetails>> observeActiveProfessionals() {
        return activeProfessionals;
    }

    public LiveData<Event<Integer>> observeFeedback() {
        return feedbackEvent;
    }

    public LiveData<Event<Boolean>> observeCloseForm() {
        return closeFormEvent;
    }

    public LiveData<Event<Integer>> observeAffectedCount() {
        return affectedCountEvent;
    }

    public long todayEpochDay() {
        return container.clock().todayEpochDay();
    }

    public void createBlock(
            @Nullable String professionalId,
            long startDay,
            long endDay,
            @Nullable Integer startMin,
            @Nullable Integer endMin,
            @NonNull ScheduleBlockType type,
            @NonNull String reason
    ) {
        container.executors().io().execute(() -> {
            Result<CreateResult> result = container.createScheduleBlockUseCase().execute(
                    new CreateScheduleBlockUseCase.Request(
                            professionalId, startDay, endDay, startMin, endMin, type, reason));
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.schedule_block_saved));
                closeFormEvent.postValue(new Event<>(true));
                int affected = result.getValue().affectedAppointments().size();
                if (affected > 0) {
                    affectedCountEvent.postValue(new Event<>(affected));
                }
            } else if (result.getError() == DomainError.VALIDATION_ERROR) {
                feedbackEvent.postValue(new Event<>(R.string.schedule_block_error_validation));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }

    public void deactivateBlock(@NonNull String blockId) {
        container.executors().io().execute(() -> {
            Result<ScheduleBlock> result = container.deactivateScheduleBlockUseCase()
                    .execute(blockId);
            if (result.isSuccess()) {
                feedbackEvent.postValue(new Event<>(R.string.schedule_block_deactivated));
            } else {
                feedbackEvent.postValue(new Event<>(R.string.appointment_error_unexpected));
            }
        });
    }
}
