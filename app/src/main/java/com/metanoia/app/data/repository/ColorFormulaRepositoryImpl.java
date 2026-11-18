package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.dao.ColorFormulaDao;
import com.metanoia.app.data.db.pojo.ColorFormulaWithComponents;
import com.metanoia.app.data.mapper.ColorFormulaMapper;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.repository.ColorFormulaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link ColorFormulaRepository}.
 */
public final class ColorFormulaRepositoryImpl implements ColorFormulaRepository {

    private final ColorFormulaDao colorFormulaDao;

    public ColorFormulaRepositoryImpl(@NonNull ColorFormulaDao colorFormulaDao) {
        this.colorFormulaDao = Objects.requireNonNull(colorFormulaDao, "colorFormulaDao");
    }

    @Override
    public Optional<ColorFormula> findById(String id) {
        ColorFormulaWithComponents row = colorFormulaDao.findByIdWithComponentsSync(id);
        if (row == null || row.components == null || row.components.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ColorFormulaMapper.toDomain(row));
    }

    @Override
    public List<ColorFormula> findActiveByClientId(String clientId) {
        return ColorFormulaMapper.toDomainList(
                colorFormulaDao.findActiveByClientIdSync(clientId));
    }

    @Override
    public Optional<ColorFormula> findLatestActiveByClientId(String clientId) {
        ColorFormulaWithComponents row = colorFormulaDao.findLatestActiveByClientIdSync(clientId);
        if (row == null || row.components == null || row.components.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ColorFormulaMapper.toDomain(row));
    }

    @Override
    public boolean isLinkedToClosedAppointment(String formulaId) {
        return colorFormulaDao.isLinkedToClosedAppointmentSync(formulaId);
    }

    @Override
    public void save(ColorFormula formula) {
        colorFormulaDao.saveFormulaWithComponents(
                ColorFormulaMapper.toEntity(formula),
                ColorFormulaMapper.toComponentEntities(formula)
        );
    }

    @Override
    public void softDelete(String formulaId, long deletedAtMillis) {
        colorFormulaDao.softDeleteById(formulaId, deletedAtMillis);
    }
}
