@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.motovault.app.ui

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.posix.*
import kotlin.test.*

class DocumentImportReadTest {
    private fun fixture(bytes: ByteArray, test: (NSURL) -> Unit) {
        val path = NSTemporaryDirectory() + "document-import-" + NSUUID().UUIDString
        try {
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            assertTrue(data.writeToFile(path, atomically = true))
            test(NSURL.fileURLWithPath(path))
        } finally { NSFileManager.defaultManager.removeItemAtPath(path, null) }
    }

    @Test fun exactLimitRetainsEveryOriginalByteAcrossChunks() {
        val expected = ByteArray(8193) { (it % 251).toByte() }
        fixture(expected) { url -> runBlocking {
            val data = readBoundedDocumentData(url, expected.size)
            val actual = ByteArray(data.length.toInt())
            actual.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            assertContentEquals(expected, actual)
        } }
    }

    @Test fun oneByteOverLimitIsRejectedWithoutChangingTheSource() {
        val expected = ByteArray(8193) { (it % 251).toByte() }
        fixture(expected) { url -> runBlocking {
            assertFailsWith<IllegalArgumentException> { readBoundedDocumentData(url, expected.size - 1) }
            val data = readBoundedDocumentData(url, expected.size)
            val actual = ByteArray(data.length.toInt())
            actual.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            assertContentEquals(expected, actual)
        } }
    }

    @Test fun realFileDescriptorClosesWhenReadRejects() {
        fixture(byteArrayOf(1, 2)) { url -> runBlocking {
            var descriptor = -1
            assertFailsWith<IllegalArgumentException> {
                withDocumentReadHandle(url) { handle ->
                    descriptor = handle.fileDescriptor
                    val chunk = handle.readDataOfLength(2uL)
                    require(chunk.length <= 1uL)
                }
            }
            assertTrue(descriptor >= 0)
            assertEquals(-1, fcntl(descriptor, F_GETFD))
        } }
    }

    @Test fun realFileDescriptorClosesOnCoroutineCancellation() {
        fixture(byteArrayOf(1, 2)) { url -> runBlocking {
            var descriptor = -1
            val reader = launch {
                withDocumentReadHandle(url) { handle ->
                    descriptor = handle.fileDescriptor
                    handle.readDataOfLength(1uL)
                    awaitCancellation()
                }
            }
            yield()
            reader.cancelAndJoin()
            assertTrue(descriptor >= 0)
            assertEquals(-1, fcntl(descriptor, F_GETFD))
        } }
    }
}
