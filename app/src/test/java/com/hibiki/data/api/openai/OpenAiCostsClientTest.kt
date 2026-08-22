package com.hibiki.data.api.openai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCostsClientTest {
    private val client = OpenAiCostsClient(apiKey = "test")

    @Test
    fun formatAmount_usdUsesDollarSymbol() {
        assertEquals("$1,23", OpenAiCostsClient.formatAmount(1.23, "usd"))
    }

    @Test
    fun formatAmount_eurUsesEuroSymbol() {
        assertEquals("€4,50", OpenAiCostsClient.formatAmount(4.5, "eur"))
    }

    @Test
    fun parseCostsPage_readsSampleResponse() {
        val page = client.parseCostsPage(
            """
            {
              "object": "page",
              "data": [
                {
                  "object": "bucket",
                  "start_time": 1730419200,
                  "end_time": 1730505600,
                  "result": [
                    {
                      "object": "organization.costs.result",
                      "amount": { "value": 0.06, "currency": "usd" },
                      "line_item": "Image models"
                    }
                  ]
                }
              ],
              "has_more": false,
              "next_page": null
            }
            """.trimIndent(),
        )
        assertFalse(page.hasMore)
        assertEquals(1, page.buckets.size)
        assertEquals(1730419200L, page.buckets.single().startTime)
        assertEquals(0.06, page.buckets.single().results.single().value, 0.0001)
        assertTrue(OpenAiCostsClient.hasBillableAmount(0.06))
        assertFalse(OpenAiCostsClient.hasBillableAmount(0.0))
    }
}
