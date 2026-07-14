package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class DependencyVerificationPolicyTest {
    @Test
    fun dependencyVerificationRequiresMetadataSignaturesAndChecksums() {
        val document = verificationMetadata()
        val configuration = document.elements("configuration").single()

        assertEquals("true", configuration.childText("verify-metadata"))
        assertEquals("true", configuration.childText("verify-signatures"))

        val artifactsWithoutSha256 =
            document
                .elements("artifact")
                .filter { artifact -> artifact.elements("sha256").isEmpty() }
                .map { artifact ->
                    val component = artifact.parentNode as Element
                    listOf(
                        component.attr("group"),
                        component.attr("name"),
                        component.attr("version"),
                        artifact.attr("name"),
                    ).joinToString(":")
                }

        assertTrue(
            "Kaikilla dependency verification -artefakteilla pitää olla SHA-256:\n" +
                artifactsWithoutSha256.joinToString("\n"),
            artifactsWithoutSha256.isEmpty(),
        )
    }

    @Test
    fun dependencyVerificationDoesNotUseWildcardTrustedKeysOrTrustedArtifacts() {
        val document = verificationMetadata()

        val trustedArtifacts = document.elements("trust")
        assertTrue(
            "trusted-artifacts ohittaa komponenttikohtaisen metadata-arvioinnin.",
            trustedArtifacts.isEmpty(),
        )

        val wildcardTrustedKeys =
            document
                .elements("trusted-key")
                .flatMap { trustedKey ->
                    listOf(trustedKey) + trustedKey.elements("trusting")
                }.filter { trustedElement -> trustedElement.attr("regex") == "true" }
                .map { trustedElement ->
                    val trustedKey =
                        if (trustedElement.tagName == "trusted-key") {
                            trustedElement
                        } else {
                            trustedElement.parentNode as Element
                        }
                    val group = trustedElement.attr("group").ifBlank { "<missing group>" }
                    "${trustedKey.attr("id")} -> $group"
                }

        assertTrue(
            "Trusted PGP keys must not use regex/wildcard group scope because signed future " +
                "updates could avoid checksum-review:\n" +
                wildcardTrustedKeys.joinToString("\n"),
            wildcardTrustedKeys.isEmpty(),
        )

        val unscopedTrustedKeys =
            document
                .elements("trusted-key")
                .filter { trustedKey ->
                    val directScope =
                        listOf("group", "name", "version", "file")
                            .any { scopeAttribute -> trustedKey.attr(scopeAttribute).isNotBlank() }
                    !directScope && trustedKey.elements("trusting").isEmpty()
                }.map { trustedKey -> trustedKey.attr("id") }

        assertTrue(
            "Trusted PGP keys must have a coordinate scope or explicit trusting entries:\n" +
                unscopedTrustedKeys.joinToString("\n"),
            unscopedTrustedKeys.isEmpty(),
        )
    }

    private fun verificationMetadata(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory
            .newDocumentBuilder()
            .parse(projectRoot().resolve("gradle/verification-metadata.xml").toFile())
    }

    private fun Document.elements(localName: String): List<Element> =
        documentElement
            .getElementsByTagNameNS("*", localName)
            .asElementList()

    private fun Element.elements(localName: String): List<Element> =
        getElementsByTagNameNS("*", localName)
            .asElementList()

    private fun Element.childText(localName: String): String =
        elements(localName)
            .single()
            .textContent
            .trim()

    private fun Element.attr(name: String): String = getAttribute(name)

    private fun org.w3c.dom.NodeList.asElementList(): List<Element> =
        (0 until length).map { index -> item(index) as Element }
}
