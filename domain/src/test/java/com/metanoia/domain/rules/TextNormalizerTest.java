package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextNormalizerTest {

    @Test
    public void normalize_accentsUmlautsAndCase_returnsLowerAscii() {
        assertEquals("maria jose", TextNormalizer.normalize("María José"));
        assertEquals("nandu aguero", TextNormalizer.normalize("Ñandú Agüero"));
        assertEquals("lucia perez", TextNormalizer.normalize("  Lucía   PÉREZ  "));
    }

    @Test
    public void normalize_nullOrBlank_returnsEmptyString() {
        assertEquals("", TextNormalizer.normalize(null));
        assertEquals("", TextNormalizer.normalize("   "));
    }
}
