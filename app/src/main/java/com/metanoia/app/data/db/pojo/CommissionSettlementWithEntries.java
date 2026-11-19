package com.metanoia.app.data.db.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.data.db.entity.CommissionSettlementEntryEntity;

import java.util.List;

/**
 * POJO Room con una liquidación histórica y sus renglones de turnos asociados.
 */
public final class CommissionSettlementWithEntries {

    @Embedded
    public CommissionSettlementEntity settlement;

    @Relation(parentColumn = "id", entityColumn = "settlement_id")
    public List<CommissionSettlementEntryEntity> entries;
}
