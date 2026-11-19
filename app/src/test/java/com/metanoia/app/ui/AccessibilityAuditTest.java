package com.metanoia.app.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Auditoría automatizada de accesibilidad: contraste WCAG AA (>= 4.5:1) en tema Claro y Oscuro,
 * descriptores para TalkBack y ausencia de alturas fijas en texto escalable.
 */
public class AccessibilityAuditTest {

    private static final double MIN_AA_CONTRAST = 4.5;
    private static final Pattern FIXED_DIMEN_HEIGHT = Pattern.compile(
            "<(?:TextView|com\\.google\\.android\\.material\\.button\\.MaterialButton|Button)\\b[^>]*android:layout_height=\"@dimen/[^\"]+\"",
            Pattern.DOTALL);

    @Test
    public void themeColors_meetWcagAaContrastInLightAndDarkModes() throws Exception {
        Map<String, Integer> colors = parseColorsXml(resolveAppFile("src/main/res/values/colors.xml"));

        // Modo claro
        assertContrastAtLeast("ink on white", colors.get("ink"), colors.get("white"));
        assertContrastAtLeast("ink on gray50", colors.get("ink"), colors.get("gray50"));
        assertContrastAtLeast("gray600 on white", colors.get("gray600"), colors.get("white"));
        assertContrastAtLeast("gray600 on gray50", colors.get("gray600"), colors.get("gray50"));
        assertContrastAtLeast("on_accent_light on accent_light",
                colors.get("on_accent_light"), colors.get("accent_light"));
        assertContrastAtLeast("accent_light on accent_container_light",
                colors.get("accent_light"), colors.get("accent_container_light"));
        assertContrastAtLeast("accent_light on white",
                colors.get("accent_light"), colors.get("white"));

        // Modo oscuro
        assertContrastAtLeast("white on ink", colors.get("white"), colors.get("ink"));
        assertContrastAtLeast("white on gray900", colors.get("white"), colors.get("gray900"));
        assertContrastAtLeast("gray300_dark on ink",
                colors.get("gray300_dark"), colors.get("ink"));
        assertContrastAtLeast("gray300_dark on gray900",
                colors.get("gray300_dark"), colors.get("gray900"));
        assertContrastAtLeast("on_accent_dark on accent_dark",
                colors.get("on_accent_dark"), colors.get("accent_dark"));
        assertContrastAtLeast("accent_dark on accent_container_dark",
                colors.get("accent_dark"), colors.get("accent_container_dark"));
        assertContrastAtLeast("accent_dark on ink",
                colors.get("accent_dark"), colors.get("ink"));
    }

    @Test
    public void layouts_provideAccessibilityDescriptionsAndAvoidFixedHeightTextContainers()
            throws Exception {
        File layoutDir = resolveAppFile("src/main/res/layout");
        File[] files = layoutDir.listFiles((dir, name) -> name.endsWith(".xml"));
        assertTrue("Faltan archivos de layout", files != null && files.length > 0);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);

        for (File file : files) {
            String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            assertTrue("Altura fija en TextView/MaterialButton detectada en " + file.getName(),
                    !FIXED_DIMEN_HEIGHT.matcher(raw).find());

            Document doc = dbf.newDocumentBuilder().parse(file);
            verifyTagDescriptions(doc, file.getName(), "ImageButton");
            verifyTagDescriptions(doc, file.getName(), "ImageView");
            verifyTagDescriptions(
                    doc, file.getName(),
                    "com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton");
        }
    }

    private static void verifyTagDescriptions(
            Document doc,
            String fileName,
            String tagName
    ) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String cd = el.getAttribute("android:contentDescription");
            String important = el.getAttribute("android:importantForAccessibility");
            boolean ok = !cd.isEmpty() || "no".equals(important);
            assertTrue("Falta contentDescription o importantForAccessibility=\"no\" en "
                    + fileName + " <" + tagName + ">", ok);
        }
    }

    private static void assertContrastAtLeast(String label, Integer fgObj, Integer bgObj) {
        assertTrue("Color faltante para " + label, fgObj != null && bgObj != null);
        double ratio = contrastRatio(fgObj, bgObj);
        assertTrue(label + " tiene contraste insuficiente: " + ratio, ratio >= MIN_AA_CONTRAST);
    }

    private static double contrastRatio(int fg, int bg) {
        double l1 = relativeLuminance(fg);
        double l2 = relativeLuminance(bg);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int rgb) {
        double r = linearize(((rgb >> 16) & 0xFF) / 255.0);
        double g = linearize(((rgb >> 8) & 0xFF) / 255.0);
        double b = linearize((rgb & 0xFF) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double srgb) {
        return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    private static Map<String, Integer> parseColorsXml(File file) throws Exception {
        String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<color name=\"([^\"]+)\">#([0-9a-fA-F]{6})</color>").matcher(raw);
        Map<String, Integer> map = new HashMap<>();
        while (m.find()) {
            map.put(m.group(1), Integer.parseInt(m.group(2), 16));
        }
        return map;
    }

    private static File resolveAppFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("app", relativePath);
    }
}
