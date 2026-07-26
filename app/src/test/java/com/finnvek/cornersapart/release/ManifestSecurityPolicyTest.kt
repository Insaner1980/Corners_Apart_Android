package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class ManifestSecurityPolicyTest {
    @Test
    fun sourceSetApplicationManifestsDisableBackupAndCleartextTraffic() {
        val root = projectRoot()
        val manifestPaths =
            listOf(
                "app/src/main/AndroidManifest.xml",
                "app/src/debug/AndroidManifest.xml",
            )

        manifestPaths.forEach { path ->
            val application = parseXml(root.resolve(path).toFile()).applicationElement()

            assertEquals("false", application.androidAttribute("allowBackup"))
            assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
            assertEquals("false", application.androidAttribute("fullBackupContent"))
            assertEquals("false", application.androidAttribute("usesCleartextTraffic"))
        }
    }

    @Test
    fun releaseManifestExportsOnlyMainActivity() {
        val manifest =
            projectRoot()
                .resolve(
                    "app/build/intermediates/packaged_manifests/release/" +
                        "processReleaseManifestForPackage/AndroidManifest.xml",
                ).toFile()

        assertTrue(
            "Run :app:processReleaseManifestForPackage before this test.",
            manifest.isFile,
        )

        val exportedComponents =
            parseXml(manifest)
                .applicationElement()
                .componentElements()
                .filter { element -> element.androidAttribute("exported") == "true" }
                .map { element -> "${element.tagName}:${element.androidAttribute("name")}" }

        assertEquals(
            listOf("activity:com.finnvek.cornersapart.MainActivity"),
            exportedComponents,
        )
    }

    private fun parseXml(file: File): Element =
        secureDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(file)
            .documentElement

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }

    private fun Element.applicationElement(): Element {
        val applications = getElementsByTagName("application")

        assertEquals(1, applications.length)

        return applications.item(0) as Element
    }

    private fun Element.componentElements(): List<Element> =
        (0 until childNodes.length)
            .map { index -> childNodes.item(index) }
            .filter { node -> node.nodeType == Node.ELEMENT_NODE }
            .map { node -> node as Element }
            .filter { element -> element.tagName in APPLICATION_COMPONENT_TAGS }

    private fun Element.androidAttribute(name: String): String {
        val value = getAttributeNS(ANDROID_NAMESPACE, name)

        assertNotNull(value)

        return value
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val APPLICATION_COMPONENT_TAGS =
            setOf(
                "activity",
                "activity-alias",
                "service",
                "receiver",
                "provider",
            )
    }
}
