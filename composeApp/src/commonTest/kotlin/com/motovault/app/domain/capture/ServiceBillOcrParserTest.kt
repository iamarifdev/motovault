package com.motovault.app.domain.capture

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceBillOcrParserTest {
    @Test
    fun parsesBanglaCashMemoWithDotLeadersAndSlashDashAmounts() {
        val ocr = """
            সার্ভিস ক্যাশ মেমো
            ১. ইঞ্জিন অয়েল ............ ৯০০/-
            ২. অয়েল ফিল্টার .......... ২৫০/-
        """.trimIndent()
        val result = ServiceBillOcrParser.parse(ocr)
        assertEquals(listOf("engine_oil", "oil_filter"), result.lines.map { it.line.catalogueId })
        assertEquals(listOf(90000, 25000), result.lines.map { it.line.unitPricePoisha })
    }

    @Test
    fun parsesEnglishReceiptAndStandaloneTrailingAmounts() {
        val ocr = """
            1. Engine oil ---- 950/-
            2. Air filter 300
        """.trimIndent()
        val result = ServiceBillOcrParser.parse(ocr)
        assertEquals(listOf("engine_oil", "air_filter"), result.lines.map { it.line.catalogueId })
        assertEquals(listOf(95000, 30000), result.lines.map { it.line.unitPricePoisha })

        val headerSkipped = ServiceBillOcrParser.parse("Service Cash Memo\nGeneral service 500")
        assertEquals(listOf("general_service"), headerSkipped.lines.map { it.line.catalogueId })
        assertEquals(listOf(50000), headerSkipped.lines.map { it.line.unitPricePoisha })
    }
}
