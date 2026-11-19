package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinLockPolicyTest {

    @Test
    public void shouldLockOnResume_enforcesColdStartAndConfiguredTimeout() {
        assertFalse(PinLockPolicy.shouldLockOnResume(false, false, 0L, 10_000L, 5));
        assertTrue(PinLockPolicy.shouldLockOnResume(true, false, 0L, 10_000L, 5));
        assertFalse(PinLockPolicy.shouldLockOnResume(true, true, 0L, 10_000L, 5));
        assertTrue(PinLockPolicy.shouldLockOnResume(true, true, 1_000L, 1_001L, 0));
        assertFalse(PinLockPolicy.shouldLockOnResume(true, true, 1_000L, 299_999L, 5));
        assertTrue(PinLockPolicy.shouldLockOnResume(true, true, 1_000L, 301_000L, 5));
    }

    @Test
    public void cooldownDurationMs_triggersThirtySecondIncrementalLockoutAtFifthFailure() {
        assertEquals(0L, PinLockPolicy.cooldownDurationMs(1));
        assertEquals(0L, PinLockPolicy.cooldownDurationMs(4));
        assertEquals(30_000L, PinLockPolicy.cooldownDurationMs(5));
        assertEquals(60_000L, PinLockPolicy.cooldownDurationMs(6));
        assertEquals(PinLockPolicy.MAX_COOLDOWN_MS, PinLockPolicy.cooldownDurationMs(100));
        assertEquals(15_000L, PinLockPolicy.remainingLockoutMs(25_000L, 10_000L));
        assertEquals(0L, PinLockPolicy.remainingLockoutMs(10_000L, 10_000L));
    }
}
