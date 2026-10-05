package com.maksimowiczm.foodyou.photo

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class KtorPhotoAnalyzer : PhotoAnalyzer {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Mismos fallbacks que AsistenteVoz: Tailscale -> LAN -> 10.0.0.1
    private val serverUrls = listOf(
        "http://100.66.224.38:8765/analyze-food",
        "http://192.168.10.214:8765/analyze-food",
        "http://10.0.0.1:8765/analyze-food"
    )

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 90000
            connectTimeoutMillis = 8000
            socketTimeoutMillis = 90000
        }
    }

    override suspend fun analyze(imageBytes: ByteArray, fileName: String): Result<PhotoFoodResult> {
        var lastError: Throwable? = null
        for (url in serverUrls) {
            try {
                val text = postImage(url, imageBytes, fileName)
                val parsed = parseResult(text)
                if (parsed.error != null && parsed.alimento == "no es comida" && parsed.kcal == 0) {
                    // si es "no es comida" intenta siguiente? no, es valido
                }
                return Result.success(parsed)
            } catch (e: Exception) {
                lastError = e
                // intenta siguiente URL
            }
        }
        return Result.failure(lastError ?: Exception("No se pudo conectar a Yui (100.66.224.38:8765). Verifica Tailscale y que yui-voice.service este activo."))
    }

    private suspend fun postImage(url: String, bytes: ByteArray, fileName: String): String {
        val ct = when {
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".webp", true) -> "image/webp"
            else -> "image/jpeg"
        }
        val resp = client.post(url) {
            setBody(MultiPartFormDataContent(formData {
                append("image", bytes, Headers.build {
                    append(HttpHeaders.ContentType, ct)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }))
        }
        val body = resp.bodyAsText()
        if (resp.status.value !in 200..299) {
            throw Exception("Yui ${resp.status.value}: $body")
        }
        return body
    }

    private fun parseResult(text: String): PhotoFoodResult {
        val cleaned = text.trim()
        // buscar JSON object
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        val jtxt = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
        try {
            val r = json.decodeFromString<PhotoFoodResult>(jtxt)
            // normalizar
            if (r.error != null) return r
            return r
        } catch (e: Exception) {
            // intentar parsear error generico
            try {
                val m = json.parseToJsonElement(jtxt)
                val obj = m.let { it as? kotlinx.serialization.json.JsonObject }
                if (obj != null && obj["error"] != null) {
                    return PhotoFoodResult(alimento = "error", kcal = 0, proteinas = 0.0, grasas = 0.0, carbos = 0.0, error = obj["error"].toString())
                }
            } catch (_: Exception) {}
            throw Exception("Respuesta no es JSON: $text")
        }
    }
}
