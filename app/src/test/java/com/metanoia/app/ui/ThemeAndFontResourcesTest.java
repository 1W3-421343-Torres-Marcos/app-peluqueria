package com.metanoia.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ThemeAndFontResourcesTest {

    private static final String[] FONT_FILES = {
            "montserrat_bold.ttf",
            "montserrat_semibold.ttf",
            "inter_regular.ttf",
            "inter_medium.ttf",
            "inter_light.ttf"
    };

    private static final String[] ASSET_FONTS = {
            "Montserrat-Bold.ttf",
            "Montserrat-SemiBold.ttf",
            "Inter-Regular.ttf",
            "Inter-Medium.ttf",
            "Inter-Light.ttf"
    };

    @Test
    public void staticFonts_existAndAreValidNonVariableTtf() throws Exception {
        for (String name : FONT_FILES) {
            File resFont = resolveAppFile("src/main/res/font/" + name);
            assertValidStaticTtf(resFont);
        }
        for (String name : ASSET_FONTS) {
            File assetFont = resolveRootFile("assets/fonts/" + name);
            assertValidStaticTtf(assetFont);
        }
        assertTrue(resolveRootFile("assets/fonts/licenses/Montserrat-OFL.txt").exists());
        assertTrue(resolveRootFile("assets/fonts/licenses/Inter-OFL.txt").exists());
    }

    @Test
    public void fontFamilyXmls_defineExpectedWeights() throws Exception {
        String montserratXml = readUtf8(resolveAppFile("src/main/res/font/montserrat.xml"));
        assertTrue(montserratXml.contains("@font/montserrat_semibold"));
        assertTrue(montserratXml.contains("@font/montserrat_bold"));

        String interXml = readUtf8(resolveAppFile("src/main/res/font/inter.xml"));
        assertTrue(interXml.contains("@font/inter_light"));
        assertTrue(interXml.contains("@font/inter_regular"));
        assertTrue(interXml.contains("@font/inter_medium"));
    }

    @Test
    public void themeResources_defineDesignTokensForDayAndNight() throws Exception {
        String colors = readUtf8(resolveAppFile("src/main/res/values/colors.xml"));
        assertTrue(colors.contains("#111111"));
        assertTrue(colors.contains("#FFFFFF"));
        assertTrue(colors.contains("#8C2F39"));
        assertTrue(colors.contains("#D9727D"));

        String nightColors = readUtf8(resolveAppFile("src/main/res/values-night/colors.xml"));
        assertTrue(nightColors.contains("@color/accent_dark"));

        String dimens = readUtf8(resolveAppFile("src/main/res/values/dimens.xml"));
        assertTrue(dimens.contains("space_m"));
        assertTrue(dimens.contains("button_primary_height"));

        String type = readUtf8(resolveAppFile("src/main/res/values/type.xml"));
        assertTrue(type.contains("TextAppearance.Metanoia.Display"));
        assertTrue(type.contains("TextAppearance.Metanoia.Money"));
        assertTrue(type.contains("tnum"));

        String lightTheme = readUtf8(resolveAppFile("src/main/res/values/themes.xml"));
        String darkTheme = readUtf8(resolveAppFile("src/main/res/values-night/themes.xml"));
        assertTrue(lightTheme.contains("Theme.Metanoia"));
        assertTrue(darkTheme.contains("Theme.Metanoia"));
    }

    private static void assertValidStaticTtf(File file) throws Exception {
        assertTrue("Falta fuente: " + file.getPath(), file.exists());
        byte[] bytes = Files.readAllBytes(file.toPath());
        assertTrue("Archivo demasiado pequeño: " + file.getName(), bytes.length > 20_000);
        assertEquals(0x00, bytes[0] & 0xFF);
        assertEquals(0x01, bytes[1] & 0xFF);
        assertEquals(0x00, bytes[2] & 0xFF);
        assertEquals(0x00, bytes[3] & 0xFF);

        int numTables = ((bytes[4] & 0xFF) << 8) | (bytes[5] & 0xFF);
        boolean hasFvar = false;
        for (int i = 0; i < numTables; i++) {
            int offset = 12 + (i * 16);
            String tag = new String(bytes, offset, 4, StandardCharsets.US_ASCII);
            if ("fvar".equals(tag)) {
                hasFvar = true;
                break;
            }
        }
        assertFalse("La fuente debe ser estática (sin tabla fvar): " + file.getName(), hasFvar);
    }

    private static String readUtf8(File file) throws Exception {
        assertTrue("Falta archivo: " + file.getPath(), file.exists());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static File resolveAppFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("app", relativePath);
    }

    private static File resolveRootFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("..", relativePath);
    }
}
