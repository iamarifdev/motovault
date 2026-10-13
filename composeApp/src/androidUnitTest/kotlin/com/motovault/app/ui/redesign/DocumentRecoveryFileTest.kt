package com.motovault.app.ui.redesign

import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DocumentRecoveryFileTest {
    @Test fun quarantineRetainsBytesTimestampReferencesAndCanRestore() {
        val root = Files.createTempDirectory("motovault-recovery").toFile().canonicalFile
        try {
            val documents = File(root, "documents").apply { mkdirs() }
            val recovery = File(root, "documents-recovery")
            val now = 1800000000000L
            val old = now - DOCUMENT_ORPHAN_GRACE_MILLIS - 1000
            val orphan = File(documents, "1790000000000--123.pdf").apply { writeBytes(byteArrayOf(0, 1, 2, 127, -1)); setLastModified(old) }
            val saved = File(documents, "1790000000000-456.jpg").apply { writeText("saved original"); setLastModified(old) }
            val fresh = File(documents, "1790000000000-789.jpg").apply { writeText("live draft"); setLastModified(now) }
            val bytes = orphan.readBytes()
            quarantineDocumentFiles(documents, recovery, setOf(saved.name), now)
            assertFalse(orphan.exists())
            val recovered = File(recovery, orphan.name)
            assertContentEquals(bytes, recovered.readBytes())
            assertEquals(old, recovered.lastModified())
            assertEquals("saved original", saved.readText())
            assertEquals("live draft", fresh.readText())
            assertEquals(orphan.name, restoreRecoveredDocumentFile(documents, recovery, orphan.name))
            assertContentEquals(bytes, orphan.readBytes())
            assertFalse(recovered.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun collisionsRetainBothOriginalsAndRestoreNeverOverwrites() {
        val root = Files.createTempDirectory("motovault-recovery-collision").toFile().canonicalFile
        try {
            val documents = File(root, "documents").apply { mkdirs() }
            val recovery = File(root, "documents-recovery").apply { mkdirs() }
            val id = "1790000000000-123.jpg"
            File(documents, id).apply { writeText("new orphan"); setLastModified(1700000000000) }
            File(recovery, id).writeText("older recovery")
            quarantineDocumentFiles(documents, recovery, emptySet(), 1800000000000)
            assertEquals(2, recovery.listFiles()!!.size)
            assertEquals(setOf("new orphan", "older recovery"), recovery.listFiles()!!.map { it.readText() }.toSet())
            File(documents, id).writeText("existing managed file")
            assertFailsWith<IllegalArgumentException> { restoreRecoveredDocumentFile(documents, recovery, id) }
            assertEquals("existing managed file", File(documents, id).readText())
            assertEquals("older recovery", File(recovery, id).readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun symlinksAndUnknownFilesRemainInPlace() {
        val root = Files.createTempDirectory("motovault-recovery-link").toFile().canonicalFile
        try {
            val documents = File(root, "documents").apply { mkdirs() }
            val target = File(root, "external-original.jpg").apply { writeText("external"); setLastModified(1700000000000) }
            val link = File(documents, "1790000000000-123.jpg")
            Files.createSymbolicLink(link.toPath(), target.toPath())
            val unknown = File(documents, "legacy-original.jpg").apply { writeText("legacy"); setLastModified(1700000000000) }
            val recovery = File(root, "documents-recovery")
            quarantineDocumentFiles(documents, recovery, emptySet(), 1800000000000)
            assertTrue(Files.isSymbolicLink(link.toPath()))
            assertEquals("external", target.readText())
            assertEquals("legacy", unknown.readText())
            assertFalse(recovery.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun danglingRecoveryLinkIsNeverOverwritten() {
        val root = Files.createTempDirectory("motovault-recovery-dangling").toFile().canonicalFile
        try {
            val documents = File(root, "documents").apply { mkdirs() }
            val recovery = File(root, "documents-recovery").apply { mkdirs() }
            val id = "1790000000000-123.jpg"
            File(documents, id).apply { writeText("orphan bytes"); setLastModified(1700000000000) }
            val link = File(recovery, id)
            Files.createSymbolicLink(link.toPath(), File(root, "missing.jpg").toPath())
            quarantineDocumentFiles(documents, recovery, emptySet(), 1800000000000)
            assertTrue(Files.isSymbolicLink(link.toPath()))
            val retained = recovery.listFiles()!!.single { it.name != id }
            assertEquals("orphan bytes", retained.readText())
            Files.createSymbolicLink(File(documents, retained.name).toPath(), File(root, "missing-again.jpg").toPath())
            assertFailsWith<IllegalArgumentException> { restoreRecoveredDocumentFile(documents, recovery, retained.name) }
            assertEquals("orphan bytes", retained.readText())
        } finally { root.deleteRecursively() }
    }
}
