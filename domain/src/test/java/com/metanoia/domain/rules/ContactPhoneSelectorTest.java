package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.ContactPhone;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class ContactPhoneSelectorTest {

    @Test
    public void selectPrimary_prefersFirstValidMobileOverLandlineOrOther() {
        Optional<String> selected = ContactPhoneSelector.selectPrimaryNormalizedPhone(Arrays.asList(
                new ContactPhone("011 4321-0000", false),
                new ContactPhone("123", true),
                new ContactPhone("011 15-5566-7788", true),
                new ContactPhone("+54 9 11 9988-7766", true)
        ));

        assertTrue(selected.isPresent());
        assertEquals("5491155667788", selected.get());
    }

    @Test
    public void selectPrimary_whenNoMobileExists_picksFirstValidAvailable() {
        Optional<String> selected = ContactPhoneSelector.selectPrimaryNormalizedPhone(Arrays.asList(
                new ContactPhone("abc", false),
                new ContactPhone("351 444-5566", false),
                new ContactPhone("11 2233-4455", false)
        ));

        assertTrue(selected.isPresent());
        assertEquals("5493514445566", selected.get());
    }

    @Test
    public void selectPrimary_whenAllInvalidOrEmpty_returnsEmpty() {
        assertFalse(ContactPhoneSelector.selectPrimaryNormalizedPhone(Collections.emptyList()).isPresent());
        assertFalse(ContactPhoneSelector.selectPrimaryNormalizedPhone(
                Collections.singletonList(new ContactPhone("911", true))).isPresent());
    }
}
