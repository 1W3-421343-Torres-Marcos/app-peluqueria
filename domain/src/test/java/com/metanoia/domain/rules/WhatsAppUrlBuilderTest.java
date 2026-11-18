package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class WhatsAppUrlBuilderTest {

    @Test
    public void buildsEncodedUrlPreservingSpanishAccentsNewlinesAndSpaces() throws Exception {
        String message = "Hola Sofía, te recuerdo tu turno el miércoles.\n— Metanoia Hair Salon";

        Optional<String> urlOpt = WhatsAppUrlBuilder.buildUrl("+54 9 11 2233-4455", message);

        assertTrue(urlOpt.isPresent());
        String url = urlOpt.get();
        assertTrue(url.startsWith("https://wa.me/5491122334455?text="));
        assertFalse("Espacios deben codificarse como %20 y no '+'", url.contains("+"));

        String queryEncoded = url.substring("https://wa.me/5491122334455?text=".length());
        String decoded = URLDecoder.decode(queryEncoded, StandardCharsets.UTF_8.name());
        assertEquals(message, decoded);
    }

    @Test
    public void rejectsMissingOrTooShortPhone() {
        assertFalse(WhatsAppUrlBuilder.hasValidPhone(null));
        assertFalse(WhatsAppUrlBuilder.hasValidPhone(""));
        assertFalse(WhatsAppUrlBuilder.hasValidPhone("12345"));
        assertFalse(WhatsAppUrlBuilder.buildUrl("   ", "Hola").isPresent());
    }
}
