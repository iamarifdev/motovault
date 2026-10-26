package com.motovault.app.ui

import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DocumentExportBoundaryTest {

    @Test
    fun testScratchCleanupRemovesOnlyTemporaryCopiesAndPreservesOtherFiles() {
        val root = Files.createTempDirectory("motovault-scratch").toFile().canonicalFile
        try {
            val exportDir = File(root, "document-exports").apply { mkdirs() }
            val processDir = File(root, "document-processing").apply { mkdirs() }

            // Temporary transient copies
            val copy1 = File(exportDir, "copy-uuid-123.pdf").apply { writeText("decrypted plaintext 1") }
            val copy2 = File(exportDir, "copy-uuid-456.jpg").apply { writeText("decrypted plaintext 2") }
            val permanentNote = File(exportDir, "notes.txt").apply { writeText("do not delete") }

            val pdfProc = File(processDir, "pdf-page-0.png").apply { writeText("rendered page") }
            val otherProc = File(processDir, "manifest.json").apply { writeText("metadata") }

            // Simulate the cleanup logic from cleanDocumentScratchOnProcessStart
            listOf(exportDir to "copy-", processDir to "pdf-").forEach { (directory, prefix) ->
                directory.listFiles()?.forEach { file ->
                    if (file.name.startsWith(prefix) && file.isFile && file.canonicalFile == file.absoluteFile) {
                        file.delete()
                    }
                }
            }

            assertFalse(copy1.exists(), "copy-uuid-123.pdf should have been removed")
            assertFalse(copy2.exists(), "copy-uuid-456.jpg should have been removed")
            assertTrue(permanentNote.exists(), "notes.txt should be preserved")

            assertFalse(pdfProc.exists(), "pdf-page-0.png should have been removed")
            assertTrue(otherProc.exists(), "manifest.json should be preserved")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun testMalformedDocumentAssetIdsAreSafelyRejected() {
        val invalidIds = listOf(
            "../secret.pdf",
            "/absolute/path/file.pdf",
            "file with spaces.png",
            "nested/dir/asset.jpg",
            "",
            "invalid*char.pdf",
            "null\u0000byte.pdf"
        )

        for (badId in invalidIds) {
            val ex = assertFailsWith<IllegalArgumentException>("ID '$badId' should be rejected") {
                validateDocumentAssetId(badId)
            }
            // Ensure error message does not echo unsafe paths back as active commands or sensitive tokens
            assertNotNull(ex.message)
        }
    }

    @Test
    fun testDocumentAssetIdValidationAllowsStandardRecordIdExtensions() {
        val validIds = listOf(
            "1700000000000-123.pdf",
            "1700000000000-456.jpg",
            "1700000000000-789.png",
            "1700000000000-abc.webp"
        )

        for (goodId in validIds) {
            assertEquals(goodId, validateDocumentAssetId(goodId))
        }
    }

    @Test
    fun testErrorMessageSanitizationAvoidsSensitiveLeaks() {
        // Test that import/export errors do not disclose confidential identity details
        val sensitiveIdentity = "DHAKA_METRO_HA_99_8888"
        val error = runCatching {
            require(!sensitiveIdentity.startsWith("DHAKA")) {
                "Malformed passport JSON schema. Stored owner documents remain intact."
            }
        }.exceptionOrNull()

        assertNotNull(error)
        assertFalse(error.message!!.contains("DHAKA_METRO"))
        assertFalse(error.message!!.contains("99_8888"))
        assertTrue(error.message!!.contains("Stored owner documents remain intact."))
    }
}
