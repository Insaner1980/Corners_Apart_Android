package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element
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

    private fun Element.androidAttribute(name: String): String {
        val value = getAttributeNS(ANDROID_NAMESPACE, name)

        assertNotNull(value)

        return value
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
