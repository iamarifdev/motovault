package com.motovault.app.ui.redesign

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

private fun path(): String = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/motovault.json"

@OptIn(ExperimentalForeignApi::class)
actual fun loadVault(): String? {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path())) return NSUserDefaults.standardUserDefaults.stringForKey("motovault")
    val data = requireNotNull(NSData.dataWithContentsOfFile(path())) { "Could not read stored records." }
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes.decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveVault(value: String) {
    val destination = path()
    val directory = destination.substringBeforeLast('/')
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)) { "Could not create local storage." }
    val bytes = value.encodeToByteArray()
    val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
    check(data.writeToFile(destination, options = NSDataWritingAtomic, error = null)) { "Could not save local records." }
}
