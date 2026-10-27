package com.motovault.app.domain.backup

/**
 * Pure Kotlin standard ZIP archive reader and writer for uncompressed (STORED) entries.
 * Produces standard ZIP files readable by any ZIP tool, and enforces strict security bounds
 * against zip bombs, path traversal, and malicious headers.
 */
object MvaZipArchive {

    private const val LOCAL_HEADER_SIG = 0x04034b50
    private const val CENTRAL_HEADER_SIG = 0x02014b50
    private const val EOCD_SIG = 0x06054b50

    private const val METHOD_STORED = 0
    private const val VERSION_NEEDED = 20

    const val MAX_ENTRY_BYTES = 32 * 1024 * 1024 // 32 MiB
    const val DEFAULT_MAX_TOTAL_BYTES = 150 * 1024 * 1024L // 150 MiB
    const val MAX_ENTRY_COUNT = 500

    fun write(entries: Map<String, ByteArray>): ByteArray {
        require(entries.size <= MAX_ENTRY_COUNT) { "Too many archive entries: ${entries.size}" }
        var totalBytes = 0L
        for ((name, data) in entries) {
            validateEntryName(name)
            require(data.size <= MAX_ENTRY_BYTES) { "Entry $name exceeds maximum size: ${data.size}" }
            totalBytes += data.size
            require(totalBytes <= DEFAULT_MAX_TOTAL_BYTES) { "Archive total payload exceeds limit: $totalBytes" }
        }

        val localHeaderOffsets = mutableMapOf<String, Int>()
        val localBytesList = mutableListOf<ByteArray>()
        var currentOffset = 0

        // 1. Write local file headers and data
        for ((name, data) in entries) {
            localHeaderOffsets[name] = currentOffset
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            val header = ByteArray(30 + nameBytes.size + data.size)

            putIntLe(header, 0, LOCAL_HEADER_SIG)
            putShortLe(header, 4, VERSION_NEEDED)
            putShortLe(header, 6, 0) // Flags
            putShortLe(header, 8, METHOD_STORED)
            putShortLe(header, 10, 0) // Time
            putShortLe(header, 12, 0) // Date
            putIntLe(header, 14, crc.toInt())
            putIntLe(header, 18, data.size) // Compressed size
            putIntLe(header, 22, data.size) // Uncompressed size
            putShortLe(header, 26, nameBytes.size)
            putShortLe(header, 28, 0) // Extra field length

            nameBytes.copyInto(header, 30)
            data.copyInto(header, 30 + nameBytes.size)

            localBytesList.add(header)
            currentOffset += header.size
        }

        val centralDirOffset = currentOffset
        val centralBytesList = mutableListOf<ByteArray>()
        var centralDirSize = 0

        // 2. Write central directory
        for ((name, data) in entries) {
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            val localOffset = localHeaderOffsets[name] ?: 0
            val cdEntry = ByteArray(46 + nameBytes.size)

            putIntLe(cdEntry, 0, CENTRAL_HEADER_SIG)
            putShortLe(cdEntry, 4, VERSION_NEEDED) // Version made by
            putShortLe(cdEntry, 6, VERSION_NEEDED) // Version needed
            putShortLe(cdEntry, 8, 0) // Flags
            putShortLe(cdEntry, 10, METHOD_STORED)
            putShortLe(cdEntry, 12, 0) // Time
            putShortLe(cdEntry, 14, 0) // Date
            putIntLe(cdEntry, 16, crc.toInt())
            putIntLe(cdEntry, 20, data.size)
            putIntLe(cdEntry, 24, data.size)
            putShortLe(cdEntry, 28, nameBytes.size)
            putShortLe(cdEntry, 30, 0) // Extra length
            putShortLe(cdEntry, 32, 0) // Comment length
            putShortLe(cdEntry, 34, 0) // Disk start
            putShortLe(cdEntry, 36, 0) // Internal attributes
            putIntLe(cdEntry, 38, 0) // External attributes
            putIntLe(cdEntry, 42, localOffset)

            nameBytes.copyInto(cdEntry, 46)
            centralBytesList.add(cdEntry)
            centralDirSize += cdEntry.size
        }

        // 3. Write EOCD
        val eocd = ByteArray(22)
        putIntLe(eocd, 0, EOCD_SIG)
        putShortLe(eocd, 4, 0) // Disk number
        putShortLe(eocd, 6, 0) // Central dir disk
        putShortLe(eocd, 8, entries.size) // Entries on this disk
        putShortLe(eocd, 10, entries.size) // Total entries
        putIntLe(eocd, 12, centralDirSize)
        putIntLe(eocd, 16, centralDirOffset)
        putShortLe(eocd, 20, 0) // Comment length

        // Combine all parts
        val totalArchiveSize = currentOffset + centralDirSize + eocd.size
        val output = ByteArray(totalArchiveSize)
        var pos = 0
        for (part in localBytesList) {
            part.copyInto(output, pos)
            pos += part.size
        }
        for (part in centralBytesList) {
            part.copyInto(output, pos)
            pos += part.size
        }
        eocd.copyInto(output, pos)

        return output
    }

    fun read(archiveBytes: ByteArray, maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES): Map<String, ByteArray> {
        require(archiveBytes.size >= 22) { "Archive file is truncated or empty." }
        val result = mutableMapOf<String, ByteArray>()
        var offset = 0
        var totalExtractedBytes = 0L

        while (offset + 30 <= archiveBytes.size) {
            val sig = getIntLe(archiveBytes, offset)
            if (sig == CENTRAL_HEADER_SIG || sig == EOCD_SIG) {
                // Reached central directory; all local file entries have been parsed
                break
            }
            require(sig == LOCAL_HEADER_SIG) { "Invalid ZIP signature at offset $offset: 0x${sig.toString(16)}" }

            val method = getShortLe(archiveBytes, offset + 8)
            require(method == METHOD_STORED) { "Unsupported compression method: $method. Only STORED is allowed in MVA archives." }

            val crc = getIntLe(archiveBytes, offset + 14).toLong() and 0xFFFFFFFFL
            val compressedSize = getIntLe(archiveBytes, offset + 18)
            val uncompressedSize = getIntLe(archiveBytes, offset + 22)
            require(compressedSize == uncompressedSize) { "Mismatch in compressed and uncompressed size for STORED entry." }
            require(uncompressedSize in 0..MAX_ENTRY_BYTES) { "Entry size out of bounds: $uncompressedSize" }

            val nameLength = getShortLe(archiveBytes, offset + 26)
            val extraLength = getShortLe(archiveBytes, offset + 28)
            require(nameLength in 1..256) { "Invalid entry name length: $nameLength" }

            val nameStart = offset + 30
            val dataStart = nameStart + nameLength + extraLength
            val dataEnd = dataStart + uncompressedSize
            require(dataEnd <= archiveBytes.size) { "Archive truncated while reading entry data." }

            val name = archiveBytes.copyOfRange(nameStart, nameStart + nameLength).decodeToString()
            validateEntryName(name)

            totalExtractedBytes += uncompressedSize
            require(totalExtractedBytes <= maxTotalBytes) { "Archive expansion exceeds safety limit ($totalExtractedBytes > $maxTotalBytes bytes)." }
            require(result.size < MAX_ENTRY_COUNT) { "Archive contains too many entries (> $MAX_ENTRY_COUNT)." }

            val entryData = archiveBytes.copyOfRange(dataStart, dataEnd)
            val actualCrc = crc32(entryData)
            require(actualCrc == crc) { "CRC32 checksum mismatch for entry $name: expected $crc, calculated $actualCrc" }

            result[name] = entryData
            offset = dataEnd
        }

        require(result.isNotEmpty()) { "Archive does not contain any valid entries." }
        return result
    }

    fun validateEntryName(name: String) {
        require(name.isNotBlank()) { "Entry name cannot be blank." }
        require(!name.startsWith("/") && !name.startsWith("\\")) { "Path traversal rejected: leading slash in $name" }
        require(!name.contains("..")) { "Path traversal rejected: '..' in $name" }
        require(!name.contains(":") && !name.contains("\\")) { "Invalid characters in entry name: $name" }
    }

    private fun putShortLe(dest: ByteArray, offset: Int, value: Int) {
        dest[offset] = (value and 0xFF).toByte()
        dest[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun putIntLe(dest: ByteArray, offset: Int, value: Int) {
        dest[offset] = (value and 0xFF).toByte()
        dest[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        dest[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        dest[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun getShortLe(src: ByteArray, offset: Int): Int {
        val b0 = src[offset].toInt() and 0xFF
        val b1 = src[offset + 1].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }

    private fun getIntLe(src: ByteArray, offset: Int): Int {
        val b0 = src[offset].toInt() and 0xFF
        val b1 = src[offset + 1].toInt() and 0xFF
        val b2 = src[offset + 2].toInt() and 0xFF
        val b3 = src[offset + 3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    /** Standard IEEE 802.3 CRC32 calculation. */
    fun crc32(data: ByteArray): Long {
        var crc = 0xFFFFFFFFL
        for (b in data) {
            val byteVal = (b.toInt() and 0xFF).toLong()
            var c = (crc xor byteVal) and 0xFFL
            for (j in 0 until 8) {
                c = if ((c and 1L) != 0L) (c ushr 1) xor 0xEDB88320L else c ushr 1
            }
            crc = (crc ushr 8) xor c
        }
        return crc xor 0xFFFFFFFFL
    }
}
