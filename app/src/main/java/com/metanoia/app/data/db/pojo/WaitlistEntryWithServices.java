package com.metanoia.app.data.db.pojo;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.metanoia.app.data.db.entity.WaitlistEntryEntity;
import com.metanoia.app.data.db.entity.WaitlistEntryServiceCrossRef;

import java.util.List;

/**
 * Proyección Room de una entrada de lista de espera con sus servicios asociados.
 */
public final class WaitlistEntryWithServices {

    @Embedded
    public WaitlistEntryEntity entry;

    @Relation(parentColumn = "id", entityColumn = "entry_id")
    public List<WaitlistEntryServiceCrossRef> serviceRefs;
}
