package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Data class to represent extracted ticket data
    data class ParsedTicket(
        val description: String,
        val amount: Double,
        val category: String,
        val notes: String
    )

    /**
     * Uses Gemini to parse a raw ticket text or informal purchase description.
     * Returns a ParsedTicket object or null if it fails.
     */
    suspend fun parseTicketText(rawText: String): ParsedTicket? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is the default placeholder.")
            return@withContext null
        }

        val prompt = """
            You are an expert bill and receipt parsing AI. 
            Parse the following raw text or purchase description from a user, and extract:
            1. A concise, clean title/description for the expense (in Spanish).
            2. The total amount as a decimal number (if no currency is specified, assume standard numerical value).
            3. A suggested category (MUST be one of these exact values: "Comida", "Supermercado", "Alojamiento", "Transporte", "Combustible", "Servicios", "Alquiler", "Otros").
            4. A short notes summary or interesting detail (in Spanish).

            Text to parse:
            "$rawText"

            Respond ONLY with a valid JSON object matching this structure:
            {
              "description": "Short clean description",
              "amount": 1250.50,
              "category": "Comida",
              "notes": "Extracted detail or empty string"
            }
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code ${response.code}: ${response.body?.string()}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Gemini raw response: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val textResult = parts.getJSONObject(0).optString("text")
                        
                        // Parse the JSON result from the text
                        val cleanJson = JSONObject(textResult.trim())
                        return@withContext ParsedTicket(
                            description = cleanJson.optString("description", "Gasto escaneado"),
                            amount = cleanJson.optDouble("amount", 0.0),
                            category = cleanJson.optString("category", "Otros"),
                            notes = cleanJson.optString("notes", "")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
        }
        return@withContext null
    }

    /**
     * Uses Gemini to perform OCR on a physical ticket image provided as Base64.
     * Returns a ParsedTicket object or null if it fails.
     */
    suspend fun parseTicketImage(base64Image: String, mimeType: String = "image/jpeg"): ParsedTicket? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is the default placeholder.")
            return@withContext null
        }

        val prompt = """
            You are an expert ticket scanning OCR AI. 
            Analyze the attached image of a receipt or invoice, extract the details, and return:
            1. A concise, clean title/description of the expense (in Spanish, like "Cena en Kokoro Sushi" or "Compra en Coto").
            2. The total amount as a decimal number.
            3. A suggested category (MUST be one of these exact values: "Comida", "Supermercado", "Alojamiento", "Transporte", "Combustible", "Servicios", "Alquiler", "Otros").
            4. A short notes summary or breakdown of items (in Spanish, listing items like: Pizza: $3000, Bebidas: $1200).

            Respond ONLY with a valid JSON object matching this structure:
            {
              "description": "Short clean description",
              "amount": 1250.50,
              "category": "Comida",
              "notes": "Desglose de productos o detalle"
            }
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Multimodal request failed with code ${response.code}: ${response.body?.string()}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Gemini multimodal raw response: $responseBody")

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val textResult = parts.getJSONObject(0).optString("text")
                        
                        // Parse the JSON result from the text
                        val cleanJson = JSONObject(textResult.trim())
                        return@withContext ParsedTicket(
                            description = cleanJson.optString("description", "Ticket Escaneado"),
                            amount = cleanJson.optDouble("amount", 0.0),
                            category = cleanJson.optString("category", "Otros"),
                            notes = cleanJson.optString("notes", "")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini multimodal call: ${e.message}", e)
        }
        return@withContext null
    }

    /**
     * Autodetects the category based on description, using a combination of fast offline keywords
     * and a quick Gemini call fallback.
     */
    fun getCategoryByHeuristics(description: String): String {
        val desc = description.lowercase()
        return when {
            desc.contains("cena") || desc.contains("almuerzo") || desc.contains("restaurante") || desc.contains("bar") || desc.contains("pizza") || desc.contains("hamburguesa") || desc.contains("sushi") || desc.contains("comida") || desc.contains("facturas") || desc.contains("desayuno") -> "Comida"
            desc.contains("super") || desc.contains("carrefour") || desc.contains("coto") || desc.contains("dia") || desc.contains("chino") || desc.contains("mercado") || desc.contains("almacen") || desc.contains("verduleria") || desc.contains("carniceria") -> "Supermercado"
            desc.contains("hotel") || desc.contains("alojamiento") || desc.contains("airbnb") || desc.contains("hostel") || desc.contains("cabaña") || desc.contains("hospedaje") -> "Alojamiento"
            desc.contains("taxi") || desc.contains("uber") || desc.contains("cabify") || desc.contains("colectivo") || desc.contains("sube") || desc.contains("tren") || desc.contains("pasaje") || desc.contains("vuelo") || desc.contains("viaje") -> "Transporte"
            desc.contains("nafta") || desc.contains("combustible") || desc.contains("nafta") || desc.contains("shell") || desc.contains("ypf") || desc.contains("axion") || desc.contains("gasoil") || desc.contains("peaje") -> "Combustible"
            desc.contains("luz") || desc.contains("agua") || desc.contains("gas") || desc.contains("internet") || desc.contains("telefono") || desc.contains("wifi") || desc.contains("expensas") || desc.contains("suscripcion") || desc.contains("netflix") || desc.contains("spotify") -> "Servicios"
            desc.contains("alquiler") || desc.contains("departamento") || desc.contains("renta") || desc.contains("seña") -> "Alquiler"
            else -> "Otros"
        }
    }

    private fun normalizeCurrencyCode(code: String): String {
        return when (code.trim().uppercase()) {
            "$" -> "ARS"
            "U\$D", "USD" -> "USD"
            "€", "EUR" -> "EUR"
            "ARS" -> "ARS"
            "CLP" -> "CLP"
            "MXN" -> "MXN"
            "COP" -> "COP"
            "PEN" -> "PEN"
            "UYU" -> "UYU"
            else -> code.trim().uppercase()
        }
    }

    /**
     * Fetches real-time exchange rate between two currencies using open.er-api.com.
     * Returns null if the API fails or is offline.
     */
    suspend fun fetchExchangeRate(from: String, to: String): Double? = withContext(Dispatchers.IO) {
        val fromCode = normalizeCurrencyCode(from)
        val toCode = normalizeCurrencyCode(to)
        if (fromCode == toCode) return@withContext 1.0

        try {
            val url = "https://open.er-api.com/v6/latest/$fromCode"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        if (json.optString("result") == "success") {
                            val rates = json.optJSONObject("rates")
                            if (rates != null && rates.has(toCode)) {
                                val rate = rates.optDouble(toCode)
                                Log.d(TAG, "Fetched exchange rate from API: 1 $fromCode = $rate $toCode")
                                return@withContext rate
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exchange rate from API: ${e.message}")
        }
        return@withContext null
    }
}
