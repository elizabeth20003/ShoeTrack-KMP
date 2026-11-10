package com.shoetrack.data.ai

import com.shoetrack.domain.model.Money
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.domain.repository.AiKeyRepository
import com.shoetrack.domain.service.NotebookReader
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.time.Clock

class NotebookReadException(message: String) : Exception(message)

/**
 * Reads a notebook photo with a vision model. Unlike on-device text recognition it copes with
 * cursive handwriting, and it understands the notebook's layout, so no text parsing is needed.
 */
internal class ClaudeNotebookReader(private val client: HttpClient, private val keys: AiKeyRepository, private val clock: Clock) :
    NotebookReader {
    override suspend fun read(image: ByteArray): NotebookEntry {
        val key = keys.key() ?: throw NotebookReadException("No API key saved")
        val response =
            client.post(ENDPOINT) {
                header("x-api-key", key)
                header("anthropic-version", API_VERSION)
                contentType(ContentType.Application.Json)
                setBody(request(image))
            }
        if (!response.status.isSuccess()) throw NotebookReadException("The service answered ${response.status}")
        val reply =
            response.body<Reply>().content.firstNotNullOfOrNull { it.text }
                ?: throw NotebookReadException("The service sent no text")
        return toEntry(reply)
    }

    private fun request(image: ByteArray): Request {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return Request(
            model = MODEL,
            maxTokens = MAX_TOKENS,
            messages =
            listOf(
                Message(
                    role = "user",
                    content =
                    listOf(
                        Block(
                            type = "image",
                            source = Source(type = "base64", mediaType = "image/jpeg", data = Base64.encode(image)),
                        ),
                        Block(type = "text", text = prompt(today)),
                    ),
                ),
            ),
        )
    }

    private fun toEntry(reply: String): NotebookEntry {
        // The model is asked for bare JSON, but tolerate text or a code fence around it.
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end < start) throw NotebookReadException("The service did not send an entry")
        val read = json.decodeFromString<ReadEntry>(reply.substring(start, end + 1))
        return NotebookEntry(
            customerName = read.customerName.orEmpty().trim(),
            productDescription = read.productDescription.orEmpty().trim(),
            totalPrice = read.totalPrice?.takeIf { it > 0 }?.let { Money.fromUnits(it) },
            paidAmount = read.paidAmount?.takeIf { it >= 0 }?.let { Money.fromUnits(it) },
            lastPaymentDate = read.lastPaymentDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }

    @Serializable
    private data class Request(val model: String, @SerialName("max_tokens") val maxTokens: Int, val messages: List<Message>)

    @Serializable
    private data class Message(val role: String, val content: List<Block>)

    @Serializable
    private data class Block(val type: String, val text: String? = null, val source: Source? = null)

    @Serializable
    private data class Source(val type: String, @SerialName("media_type") val mediaType: String, val data: String)

    @Serializable
    private data class Reply(val content: List<ReplyBlock>)

    @Serializable
    private data class ReplyBlock(val text: String? = null)

    @Serializable
    private data class ReadEntry(
        val customerName: String? = null,
        val productDescription: String? = null,
        val totalPrice: Long? = null,
        val paidAmount: Long? = null,
        val lastPaymentDate: String? = null,
    )

    private companion object {
        const val ENDPOINT = "https://api.anthropic.com/v1/messages"
        const val API_VERSION = "2023-06-01"
        const val MODEL = "claude-haiku-5-5"
        const val MAX_TOKENS = 400

        val json = Json { ignoreUnknownKeys = true }

        fun prompt(today: LocalDate) =
            """
            This is a photo of the handwritten notebook of a shoe store in Colombia, where customers pay for shoes in installments (layaways). Amounts are Colombian pesos and use "." for thousands.
            Each layaway starts with a date (day-month-year, like 27-10-26), then the customer's first and last name, then the shoes (brand, model, color, size written as T or Talla), then the total price. Payments follow as "Abona 50.000" with the remaining "saldo 30.000". Later payments are on their own dated lines below.
            A layaway crossed out with a line is already paid: ignore it and read the last layaway that is not crossed out.
            Today is $today. The handwriting is cursive, so give your best reading of the name.
            Reply with only a JSON object, no other text:
            {"customerName": "...", "productDescription": "...", "totalPrice": whole pesos or null, "paidAmount": whole pesos or null, "lastPaymentDate": "YYYY-MM-DD" or null}
            paidAmount is everything paid so far: the total minus the last saldo, or the sum of the payments. lastPaymentDate is the date of the most recent payment line. Use null for anything you cannot read.
            """.trimIndent()
    }
}
