package com.metanoia.domain.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResultTest {

    @Test
    public void success_validValue_exposesValueAndSuccessState() {
        Result<String> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("ok", result.getValue());
        assertTrue(result.valueOptional().isPresent());
        assertEquals("ok", result.valueOptional().get());
        assertFalse(result.errorOptional().isPresent());
    }

    @Test
    public void success_readingError_throwsIllegalState() {
        Result<Integer> result = Result.success(42);

        assertThrows(IllegalStateException.class, result::getError);
        assertThrows(IllegalStateException.class, result::getErrorMessage);
    }

    @Test
    public void success_nullValue_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> Result.success(null));
    }

    @Test
    public void failure_validError_exposesErrorAndMessage() {
        Result<String> result = Result.failure(DomainError.CONFLICT, "Horario ocupado");

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(DomainError.CONFLICT, result.getError());
        assertEquals("Horario ocupado", result.getErrorMessage());
        assertTrue(result.errorOptional().isPresent());
        assertFalse(result.valueOptional().isPresent());
    }

    @Test
    public void failure_nullMessage_defaultsToEmptyString() {
        Result<String> result = Result.failure(DomainError.NOT_FOUND, null);

        assertEquals(DomainError.NOT_FOUND, result.getError());
        assertEquals("", result.getErrorMessage());
    }

    @Test
    public void failure_readingValue_throwsIllegalState() {
        Result<String> result = Result.failure(DomainError.VALIDATION_ERROR, "Dato inválido");

        assertThrows(IllegalStateException.class, result::getValue);
    }
}
