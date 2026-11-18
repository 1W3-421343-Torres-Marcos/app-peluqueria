package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.model.Settings;
import com.metanoia.domain.rules.TemplateRenderer.RenderResult;
import com.metanoia.domain.rules.TemplateRenderer.TemplateContext;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

public class TemplateRendererTest {

    @Test
    public void rendersAllSupportedVariablesAndAccentedWeekdayWithoutDuplicateSignature() {
        long wednesday = LocalDate.of(2026, 11, 18).toEpochDay();
        TemplateContext context = new TemplateContext(
                "Sofía Martínez",
                wednesday,
                16 * 60 + 15,
                "Coloración, Brushing",
                "Lucía",
                "Metanoia Hair Salon");
        String template = "Hola {nombre}, tu turno del {dia} {fecha} a las {hora} para {servicios} "
                + "con {profesional} en {negocio} está confirmado.";

        RenderResult result = TemplateRenderer.render(template, context);

        assertEquals(
                "Hola Sofía, tu turno del miércoles 18/11/2026 a las 16:15 para Coloración, "
                        + "Brushing con Lucía en Metanoia Hair Salon está confirmado.",
                result.renderedText());
        assertTrue(result.unknownVariables().isEmpty());
    }

    @Test
    public void appendsSignatureWhenBusinessPlaceholderIsOmitted() {
        long saturday = LocalDate.of(2026, 11, 21).toEpochDay();
        TemplateContext context = new TemplateContext(
                "Carla Pérez",
                saturday,
                10 * 60,
                "Corte",
                "Ana",
                "Metanoia Hair Salon");

        RenderResult result = TemplateRenderer.render(
                "Hola {nombre}, nos vemos el {dia} a las {hora}.", context);

        assertEquals(
                "Hola Carla, nos vemos el sábado a las 10:00.\n— Metanoia Hair Salon",
                result.renderedText());
        assertTrue(result.unknownVariables().isEmpty());
    }

    @Test
    public void preservesAndReportsUnknownVariablesWhileKeepingValidOnes() {
        TemplateContext context = TemplateRenderer.sampleContext(Settings.defaultSettings());
        String template = "Hola {nombre}, sucursal {sucursal} código {codigo} ({sucursal}) en {negocio}.";

        RenderResult result = TemplateRenderer.render(template, context);

        assertEquals(
                "Hola Sofía, sucursal {sucursal} código {codigo} ({sucursal}) en Metanoia Hair Salon.",
                result.renderedText());
        assertEquals(Arrays.asList("{sucursal}", "{codigo}"), result.unknownVariables());
    }

    @Test
    public void rendersDefaultReminderAndBirthdayTemplatesCleanly() {
        Settings defaults = Settings.defaultSettings();
        TemplateContext context = TemplateRenderer.sampleContext(defaults);

        RenderResult reminder = TemplateRenderer.render(defaults.reminderTemplate(), context);
        RenderResult birthday = TemplateRenderer.render(defaults.birthdayTemplate(), context);

        assertTrue(reminder.renderedText().contains("Hola Sofía"));
        assertTrue(reminder.renderedText().contains("19/11/2026"));
        assertTrue(reminder.renderedText().contains("15:30"));
        assertTrue(reminder.renderedText().contains("Metanoia Hair Salon"));
        assertTrue(reminder.unknownVariables().isEmpty());

        assertEquals(
                "¡Feliz cumpleaños, Sofía! Te saludamos desde Metanoia Hair Salon.",
                birthday.renderedText());
        assertTrue(birthday.unknownVariables().isEmpty());
    }
}
