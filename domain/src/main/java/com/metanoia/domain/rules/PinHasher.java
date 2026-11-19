package com.metanoia.domain.rules;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Derivación y verificación de PIN numérico (4–6 dígitos) mediante PBKDF2-HMAC-SHA256 con sal
 * aleatoria y comparación en tiempo constante.
 */
public final class PinHasher {

    public static final int MIN_PIN_LENGTH = 4;
    public static final int MAX_PIN_LENGTH = 6;
    public static final int SALT_BYTES = 16;
    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final class HashedPin {
        private final String saltBase64;
        private final String hashBase64;
        private final int iterations;

        public HashedPin(String saltBase64, String hashBase64, int iterations) {
            this.saltBase64 = Objects.requireNonNull(saltBase64, "saltBase64");
            this.hashBase64 = Objects.requireNonNull(hashBase64, "hashBase64");
            if (iterations <= 0) {
                throw new IllegalArgumentException("iterations debe ser > 0");
            }
            this.iterations = iterations;
        }

        public String saltBase64() {
            return saltBase64;
        }

        public String hashBase64() {
            return hashBase64;
        }

        public int iterations() {
            return iterations;
        }
    }

    private PinHasher() {
    }

    public static boolean isValidPinFormat(String pin) {
        if (pin == null) {
            return false;
        }
        int len = pin.length();
        if (len < MIN_PIN_LENGTH || len > MAX_PIN_LENGTH) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char ch = pin.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    public static HashedPin hash(String pin) {
        return hash(pin, DEFAULT_ITERATIONS);
    }

    public static HashedPin hash(String pin, int iterations) {
        if (!isValidPinFormat(pin)) {
            throw new IllegalArgumentException("El PIN debe tener entre 4 y 6 dígitos numéricos");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iteraciones inválidas: " + iterations);
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] derived = derive(pin, salt, iterations);
        Base64.Encoder encoder = Base64.getEncoder();
        return new HashedPin(
                encoder.encodeToString(salt),
                encoder.encodeToString(derived),
                iterations
        );
    }

    public static boolean verify(
            String candidatePin,
            String saltBase64,
            String expectedHashBase64,
            int iterations
    ) {
        if (!isValidPinFormat(candidatePin)
                || saltBase64 == null || saltBase64.trim().isEmpty()
                || expectedHashBase64 == null || expectedHashBase64.trim().isEmpty()
                || iterations <= 0) {
            return false;
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(saltBase64.trim());
            byte[] expected = decoder.decode(expectedHashBase64.trim());
            byte[] actual = derive(candidatePin, salt, iterations);
            boolean matches = MessageDigest.isEqual(actual, expected);
            Arrays.fill(actual, (byte) 0);
            return matches;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(String pin, byte[] salt, int iterations) {
        char[] chars = pin.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 no disponible", ex);
        } finally {
            Arrays.fill(chars, '\0');
            spec.clearPassword();
        }
    }
}
