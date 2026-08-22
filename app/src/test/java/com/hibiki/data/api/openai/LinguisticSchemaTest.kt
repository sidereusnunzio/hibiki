package com.hibiki.data.api.openai

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LinguisticSchemaTest {
    @Test
    fun structuredOutputOmitsJapanese() {
        val schema = OpenAiProvider.linguisticSchema()
        val properties = schema.getValue("properties").jsonObject.keys
        val expected = setOf("kana", "romaji", "literalTranslation", "naturalTranslation")
        assertEquals(expected, properties)
        val required = schema.getValue("required").jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(expected, required)
        assertFalse(schema.getValue("additionalProperties").jsonPrimitive.boolean)
        assertFalse("japanese" in properties)
    }
}
