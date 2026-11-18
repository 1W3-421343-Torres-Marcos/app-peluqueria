package com.metanoia.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

public class ManifestPrivacyTest {

    @Test
    public void sourceManifest_disablesBackupAndBlocksInternet() throws Exception {
        File sourceManifest = resolveAppFile("src/main/AndroidManifest.xml");
        assertTrue("Falta AndroidManifest.xml", sourceManifest.exists());

        Document doc = parseXml(sourceManifest);
        Element app = (Element) doc.getElementsByTagName("application").item(0);
        assertEquals("false", app.getAttribute("android:allowBackup"));
        assertEquals("@xml/data_extraction_rules", app.getAttribute("android:dataExtractionRules"));
        assertEquals("@xml/backup_rules", app.getAttribute("android:fullBackupContent"));

        NodeList permissions = doc.getElementsByTagName("uses-permission");
        for (int i = 0; i < permissions.getLength(); i++) {
            Element perm = (Element) permissions.item(i);
            if ("android.permission.INTERNET".equals(perm.getAttribute("android:name"))) {
                assertEquals("remove", perm.getAttribute("tools:node"));
            }
        }
    }

    @Test
    public void mergedManifest_whenBuilt_hasNoInternetAndAllowBackupFalse() throws Exception {
        File merged = resolveAppFile(
                "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml");
        if (!merged.exists()) {
            return;
        }
        Document doc = parseXml(merged);
        Element app = (Element) doc.getElementsByTagName("application").item(0);
        assertEquals("false", app.getAttribute("android:allowBackup"));

        NodeList permissions = doc.getElementsByTagName("uses-permission");
        for (int i = 0; i < permissions.getLength(); i++) {
            Element perm = (Element) permissions.item(i);
            assertFalse("El manifiesto fusionado no debe declarar INTERNET",
                    "android.permission.INTERNET".equals(perm.getAttribute("android:name")));
        }
    }

    @Test
    public void dataExtractionRules_excludesAllDomains() throws Exception {
        File rules = resolveAppFile("src/main/res/xml/data_extraction_rules.xml");
        assertTrue(rules.exists());
        Document doc = parseXml(rules);
        assertEquals(5, doc.getElementsByTagName("cloud-backup").item(0).getChildNodes().getLength() > 0
                ? ((Element) doc.getElementsByTagName("cloud-backup").item(0))
                        .getElementsByTagName("exclude").getLength()
                : 0);
        assertEquals(5, ((Element) doc.getElementsByTagName("device-transfer").item(0))
                .getElementsByTagName("exclude").getLength());
    }

    private static File resolveAppFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("app", relativePath);
    }

    private static Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(file);
    }
}
