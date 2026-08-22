package com.hibiki.data.api.openai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

data class OpenAiCostsReport(
    val totalValue: Double,
    val currency: String,
    val dailyCosts: List<OpenAiDailyCost>,
    val lineItemTotals: List<OpenAiLineItemCost>,
)

data class OpenAiDailyCost(
    val dateLabel: String,
    val amount: Double,
    val currency: String,
)

data class OpenAiLineItemCost(
    val lineItem: String,
    val amount: Double,
    val currency: String,
)

class OpenAiCostsClient(
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: OkHttpClient = OpenAiProvider.defaultHttpClient(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun fetchLast28Days(): OpenAiCostsReport = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Inserisci una chiave API" }

        val range = queryTimeRange()
        val buckets = mutableListOf<ParsedCostsBucket>()
        var page: String? = null
        var pageCount = 0

        do {
            val urlBuilder = "${baseUrl.trimEnd('/')}/organization/costs"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("start_time", range.startTime.toString())
                .addQueryParameter("end_time", range.apiEndTimeExclusive.toString())
                .addQueryParameter("bucket_width", "1d")
                .addQueryParameter("limit", QUERY_BUCKET_LIMIT.toString())
                .addQueryParameter("group_by", "line_item")
            page?.let { urlBuilder.addQueryParameter("page", it) }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { httpResponse ->
                val responseBody = httpResponse.body?.string().orEmpty()
                if (!httpResponse.isSuccessful) {
                    throw IOException(parseErrorMessage(responseBody, httpResponse.code))
                }
                responseBody
            }

            val pageResponse = parseCostsPage(body)
            buckets += pageResponse.buckets
            page = pageResponse.nextPage?.takeIf { pageResponse.hasMore && it.isNotBlank() }
            pageCount++
        } while (page != null && pageCount < MAX_PAGES)

        buildReport(
            buckets = buckets,
            rangeStartTime = range.startTime,
            rangeEndTimeExclusive = range.displayEndTimeExclusive,
        )
    }

    internal fun parseCostsPage(body: String): ParsedCostsPage {
        val root = json.parseToJsonElement(body).jsonObject
        val buckets = root["data"]?.jsonArray
            ?.mapNotNull { element ->
                runCatching { parseBucket(element.jsonObject) }.getOrNull()
            }
            .orEmpty()
        val hasMore = root["has_more"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true
        val nextPage = root["next_page"]?.jsonPrimitive?.contentOrNull
        return ParsedCostsPage(
            buckets = buckets,
            hasMore = hasMore,
            nextPage = nextPage,
        )
    }

    internal fun buildReport(
        buckets: List<ParsedCostsBucket>,
        rangeStartTime: Long,
        rangeEndTimeExclusive: Long,
    ): OpenAiCostsReport {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)
        var totalValue = 0.0
        var currency = "usd"
        val dailyTotalsByDate = linkedMapOf<LocalDate, Double>()
        val lineItemTotals = linkedMapOf<String, Double>()

        buckets.sortedBy { it.startTime }.forEach { bucket ->
            val bucketDate = Instant.ofEpochSecond(bucket.startTime).atZone(ZoneOffset.UTC).toLocalDate()
            var dayTotal = dailyTotalsByDate[bucketDate] ?: 0.0
            bucket.results.forEach { item ->
                val amountValue = item.value ?: return@forEach
                if (!hasBillableAmount(amountValue)) return@forEach
                val amountCurrency = item.currency.ifBlank { currency }
                currency = amountCurrency
                dayTotal += amountValue
                totalValue += amountValue
                item.lineItem?.takeIf { it.isNotBlank() }?.let { lineItem ->
                    lineItemTotals[lineItem] = (lineItemTotals[lineItem] ?: 0.0) + amountValue
                }
            }
            dailyTotalsByDate[bucketDate] = dayTotal
        }

        val utc = ZoneOffset.UTC
        val startDate = Instant.ofEpochSecond(rangeStartTime).atZone(utc).toLocalDate()
        val endDate = Instant.ofEpochSecond(rangeEndTimeExclusive).atZone(utc).toLocalDate()
        val dailyCosts = buildList {
            var current = endDate.minusDays(1)
            while (!current.isBefore(startDate)) {
                add(
                    OpenAiDailyCost(
                        dateLabel = current.atStartOfDay(zoneId).format(dateFormatter),
                        amount = dailyTotalsByDate[current] ?: 0.0,
                        currency = currency,
                    ),
                )
                current = current.minusDays(1)
            }
        }

        return OpenAiCostsReport(
            totalValue = totalValue,
            currency = currency,
            dailyCosts = dailyCosts,
            lineItemTotals = lineItemTotals.entries
                .filter { hasBillableAmount(it.value) }
                .sortedByDescending { it.value }
                .map { (lineItem, amount) ->
                    OpenAiLineItemCost(
                        lineItem = lineItem,
                        amount = amount,
                        currency = currency,
                    )
                },
        )
    }

    private fun parseBucket(bucket: JsonObject): ParsedCostsBucket {
        val startTime = bucket["start_time"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: error("Missing bucket start_time")
        val resultElements = bucket["results"]?.jsonArray ?: bucket["result"]?.jsonArray
        val results = resultElements
            ?.mapNotNull { element ->
                runCatching { parseCostResult(element.jsonObject) }.getOrNull()
            }
            .orEmpty()
        return ParsedCostsBucket(startTime = startTime, results = results)
    }

    private fun parseCostResult(result: JsonObject): ParsedCostResult? {
        val objectType = result["object"]?.jsonPrimitive?.contentOrNull
        if (objectType != null && objectType != "organization.costs.result") {
            return null
        }
        val amountObject = result["amount"]?.jsonObject ?: return null
        val value = amountObject.parseNumericAmount("value") ?: return null
        if (!hasBillableAmount(value)) return null
        val currency = amountObject["currency"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val lineItem = result["line_item"]?.jsonPrimitive?.contentOrNull
        return ParsedCostResult(value = value, currency = currency, lineItem = lineItem)
    }

    private fun JsonObject.parseNumericAmount(field: String): Double? {
        val primitive = this[field]?.jsonPrimitive ?: return null
        primitive.doubleOrNull?.let { return it }
        return primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        val apiMessage = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        return apiMessage?.takeIf { it.isNotBlank() } ?: "OpenAI HTTP $code"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val QUERY_DAY_COUNT = 28L
        private const val QUERY_BUCKET_LIMIT = 31
        private const val MAX_PAGES = 12

        internal fun queryTimeRange(now: Instant = Instant.now()): CostsQueryRange {
            val utc = ZoneOffset.UTC
            val today = now.atZone(utc).toLocalDate()
            val startTime = today.minusDays(QUERY_DAY_COUNT - 1).atStartOfDay(utc).toEpochSecond()
            val displayEndTimeExclusive = today.plusDays(1).atStartOfDay(utc).toEpochSecond()
            val apiEndTimeExclusive = today.plusDays(2).atStartOfDay(utc).toEpochSecond()
            return CostsQueryRange(
                startTime = startTime,
                displayEndTimeExclusive = displayEndTimeExclusive,
                apiEndTimeExclusive = apiEndTimeExclusive,
            )
        }

        fun formatAmount(value: Double, currency: String): String {
            val symbol = when (currency.lowercase(Locale.ROOT)) {
                "usd" -> "$"
                "eur" -> "€"
                else -> currency.uppercase(Locale.ROOT) + " "
            }
            return symbol + "%.2f".format(Locale.ITALY, value)
        }

        fun hasBillableAmount(value: Double): Boolean {
            if (value <= 0.0) return false
            return (value * 100.0).roundToLong() > 0L
        }
    }
}

internal data class CostsQueryRange(
    val startTime: Long,
    val displayEndTimeExclusive: Long,
    val apiEndTimeExclusive: Long,
)

internal data class ParsedCostsPage(
    val buckets: List<ParsedCostsBucket>,
    val hasMore: Boolean,
    val nextPage: String?,
)

internal data class ParsedCostsBucket(
    val startTime: Long,
    val results: List<ParsedCostResult>,
)

internal data class ParsedCostResult(
    val value: Double,
    val currency: String,
    val lineItem: String?,
)
