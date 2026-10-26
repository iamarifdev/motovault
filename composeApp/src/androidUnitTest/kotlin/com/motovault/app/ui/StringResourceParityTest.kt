package com.motovault.app.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

class StringResourceParityTest {

    private fun parseStringsXml(file: File): Map<String, String> {
        assertTrue(file.exists(), "Resource file ${file.absolutePath} must exist")
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val result = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            assertNotNull(name, "String tag at index $i must have a name attribute")
            result[name] = node.textContent
        }
        return result
    }

    @Test
    fun testDefaultAndBengaliStringsHaveFullKeyParity() {
        val root = File("src/commonMain/composeResources")
        val defaultXml = File(root, "values/strings.xml")
        val bnXml = File(root, "values-bn/strings.xml")

        val defaultStrings = parseStringsXml(defaultXml)
        val bnStrings = parseStringsXml(bnXml)

        assertTrue(defaultStrings.isNotEmpty(), "Default strings must not be empty")
        assertTrue(bnStrings.isNotEmpty(), "Bengali strings must not be empty")

        val missingInBn = defaultStrings.keys - bnStrings.keys
        val missingInDefault = bnStrings.keys - defaultStrings.keys

        assertTrue(missingInBn.isEmpty(), "Keys missing in values-bn/strings.xml: $missingInBn")
        assertTrue(missingInDefault.isEmpty(), "Keys missing in values/strings.xml: $missingInDefault")

        // Ensure no empty string values
        for ((key, value) in defaultStrings) {
            assertTrue(value.isNotBlank(), "Default string for '$key' should not be blank")
        }
        for ((key, value) in bnStrings) {
            assertTrue(value.isNotBlank(), "Bengali string for '$key' should not be blank")
        }
    }
}
