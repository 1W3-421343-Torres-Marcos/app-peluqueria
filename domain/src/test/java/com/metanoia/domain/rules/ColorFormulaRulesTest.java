package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.ColorComponentUnit;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;
import com.metanoia.domain.rules.ColorFormulaRules.ComponentDraft;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorFormulaRulesTest {

    @Test
    public void validateAndNormalizeComponents_listaVaciaOSinMarcaOTono_fallaValidacion() {
        IdGenerator ids = sequentialIds();

        Result<List<ColorFormulaComponent>> emptyRes = ColorFormulaRules
                .validateAndNormalizeComponents("f-1", Collections.emptyList(), ids);
        assertTrue(emptyRes.isFailure());

        Result<List<ColorFormulaComponent>> blankBrandRes = ColorFormulaRules
                .validateAndNormalizeComponents(
                        "f-1",
                        Collections.singletonList(
                                new ComponentDraft(" ", null, "7.1", 300, ColorComponentUnit.GRAMS)),
                        ids);
        assertTrue(blankBrandRes.isFailure());

        Result<List<ColorFormulaComponent>> zeroAmountRes = ColorFormulaRules
                .validateAndNormalizeComponents(
                        "f-1",
                        Collections.singletonList(
                                new ComponentDraft("Wella", null, "7.1", 0, ColorComponentUnit.GRAMS)),
                        ids);
        assertTrue(zeroAmountRes.isFailure());
    }

    @Test
    public void validateAndNormalizeComponents_datosValidos_asignaPosicionesYParseaDecimas() {
        IdGenerator ids = sequentialIds();
        assertEquals(Optional.of(305), ColorFormulaComponent.parseAmountTenths("30,5"));
        assertEquals(Optional.of(450), ColorFormulaComponent.parseAmountTenths("45"));
        assertFalse(ColorFormulaComponent.parseAmountTenths("0").isPresent());
        assertEquals(Optional.of(ColorComponentUnit.MILLILITERS),
                ColorComponentUnit.fromString("ml"));

        Result<List<ColorFormulaComponent>> result = ColorFormulaRules
                .validateAndNormalizeComponents(
                        "f-1",
                        Arrays.asList(
                                new ComponentDraft("L'Oréal", "Majirel", "7.1", 300,
                                        ColorComponentUnit.GRAMS),
                                new ComponentDraft("L'Oréal", null, "7.0", 305,
                                        ColorComponentUnit.GRAMS)
                        ),
                        ids
                );

        assertTrue(result.isSuccess());
        List<ColorFormulaComponent> components = result.getValue();
        assertEquals(2, components.size());
        assertEquals(0, components.get(0).position());
        assertEquals("30", components.get(0).formattedAmount());
        assertEquals(1, components.get(1).position());
        assertEquals("30,5", components.get(1).formattedAmount());
        assertEquals("7.1 (30 g)", components.get(0).summaryPart());
    }

    @Test
    public void selectLatestActiveFormula_ignoraEliminadasYOrdenaPorFechaDescendente() {
        ColorFormula older = sampleFormula("f-old", 20_500L, 1000L, null);
        ColorFormula newestDeleted = sampleFormula("f-del", 20_520L, 3000L, 4000L);
        ColorFormula latestDayEarly = sampleFormula("f-a", 20_510L, 1000L, null);
        ColorFormula latestDayLate = sampleFormula("f-b", 20_510L, 2000L, null);

        Optional<ColorFormula> selected = ColorFormulaRules.selectLatestActiveFormula(
                Arrays.asList(older, newestDeleted, latestDayEarly, latestDayLate));

        assertTrue(selected.isPresent());
        assertEquals("f-b", selected.get().id());
    }

    @Test
    public void isLockedByAppointmentStatus_soloCompletedYPaidSonInmutables() {
        assertTrue(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.COMPLETED)));
        assertTrue(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.PAID)));
        assertFalse(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.PENDING)));
        assertFalse(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.CONFIRMED)));
        assertFalse(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.CANCELLED)));
        assertFalse(ColorFormulaRules.isLockedByAppointmentStatus(
                Optional.of(AppointmentStatus.NO_SHOW)));
        assertFalse(ColorFormulaRules.isLockedByAppointmentStatus(Optional.empty()));
    }

    @Test
    public void duplicateAsNewDraft_copiaComponentesConNuevosIdsYVincularTurnoDestino() {
        ColorFormula source = sampleFormula("f-orig", 20_500L, 1000L, null)
                .toBuilder()
                .appointmentId("apt-old")
                .professionalId("pro-1")
                .build();

        ColorFormula copy = ColorFormulaRules.duplicateAsNewDraft(
                source, "f-copy", "apt-new", "pro-2", 20_520L, 9999L, sequentialIds());

        assertEquals("f-copy", copy.id());
        assertEquals(Optional.of("apt-new"), copy.appointmentId());
        assertEquals(Optional.of("pro-2"), copy.professionalId());
        assertEquals(20_520L, copy.day());
        assertEquals(9999L, copy.createdAt());
        assertEquals(source.components().size(), copy.components().size());
        assertEquals("f-copy", copy.components().get(0).formulaId());
        assertNotEquals(source.components().get(0).id(), copy.components().get(0).id());
    }

    private static ColorFormula sampleFormula(String id, long day, long createdAt, Long deletedAt) {
        return new ColorFormula.Builder()
                .id(id)
                .clientId("cli-1")
                .day(day)
                .exposureMin(35)
                .developerVolume("20 vol")
                .createdAt(createdAt)
                .deletedAt(deletedAt)
                .components(Collections.singletonList(new ColorFormulaComponent.Builder()
                        .id("cmp-" + id)
                        .formulaId(id)
                        .brand("Wella")
                        .shade("7.1")
                        .amountTenths(300)
                        .unit(ColorComponentUnit.GRAMS)
                        .position(0)
                        .build()))
                .build();
    }

    private static IdGenerator sequentialIds() {
        AtomicInteger seq = new AtomicInteger(1);
        return () -> "id-" + seq.getAndIncrement();
    }
}
