package com.metanoia.domain.rules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.SalonServiceRepository;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProfessionalValidatorTest {

    @Test
    public void isValidCommissionBasisPoints_acceptsZeroTo100PercentOnly() {
        assertTrue(ProfessionalValidator.isValidCommissionBasisPoints(0));
        assertTrue(ProfessionalValidator.isValidCommissionBasisPoints(4_550));
        assertTrue(ProfessionalValidator.isValidCommissionBasisPoints(10_000));

        assertFalse(ProfessionalValidator.isValidCommissionBasisPoints(-1));
        assertFalse(ProfessionalValidator.isValidCommissionBasisPoints(10_001));
    }

    @Test
    public void areValidWorkSchedules_rejectsOverlapAndOutsideSalonHours() {
        List<TimeRange> salon = Arrays.asList(
                new TimeRange(1, 540, 1200),
                new TimeRange(2, 540, 1200)
        );

        assertTrue(ProfessionalValidator.areValidWorkSchedules(
                Arrays.asList(new TimeRange(1, 540, 780), new TimeRange(1, 840, 1140)),
                salon));

        assertFalse(ProfessionalValidator.areValidWorkSchedules(
                Arrays.asList(new TimeRange(1, 540, 900), new TimeRange(1, 840, 1140)),
                salon));

        assertFalse(ProfessionalValidator.areValidWorkSchedules(
                Collections.singletonList(new TimeRange(1, 480, 1140)),
                salon));

        assertFalse(ProfessionalValidator.areValidWorkSchedules(
                Collections.singletonList(new TimeRange(7, 600, 900)),
                salon));
    }

    @Test
    public void areAllServicesActive_rejectsInactiveOrMissingServices() {
        Map<String, SalonService> map = new HashMap<>();
        map.put("active-1", new SalonService.Builder()
                .id("active-1").name("Corte").category("Corte")
                .price(Money.ofCents(1_000_00L)).durationMin(30).active(true).build());
        map.put("inactive-1", new SalonService.Builder()
                .id("inactive-1").name("Viejo").category("Corte")
                .price(Money.ofCents(1_000_00L)).durationMin(30).active(false).build());

        SalonServiceRepository repo = new SalonServiceRepository() {
            @Override
            public List<SalonService> findAll(boolean includeInactive) {
                return Collections.emptyList();
            }

            @Override
            public Optional<SalonService> findById(String id) {
                return Optional.ofNullable(map.get(id));
            }

            @Override
            public Optional<SalonService> findActiveByCategoryAndNormalizedName(
                    String normalizedCategory, String normalizedName) {
                return Optional.empty();
            }

            @Override
            public void save(SalonService service) {
            }
        };

        assertTrue(ProfessionalValidator.areAllServicesActive(
                Collections.singleton("active-1"), repo));
        assertFalse(ProfessionalValidator.areAllServicesActive(
                new HashSet<>(Arrays.asList("active-1", "inactive-1")), repo));
        assertFalse(ProfessionalValidator.areAllServicesActive(
                Collections.singleton("missing"), repo));
    }
}
