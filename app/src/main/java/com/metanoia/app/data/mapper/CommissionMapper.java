package com.metanoia.app.data.mapper;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.data.db.entity.CommissionSettlementEntryEntity;
import com.metanoia.app.data.db.pojo.CommissionSettlementWithEntries;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.CommissionSettlementEntry;
import com.metanoia.domain.model.CommissionSettlementEntryType;
import com.metanoia.domain.model.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Conversión entre entidades Room de liquidaciones de comisiones y modelos del dominio.
 */
public final class CommissionMapper {

    private CommissionMapper() {
    }

    @NonNull
    public static CommissionSettlement toDomain(@NonNull CommissionSettlementWithEntries row) {
        CommissionSettlementEntity s = row.settlement;
        List<CommissionSettlementEntryEntity> rawEntries = row.entries == null
                ? Collections.emptyList()
                : row.entries;
        List<CommissionSettlementEntry> entries = new ArrayList<>(rawEntries.size());
        for (CommissionSettlementEntryEntity e : rawEntries) {
            entries.add(new CommissionSettlementEntry.Builder()
                    .id(e.id)
                    .settlementId(e.settlementId)
                    .appointmentId(e.appointmentId)
                    .type(CommissionSettlementEntryType.valueOf(e.type))
                    .baseCents(e.baseCents)
                    .commissionCents(e.commissionCents)
                    .build());
        }
        return new CommissionSettlement.Builder()
                .id(s.id)
                .professionalId(s.professionalId)
                .fromDay(s.fromDay)
                .toDay(s.toDay)
                .baseAmount(Money.ofCents(s.baseCents))
                .commissionAmount(Money.ofCents(s.commissionCents))
                .cashMovementId(s.cashMovementId)
                .settledAt(s.settledAt)
                .operator(s.operator)
                .entries(entries)
                .build();
    }

    @NonNull
    public static CommissionSettlementEntity toEntity(@NonNull CommissionSettlement model) {
        return new CommissionSettlementEntity(
                model.id(),
                model.professionalId(),
                model.fromDay(),
                model.toDay(),
                model.baseAmount().cents(),
                model.commissionAmount().cents(),
                model.cashMovementId(),
                model.settledAt(),
                model.operator()
        );
    }

    @NonNull
    public static List<CommissionSettlementEntryEntity> toEntryEntities(
            @NonNull CommissionSettlement model
    ) {
        List<CommissionSettlementEntry> entries = model.entries();
        List<CommissionSettlementEntryEntity> list = new ArrayList<>(entries.size());
        for (CommissionSettlementEntry entry : entries) {
            list.add(new CommissionSettlementEntryEntity(
                    entry.id(),
                    model.id(),
                    entry.appointmentId(),
                    entry.type().name(),
                    entry.baseCents(),
                    entry.commissionCents()
            ));
        }
        return list;
    }
}
