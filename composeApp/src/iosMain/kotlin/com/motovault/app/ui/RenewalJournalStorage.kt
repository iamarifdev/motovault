@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*
import platform.posix.*

/** Private reminder metadata only. Never reads or writes the owner database. */
internal class RenewalJournalStorage(private val directory: String) {
    private val path get() = "$directory/renewal-journal.json"

    fun read(): RenewalJournal? {
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        val data = requireNotNull(NSData.dataWithContentsOfFile(path)) { "Could not read reminder journal." }
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        // Corrupt/unreadable metadata fails closed instead of risking duplicate immediate alerts.
        return Json.decodeFromString<RenewalJournal>(bytes.decodeToString())
    }

    fun write(journal: RenewalJournal) {
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)) { "Could not create reminder storage." }
        val temporary = "$path.tmp"
        val fd = open(temporary, O_WRONLY or O_CREAT or O_TRUNC, 384)
        check(fd >= 0) { "Could not open reminder journal." }
        try {
            val bytes = Json.encodeToString(journal).encodeToByteArray()
            bytes.usePinned { pinned ->
                var offset = 0
                while (offset < bytes.size) {
                    val written = platform.posix.write(fd, pinned.addressOf(offset), (bytes.size - offset).toULong())
                    check(written > 0) { "Could not write reminder journal." }
                    offset += written.toInt()
                }
            }
            check(fsync(fd) == 0) { "Could not sync reminder journal." }
        } finally { close(fd) }
        check(rename(temporary, path) == 0) { "Could not replace reminder journal." }
        val parent = open(directory, O_RDONLY)
        check(parent >= 0) { "Could not open reminder directory." }
        try { check(fsync(parent) == 0) { "Could not sync reminder directory." } }
        finally { close(parent) }
    }
}

internal fun renewalJournalStorage() = RenewalJournalStorage(
    NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String,
)
