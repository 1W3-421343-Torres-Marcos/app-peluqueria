package com.metanoia.domain.repository;

import com.metanoia.domain.model.FrequentClientsReport;
import com.metanoia.domain.model.IncomeReport;
import com.metanoia.domain.model.OccupancyReport;
import com.metanoia.domain.model.ProfessionalPerformanceReport;
import com.metanoia.domain.model.ReceivablesReport;
import com.metanoia.domain.model.ReportFilter;
import com.metanoia.domain.model.TopServicesReport;

/**
 * Repositorio síncrono de consultas agregadas para los reportes del negocio.
 */
public interface ReportsRepository {

    int DEFAULT_TOP_CLIENTS_LIMIT = 20;

    IncomeReport getIncomeReport(ReportFilter filter);

    TopServicesReport getTopServicesReport(ReportFilter filter);

    FrequentClientsReport getFrequentClientsReport(ReportFilter filter, int limit);

    OccupancyReport getOccupancyReport(ReportFilter filter);

    ProfessionalPerformanceReport getProfessionalPerformanceReport(ReportFilter filter);

    ReceivablesReport getReceivablesReport(ReportFilter filter);
}
