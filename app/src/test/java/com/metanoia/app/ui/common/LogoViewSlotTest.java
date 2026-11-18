package com.metanoia.app.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogoViewSlotTest {

    @Test
    public void logoDrawableSlot_usesExpectedResourceName() {
        assertEquals("logo_metanoia", LogoView.LOGO_DRAWABLE_NAME);
    }
}
