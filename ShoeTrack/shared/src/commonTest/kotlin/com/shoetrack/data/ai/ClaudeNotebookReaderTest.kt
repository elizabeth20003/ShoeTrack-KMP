package com.shoetrack.data.ai

import com.shoetrack.domain.fakes.FakeAiKeyRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.model.Money
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ClaudeNotebookReaderTest {

    private val keys = FakeAiKeyRepository(stored = "secret-key")
    private val clock = FixedClock(Instant.parse("2026-11-04T15:00:00Z"))
    private var sentKey: String? = null

    private fun reader(status: HttpStatusCode = HttpStatusCode.OK, body: String): ClaudeNotebookReader {
        val engine = MockEngine { request ->
            sentKey = request.headers["x-api-key"]
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ClaudeNotebookReader(client, keys, clock)
    }

    private fun reply(text: String) = """{"content":[{"type":"text","text":${Json.encodeToString(String.serializer(), text)}}]}"""

    @Test
    fun readsTheEntryTheModelSends() = runTest {
        val json = """{"customerName":"Paola Flor","productDescription":"Vans blanco/negro alto T37","totalPrice":80000,"paidAmount":60000,"lastPaymentDate":"2026-11-03"}"""

        val entry = reader(body = reply(json)).read(byteArrayOf(1, 2, 3))

        assertEquals("secret-key", sentKey)
        assertEquals("Paola Flor", entry.customerName)
        assertEquals("Vans blanco/negro alto T37", entry.productDescription)
        assertEquals(Money.fromUnits(80_000), entry.totalPrice)
        assertEquals(Money.fromUnits(60_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 11, 3), entry.lastPaymentDate)
    }

    @Test
    fun toleratesTextAndNullsAroundTheJson() = runTest {
        val text = "Here it is:\n```json\n{\"customerName\":\"Alba\",\"totalPrice\":null,\"lastPaymentDate\":\"not a date\"}\n```"

        val entry = reader(body = reply(text)).read(byteArrayOf(1))

        assertEquals("Alba", entry.customerName)
        assertEquals(null, entry.totalPrice)
        assertEquals(null, entry.lastPaymentDate)
    }

    @Test
    fun failsWhenTheServiceRejectsTheRequest() = runTest {
        assertFailsWith<NotebookReadException> {
            reader(HttpStatusCode.Unauthorized, """{"error":"invalid key"}""").read(byteArrayOf(1))
        }
    }

    @Test
    fun failsWhenThereIsNoKey() = runTest {
        keys.stored = null

        assertFailsWith<NotebookReadException> { reader(body = reply("{}")).read(byteArrayOf(1)) }
    }
}
