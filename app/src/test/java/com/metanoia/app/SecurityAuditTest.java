package com.metanoia.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Auditoría estática de privacidad, R8 y ausencia de red o logs sensibles en fuentes principales.
 */
public class SecurityAuditTest {

    private static final String[] FORBIDDEN_SOURCE_PATTERNS = {
            "android.util.Log",
            "System.out.",
            "System.err.",
            ".printStackTrace(",
            "HttpURLConnection",
            "java.net.Socket",
            "okhttp3",
            "retrofit2"
    };

    @Test
    public void mainJavaSources_containNoSensitiveLoggingOrNetworkClients() throws IOException {
        List<Path> roots = new ArrayList<>();
        roots.add(resolveProjectFile("app/src/main/java").toPath());
        roots.add(resolveProjectFile("domain/src/main/java").toPath());

        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> scanSourceFile(path, violations));
            }
        }
        assertTrue("Se detectaron patrones prohibidos: " + violations, violations.isEmpty());
    }

    @Test
    public void releaseBuildAndManifest_enableR8AndSecureStorageDefaults() throws IOException {
        String gradle = readUtf8(resolveProjectFile("app/build.gradle"));
        assertTrue("R8 minifyEnabled true debe estar activo en release",
                gradle.contains("minifyEnabled true"));
        assertTrue("shrinkResources true debe estar activo en release",
                gradle.contains("shrinkResources true"));

        String proguard = readUtf8(resolveProjectFile("app/proguard-rules.pro"));
        assertTrue("proguard-rules.pro debe remover android.util.Log en release",
                proguard.contains("-assumenosideeffects class android.util.Log"));

        String gitignore = readUtf8(resolveProjectFile(".gitignore"));
        for (String ignored : new String[]{"keystore.properties", "*.jks", "*.keystore", "*.apk", "*.aab"}) {
            assertTrue(".gitignore debe excluir " + ignored, gitignore.contains(ignored));
        }

        String manifest = readUtf8(resolveProjectFile("app/src/main/AndroidManifest.xml"));
        assertTrue("AndroidManifest.xml debe remover INTERNET",
                manifest.contains("android.permission.INTERNET")
                        && manifest.contains("tools:node=\"remove\""));
        assertTrue("AndroidManifest.xml debe declarar READ_CONTACTS solo lectura",
                manifest.contains("android.permission.READ_CONTACTS"));
        assertTrue("AndroidManifest.xml no debe declarar WRITE_CONTACTS",
                !manifest.contains("WRITE_CONTACTS"));
        assertTrue("AndroidManifest.xml debe desactivar allowBackup",
                manifest.contains("android:allowBackup=\"false\""));
        assertTrue("FileProvider debe permanecer no exportado y con URI temporal",
                manifest.contains("android:exported=\"false\"")
                        && manifest.contains("android:grantUriPermissions=\"true\""));
    }

    private static void scanSourceFile(Path path, List<String> violations) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_SOURCE_PATTERNS) {
                if (content.contains(forbidden)) {
                    violations.add(path.getFileName() + " contiene " + forbidden);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + path, ex);
        }
    }

    private static String readUtf8(File file) throws IOException {
        assertTrue("Archivo requerido inexistente: " + file.getPath(), file.exists());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static File resolveProjectFile(String relativeFromRoot) {
        File direct = new File(relativeFromRoot);
        if (direct.exists()) {
            return direct;
        }
        File parent = new File("..", relativeFromRoot);
        if (parent.exists()) {
            return parent;
        }
        return direct;
    }
}
