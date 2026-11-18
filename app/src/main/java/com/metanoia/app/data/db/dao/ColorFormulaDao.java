package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.metanoia.app.data.db.entity.ColorFormulaComponentEntity;
import com.metanoia.app.data.db.entity.ColorFormulaEntity;
import com.metanoia.app.data.db.pojo.ColorFormulaWithComponents;

import java.util.List;

/**
 * DAO de fichas de color con consultas ordenadas por fecha descendente y baja lógica.
 */
@Dao
public interface ColorFormulaDao {

    @Transaction
    @Query("SELECT * FROM color_formulas WHERE id = :id LIMIT 1")
    ColorFormulaWithComponents findByIdWithComponentsSync(String id);

    @Transaction
    @Query("SELECT * FROM color_formulas "
            + "WHERE client_id = :clientId AND deleted_at IS NULL "
            + "ORDER BY day DESC, created_at DESC, id DESC")
    List<ColorFormulaWithComponents> findActiveByClientIdSync(String clientId);

    @Transaction
    @Query("SELECT * FROM color_formulas "
            + "WHERE client_id = :clientId AND deleted_at IS NULL "
            + "ORDER BY day DESC, created_at DESC, id DESC LIMIT 1")
    ColorFormulaWithComponents findLatestActiveByClientIdSync(String clientId);

    @Transaction
    @Query("SELECT * FROM color_formulas "
            + "WHERE client_id = :clientId AND deleted_at IS NULL "
            + "ORDER BY day DESC, created_at DESC, id DESC")
    LiveData<List<ColorFormulaWithComponents>> observeActiveByClientId(String clientId);

    @Transaction
    @Query("SELECT * FROM color_formulas "
            + "WHERE client_id = :clientId AND deleted_at IS NULL "
            + "ORDER BY day DESC, created_at DESC, id DESC LIMIT 1")
    LiveData<ColorFormulaWithComponents> observeLatestActiveByClientId(String clientId);

    @Query("SELECT EXISTS("
            + "SELECT 1 FROM color_formulas cf "
            + "LEFT JOIN appointments a1 ON a1.id = cf.appointment_id "
            + "LEFT JOIN appointment_items ai ON ai.color_formula_id = cf.id "
            + "LEFT JOIN appointments a2 ON a2.id = ai.appointment_id "
            + "WHERE cf.id = :formulaId "
            + "AND (a1.status IN ('COMPLETED', 'PAID') OR a2.status IN ('COMPLETED', 'PAID'))"
            + ")")
    boolean isLinkedToClosedAppointmentSync(String formulaId);

    @Query("SELECT DISTINCT cf.id FROM color_formulas cf "
            + "LEFT JOIN appointments a1 ON a1.id = cf.appointment_id "
            + "LEFT JOIN appointment_items ai ON ai.color_formula_id = cf.id "
            + "LEFT JOIN appointments a2 ON a2.id = ai.appointment_id "
            + "WHERE cf.client_id = :clientId AND cf.deleted_at IS NULL "
            + "AND (a1.status IN ('COMPLETED', 'PAID') OR a2.status IN ('COMPLETED', 'PAID'))")
    LiveData<List<String>> observeClosedFormulaIdsByClient(String clientId);

    @Query("UPDATE color_formulas SET deleted_at = :deletedAt WHERE id = :id")
    void softDeleteById(String id, long deletedAt);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertFormulaIgnore(ColorFormulaEntity entity);

    @Update
    void updateFormula(ColorFormulaEntity entity);

    @Query("DELETE FROM color_formula_components WHERE formula_id = :formulaId")
    void deleteComponentsForFormula(String formulaId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComponents(List<ColorFormulaComponentEntity> components);

    @Transaction
    default void saveFormulaWithComponents(
            ColorFormulaEntity entity,
            List<ColorFormulaComponentEntity> components
    ) {
        if (insertFormulaIgnore(entity) == -1L) {
            updateFormula(entity);
        }
        deleteComponentsForFormula(entity.id);
        if (!components.isEmpty()) {
            insertComponents(components);
        }
    }
}
