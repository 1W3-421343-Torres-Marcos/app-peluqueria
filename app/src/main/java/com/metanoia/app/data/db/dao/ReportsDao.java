package com.metanoia.app.data.db.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;

import com.metanoia.app.data.db.projection.FrequentClientProjection;
import com.metanoia.app.data.db.projection.IncomeByDayProjection;
import com.metanoia.app.data.db.projection.IncomeByMethodProjection;
import com.metanoia.app.data.db.projection.OccupiedSlotProjection;
import com.metanoia.app.data.db.projection.ProfessionalPerformanceProjection;
import com.metanoia.app.data.db.projection.ReceivableClientProjection;
import com.metanoia.app.data.db.projection.ReceivableTotalsProjection;
import com.metanoia.app.data.db.projection.TopServiceCategoryProjection;
import com.metanoia.app.data.db.projection.TopServiceProjection;

import java.util.List;

/**
 * DAO de consultas agregadas SQL (GROUP BY / CTEs) acotadas por rango de fechas para reportes.
 */
@Dao
public interface ReportsDao {

    @Query("SELECT cm.day AS day, "
            + "COALESCE(SUM(CASE "
            + "WHEN cm.type IN ('APPOINTMENT_PAYMENT', 'PRODUCT_SALE', 'OTHER_INCOME') THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' AND COALESCE(orig.type, 'APPOINTMENT_PAYMENT') NOT IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN -cm.amount_cents "
            + "ELSE 0 END), 0) AS income_cents, "
            + "COALESCE(SUM(CASE "
            + "WHEN cm.type IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' AND orig.type IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN -cm.amount_cents "
            + "ELSE 0 END), 0) AS expense_cents, "
            + "COUNT(*) AS movement_count "
            + "FROM cash_movements cm "
            + "LEFT JOIN cash_movements orig ON orig.id = cm.reverses_movement_id "
            + "LEFT JOIN appointments a ON :professionalId IS NOT NULL AND a.id = cm.appointment_id "
            + "LEFT JOIN commission_settlements cs ON :professionalId IS NOT NULL "
            + "AND cs.cash_movement_id = COALESCE(cm.reverses_movement_id, cm.id) "
            + "WHERE cm.day BETWEEN :fromDay AND :toDay "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId OR cs.professional_id = :professionalId) "
            + "GROUP BY cm.day "
            + "ORDER BY cm.day ASC")
    List<IncomeByDayProjection> queryIncomeByDaySync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("SELECT cm.method AS method, "
            + "COALESCE(SUM(CASE "
            + "WHEN cm.type IN ('APPOINTMENT_PAYMENT', 'PRODUCT_SALE', 'OTHER_INCOME') THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' AND COALESCE(orig.type, 'APPOINTMENT_PAYMENT') NOT IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN -cm.amount_cents "
            + "ELSE 0 END), 0) AS income_cents, "
            + "COALESCE(SUM(CASE "
            + "WHEN cm.type IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' AND orig.type IN ('EXPENSE', 'COMMISSION_PAYOUT') THEN -cm.amount_cents "
            + "ELSE 0 END), 0) AS expense_cents, "
            + "COUNT(*) AS movement_count "
            + "FROM cash_movements cm "
            + "LEFT JOIN cash_movements orig ON orig.id = cm.reverses_movement_id "
            + "LEFT JOIN appointments a ON :professionalId IS NOT NULL AND a.id = cm.appointment_id "
            + "LEFT JOIN commission_settlements cs ON :professionalId IS NOT NULL "
            + "AND cs.cash_movement_id = COALESCE(cm.reverses_movement_id, cm.id) "
            + "WHERE cm.day BETWEEN :fromDay AND :toDay "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId OR cs.professional_id = :professionalId) "
            + "GROUP BY cm.method "
            + "ORDER BY income_cents DESC, cm.method ASC")
    List<IncomeByMethodProjection> queryIncomeByMethodSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("SELECT ai.service_id AS service_id, "
            + "ai.service_name_snapshot AS service_name, "
            + "COALESCE(ss.category, '') AS category, "
            + "COUNT(*) AS service_count, "
            + "COALESCE(SUM(ai.price_cents_snapshot), 0) AS revenue_cents "
            + "FROM appointment_items ai "
            + "INNER JOIN appointments a ON a.id = ai.appointment_id "
            + "LEFT JOIN salon_services ss ON ss.id = ai.service_id "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('COMPLETED', 'PAID') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY ai.service_id, ai.service_name_snapshot, COALESCE(ss.category, '') "
            + "ORDER BY service_count DESC, revenue_cents DESC, service_name COLLATE NOCASE ASC")
    List<TopServiceProjection> queryTopServicesSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("SELECT COALESCE(ss.category, '') AS category, "
            + "COUNT(*) AS service_count, "
            + "COALESCE(SUM(ai.price_cents_snapshot), 0) AS revenue_cents "
            + "FROM appointment_items ai "
            + "INNER JOIN appointments a ON a.id = ai.appointment_id "
            + "LEFT JOIN salon_services ss ON ss.id = ai.service_id "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('COMPLETED', 'PAID') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY COALESCE(ss.category, '') "
            + "ORDER BY revenue_cents DESC, service_count DESC, category COLLATE NOCASE ASC")
    List<TopServiceCategoryProjection> queryTopServiceCategoriesSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("WITH appt_totals AS ("
            + "SELECT a.id AS appointment_id, a.client_id AS client_id, a.day AS day, "
            + "MAX(0, COALESCE(SUM(ai.price_cents_snapshot), 0) + a.adjustment_cents) AS total_cents "
            + "FROM appointments a "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('COMPLETED', 'PAID') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id) "
            + "SELECT c.id AS client_id, c.name AS client_name, c.phone AS phone, "
            + "COUNT(*) AS visit_count, "
            + "COALESCE(SUM(t.total_cents), 0) AS total_spent_cents, "
            + "MAX(t.day) AS last_visit_day "
            + "FROM appt_totals t "
            + "INNER JOIN clients c ON c.id = t.client_id "
            + "GROUP BY c.id, c.name, c.phone "
            + "ORDER BY visit_count DESC, total_spent_cents DESC, last_visit_day DESC, c.name COLLATE NOCASE ASC "
            + "LIMIT :limit")
    List<FrequentClientProjection> queryFrequentClientsByVisitsSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId,
            int limit
    );

    @Query("WITH appt_totals AS ("
            + "SELECT a.id AS appointment_id, a.client_id AS client_id, a.day AS day, "
            + "MAX(0, COALESCE(SUM(ai.price_cents_snapshot), 0) + a.adjustment_cents) AS total_cents "
            + "FROM appointments a "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('COMPLETED', 'PAID') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id) "
            + "SELECT c.id AS client_id, c.name AS client_name, c.phone AS phone, "
            + "COUNT(*) AS visit_count, "
            + "COALESCE(SUM(t.total_cents), 0) AS total_spent_cents, "
            + "MAX(t.day) AS last_visit_day "
            + "FROM appt_totals t "
            + "INNER JOIN clients c ON c.id = t.client_id "
            + "GROUP BY c.id, c.name, c.phone "
            + "ORDER BY total_spent_cents DESC, visit_count DESC, last_visit_day DESC, c.name COLLATE NOCASE ASC "
            + "LIMIT :limit")
    List<FrequentClientProjection> queryFrequentClientsBySpendSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId,
            int limit
    );

    @Query("WITH RECURSIVE hours(slot_hour) AS ("
            + "SELECT 0 UNION ALL SELECT slot_hour + 1 FROM hours WHERE slot_hour < 23) "
            + "SELECT ((((a.day + 3) % 7) + 7) % 7) + 1 AS day_of_week, "
            + "h.slot_hour AS slot_hour, "
            + "COALESCE(SUM(MAX(0, MIN(a.end_min, (h.slot_hour + 1) * 60) - MAX(a.start_min, h.slot_hour * 60))), 0) AS occupied_minutes, "
            + "COUNT(*) AS appointment_count "
            + "FROM appointments a "
            + "INNER JOIN hours h ON a.start_min < (h.slot_hour + 1) * 60 AND a.end_min > h.slot_hour * 60 "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'PAID') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY day_of_week, h.slot_hour "
            + "ORDER BY day_of_week ASC, h.slot_hour ASC")
    List<OccupiedSlotProjection> queryOccupiedSlotMinutesSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("WITH appt_subtotals AS ("
            + "SELECT a.id AS id, a.professional_id AS professional_id, a.status AS status, "
            + "a.adjustment_cents AS adjustment_cents, "
            + "COALESCE(SUM(ai.price_cents_snapshot), 0) AS subtotal_cents, "
            + "MAX(0, COALESCE(SUM(ai.price_cents_snapshot), 0) + a.adjustment_cents) AS total_cents "
            + "FROM appointments a "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.day BETWEEN :fromDay AND :toDay "
            + "AND a.status IN ('COMPLETED', 'PAID', 'NO_SHOW') "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id), "
            + "appt_metrics AS ("
            + "SELECT s.professional_id AS professional_id, "
            + "SUM(CASE WHEN s.status IN ('COMPLETED', 'PAID') THEN 1 ELSE 0 END) AS completed_count, "
            + "COALESCE(SUM(CASE WHEN s.status IN ('COMPLETED', 'PAID') THEN s.total_cents ELSE 0 END), 0) AS billed_cents, "
            + "SUM(CASE WHEN s.status = 'NO_SHOW' THEN 1 ELSE 0 END) AS no_show_count "
            + "FROM appt_subtotals s "
            + "GROUP BY s.professional_id), "
            + "appt_commissions AS ("
            + "SELECT s.professional_id AS professional_id, "
            + "COALESCE(SUM(CASE "
            + "WHEN s.total_cents <= 0 OR COALESCE(ai.commission_bp_snapshot, 0) <= 0 THEN 0 "
            + "WHEN s.adjustment_cents = 0 THEN ((ai.price_cents_snapshot * ai.commission_bp_snapshot) + 5000) / 10000 "
            + "WHEN s.subtotal_cents > 0 THEN CAST(ROUND((CAST(ai.price_cents_snapshot AS REAL) * CAST(s.total_cents AS REAL) * CAST(ai.commission_bp_snapshot AS REAL)) / (CAST(s.subtotal_cents AS REAL) * 10000.0)) AS INTEGER) "
            + "ELSE 0 END), 0) AS commission_cents "
            + "FROM appt_subtotals s "
            + "INNER JOIN appointment_items ai ON ai.appointment_id = s.id "
            + "WHERE s.status = 'PAID' "
            + "GROUP BY s.professional_id) "
            + "SELECT p.id AS professional_id, p.name AS professional_name, "
            + "COALESCE(m.completed_count, 0) AS completed_count, "
            + "COALESCE(m.billed_cents, 0) AS billed_cents, "
            + "COALESCE(m.no_show_count, 0) AS no_show_count, "
            + "COALESCE(c.commission_cents, 0) AS commission_cents "
            + "FROM professionals p "
            + "LEFT JOIN appt_metrics m ON m.professional_id = p.id "
            + "LEFT JOIN appt_commissions c ON c.professional_id = p.id "
            + "WHERE (:professionalId IS NULL AND (p.active = 1 OR m.professional_id IS NOT NULL)) "
            + "OR p.id = :professionalId "
            + "ORDER BY billed_cents DESC, completed_count DESC, p.name COLLATE NOCASE ASC")
    List<ProfessionalPerformanceProjection> queryProfessionalPerformanceSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("WITH completed_balances AS ("
            + "SELECT a.id AS appointment_id, a.client_id AS client_id, a.day AS day, "
            + "MAX(0, MAX(0, COALESCE(SUM(ai.price_cents_snapshot), 0) + a.adjustment_cents) "
            + "- COALESCE((SELECT SUM(CASE "
            + "WHEN cm.type = 'APPOINTMENT_PAYMENT' THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' THEN -cm.amount_cents ELSE 0 END) "
            + "FROM cash_movements cm WHERE cm.appointment_id = a.id), 0)) AS balance_cents "
            + "FROM appointments a "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.status = 'COMPLETED' "
            + "AND a.day BETWEEN :fromDay AND :toDay "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id) "
            + "SELECT c.id AS client_id, c.name AS client_name, "
            + "COUNT(*) AS appointment_count, "
            + "COALESCE(SUM(cb.balance_cents), 0) AS balance_cents, "
            + "MIN(cb.day) AS oldest_day "
            + "FROM completed_balances cb "
            + "INNER JOIN clients c ON c.id = cb.client_id "
            + "WHERE cb.balance_cents > 0 "
            + "GROUP BY c.id, c.name "
            + "ORDER BY balance_cents DESC, oldest_day ASC, c.name COLLATE NOCASE ASC")
    List<ReceivableClientProjection> queryReceivablesByClientSync(
            long fromDay,
            long toDay,
            @Nullable String professionalId
    );

    @Query("WITH completed_balances AS ("
            + "SELECT a.id AS appointment_id, "
            + "MAX(0, MAX(0, COALESCE(SUM(ai.price_cents_snapshot), 0) + a.adjustment_cents) "
            + "- COALESCE((SELECT SUM(CASE "
            + "WHEN cm.type = 'APPOINTMENT_PAYMENT' THEN cm.amount_cents "
            + "WHEN cm.type = 'REVERSAL' THEN -cm.amount_cents ELSE 0 END) "
            + "FROM cash_movements cm WHERE cm.appointment_id = a.id), 0)) AS balance_cents "
            + "FROM appointments a "
            + "LEFT JOIN appointment_items ai ON ai.appointment_id = a.id "
            + "WHERE a.status = 'COMPLETED' "
            + "AND (:professionalId IS NULL OR a.professional_id = :professionalId) "
            + "GROUP BY a.id) "
            + "SELECT COALESCE(SUM(cb.balance_cents), 0) AS total_balance_cents, "
            + "COUNT(*) AS appointment_count "
            + "FROM completed_balances cb "
            + "WHERE cb.balance_cents > 0")
    ReceivableTotalsProjection queryHistoricalReceivablesTotalsSync(
            @Nullable String professionalId
    );
}
