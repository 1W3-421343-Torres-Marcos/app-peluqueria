package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;
import com.metanoia.domain.rules.WaitlistMatcher.MatchedCandidate;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WaitlistMatcherTest {

    // Miércoles 18/11/2026
    private static final long TODAY = LocalDate.of(2026, 11, 18).toEpochDay();
    // Viernes 20/11/2026 (ISO day of week = 5)
    private static final long FRIDAY = TODAY + 2L;

    private Professional ana;
    private Map<String, Client> clientsById;
    private Map<String, SalonService> servicesById;

    @Before
    public void setUp() {
        ana = new Professional.Builder()
                .id("pro-ana")
                .name("Ana")
                .active(true)
                .serviceIds(new LinkedHashSet<>(Arrays.asList("svc-cut", "svc-brush")))
                .build();

        clientsById = new LinkedHashMap<>();
        clientsById.put("cli-zero-ns", client("cli-zero-ns", "Sofía", 0));
        clientsById.put("cli-two-ns", client("cli-two-ns", "Lucía", 2));
        clientsById.put("cli-one-ns", client("cli-one-ns", "Carla", 1));

        servicesById = new LinkedHashMap<>();
        servicesById.put("svc-cut", service("svc-cut", "Corte", 30));
        servicesById.put("svc-brush", service("svc-brush", "Brushing", 20)); // redondea a 60 con corte (50 -> 60 en bloques de 15)
        servicesById.put("svc-color", service("svc-color", "Color", 90));
    }

    @Test
    public void match_ordenaPorProfesionalExactoLuegoAntiguedadLuegoMenosAusencias() {
        FreedSlot slot = new FreedSlot("pro-ana", FRIDAY, 600, 660); // 10:00 - 11:00 (60 min)

        WaitlistEntry anyOldZero = entry("e-any-old", "cli-zero-ns", null,
                Collections.singleton(5), 540, 780, FRIDAY + 5, WaitlistStatus.ACTIVE, 100L);
        WaitlistEntry exactLate = entry("e-exact-late", "cli-zero-ns", "pro-ana",
                Collections.singleton(5), 540, 780, FRIDAY + 5, WaitlistStatus.ACTIVE, 500L);
        WaitlistEntry exactEarlyHighNoShow = entry("e-exact-early-2ns", "cli-two-ns", "pro-ana",
                Collections.singleton(5), 540, 780, FRIDAY + 5, WaitlistStatus.ACTIVE, 200L);
        WaitlistEntry exactEarlyLowNoShow = entry("e-exact-early-1ns", "cli-one-ns", "pro-ana",
                Collections.singleton(5), 540, 780, FRIDAY + 5, WaitlistStatus.ACTIVE, 200L);

        List<MatchedCandidate> result = WaitlistMatcher.match(
                slot, ana, TODAY, 15,
                Arrays.asList(anyOldZero, exactLate, exactEarlyHighNoShow, exactEarlyLowNoShow),
                clientsById, servicesById);

        assertEquals(4, result.size());
        assertEquals("e-exact-early-1ns", result.get(0).entry().id());
        assertEquals("e-exact-early-2ns", result.get(1).entry().id());
        assertEquals("e-exact-late", result.get(2).entry().id());
        assertEquals("e-any-old", result.get(3).entry().id());
        assertTrue(result.get(0).exactProfessionalMatch());
    }

    @Test
    public void match_excluyeFranjaIncompatibleDuracionInsuficienteServiciosYEstados() {
        FreedSlot slot = new FreedSlot("pro-ana", FRIDAY, 600, 645); // 10:00 - 10:45 (45 min)

        // Cabe Corte (30 min) dentro de [600, 660]
        WaitlistEntry validSpecificDay = new WaitlistEntry.Builder()
                .id("e-ok")
                .clientId("cli-zero-ns")
                .serviceIds(Collections.singletonList("svc-cut"))
                .specificDay(FRIDAY)
                .fromMin(600)
                .toMin(720)
                .validUntilDay(FRIDAY)
                .status(WaitlistStatus.ACTIVE)
                .createdAt(100L)
                .build();

        // Excluida: pide Corte + Brushing = 50 -> 60 min > 45 min del hueco
        WaitlistEntry tooLong = new WaitlistEntry.Builder()
                .id("e-long")
                .clientId("cli-zero-ns")
                .serviceIds(Arrays.asList("svc-cut", "svc-brush"))
                .specificDay(FRIDAY)
                .fromMin(540)
                .toMin(720)
                .validUntilDay(FRIDAY)
                .status(WaitlistStatus.ACTIVE)
                .createdAt(50L)
                .build();

        // Excluida: franja termina a las 10:25 (< 10:00 + 30 min = 10:30)
        WaitlistEntry tightWindow = entry("e-window", "cli-one-ns", null,
                Collections.singleton(5), 540, 625, FRIDAY + 5, WaitlistStatus.ACTIVE, 50L);

        // Excluida: otro profesional o servicio no habilitado
        WaitlistEntry otherPro = entry("e-pro", "cli-one-ns", "pro-otro",
                Collections.singleton(5), 540, 720, FRIDAY + 5, WaitlistStatus.ACTIVE, 50L);
        WaitlistEntry unsupportedSvc = new WaitlistEntry.Builder()
                .id("e-svc")
                .clientId("cli-one-ns")
                .serviceIds(Collections.singletonList("svc-color"))
                .preferredDaysOfWeek(Collections.singleton(5))
                .fromMin(540)
                .toMin(720)
                .validUntilDay(FRIDAY + 5)
                .status(WaitlistStatus.ACTIVE)
                .createdAt(50L)
                .build();

        // Excluida: vencida y OFFERED
        WaitlistEntry expired = entry("e-exp", "cli-one-ns", null,
                Collections.singleton(5), 540, 720, TODAY - 1, WaitlistStatus.ACTIVE, 50L);
        WaitlistEntry alreadyOffered = entry("e-off", "cli-one-ns", null,
                Collections.singleton(5), 540, 720, FRIDAY + 5, WaitlistStatus.OFFERED, 50L);

        List<MatchedCandidate> result = WaitlistMatcher.match(
                slot, ana, TODAY, 15,
                Arrays.asList(validSpecificDay, tooLong, tightWindow, otherPro, unsupportedSvc,
                        expired, alreadyOffered),
                clientsById, servicesById);

        assertEquals(1, result.size());
        assertEquals("e-ok", result.get(0).entry().id());
    }

    private static WaitlistEntry entry(
            String id,
            String clientId,
            String professionalId,
            Set<Integer> weekdays,
            int fromMin,
            int toMin,
            long validUntil,
            WaitlistStatus status,
            long createdAt
    ) {
        return new WaitlistEntry.Builder()
                .id(id)
                .clientId(clientId)
                .serviceIds(Collections.singletonList("svc-cut"))
                .professionalId(professionalId)
                .preferredDaysOfWeek(weekdays)
                .fromMin(fromMin)
                .toMin(toMin)
                .validUntilDay(validUntil)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private static Client client(String id, String name, int noShowCount) {
        return new Client.Builder()
                .id(id)
                .name(name)
                .nameNormalized(name.toLowerCase())
                .phone("5491122334455")
                .noShowCount(noShowCount)
                .active(true)
                .build();
    }

    private static SalonService service(String id, String name, int durationMin) {
        return new SalonService.Builder()
                .id(id)
                .name(name)
                .category(name)
                .price(Money.ofCents(1_500_000L))
                .durationMin(durationMin)
                .active(true)
                .build();
    }
}
