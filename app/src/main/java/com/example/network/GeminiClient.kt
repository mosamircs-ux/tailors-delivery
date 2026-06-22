package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateFashionDesign(
        occasion: String,
        style: String,
        bodyType: String,
        fabric: String,
        budget: Double,
        colors: List<String>
    ): AIDesignResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder! Running robust local rule-based creative fashion generator.")
            return@withContext generateLocalDesign(occasion, style, bodyType, fabric, budget, colors)
        }

        val prompt = """
            You are a master haute couture fashion designer and professional tailor.
            Generate a detailed custom clothing item design specification based on these parameters:
            - Occasion/Purpose: $occasion
            - Aesthetic Style: $style
            - Body Type/Silhouette: $bodyType
            - Preferred Fabric: $fabric
            - Estimated Customer Budget: $budget EGP
            - Chosen Colors: ${colors.joinToString(", ")}

            Please reply with a valid raw JSON object ONLY, containing these exact keys:
            "title" (a cool, luxurious, descriptive Arabic/English title, e.g. "Royal Nile Linen Shirt" or "القميص الملكي الكاجوال"),
            "description" (a highly elegant, professional and visually rich description in English or Arabic of why this is perfect, detailing the cut, look, and drapes),
            "suggestedFabrics" (a string list of 2-3 specific fabric names suitable),
            "tailoringSecrets" (a string list of 3-4 professional tailor craftsmanship touches, e.g. "French seams", "Hand-rolled hems", "Contrast pocket stitching"),
            "estimatedEGPPrice" (a realistic number representing EGP cost, strictly within budget),
            "assemblySteps" (a string list of 5 step-by-step phases of drafting, cutting, and stitching that the tailor will perform).

            Do not prepend or append markdown code block ticks. Output only raw JSON.
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                // Optional generation config to enforce JSON
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API request failed with code ${response.code}. Falling back to local designer.")
                    return@withContext generateLocalDesign(occasion, style, bodyType, fabric, budget, colors)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val textResponse = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                // Parsed text is supposed to be JSON
                val cleanJson = if (textResponse.startsWith("```")) {
                    textResponse.removeSurrounding("```json", "```").trim()
                } else textResponse

                val parsed = JSONObject(cleanJson)
                val title = parsed.optString("title", "Custom Crafted Fashion")
                val description = parsed.optString("description", "A gorgeous bespoke design fits perfectly to your body shape.")
                val suggestedFabrics = mutableListOf<String>()
                parsed.optJSONArray("suggestedFabrics")?.let { arr ->
                    for (i in 0 until arr.length()) suggestedFabrics.add(arr.getString(i))
                }
                val tailoringSecrets = mutableListOf<String>()
                parsed.optJSONArray("tailoringSecrets")?.let { arr ->
                    for (i in 0 until arr.length()) tailoringSecrets.add(arr.getString(i))
                }
                val estimatedEGPPrice = parsed.optDouble("estimatedEGPPrice", budget * 0.9)
                val assemblySteps = mutableListOf<String>()
                parsed.optJSONArray("assemblySteps")?.let { arr ->
                    for (i in 0 until arr.length()) assemblySteps.add(arr.getString(i))
                }

                AIDesignResult(
                    title = title,
                    description = description,
                    suggestedFabrics = if (suggestedFabrics.isEmpty()) listOf(fabric) else suggestedFabrics,
                    tailoringSecrets = if (tailoringSecrets.isEmpty()) listOf("Premium matching threads", "Reinforced collars") else tailoringSecrets,
                    estimatedPrice = estimatedEGPPrice,
                    assemblySteps = if (assemblySteps.isEmpty()) listOf("Drafting measurements", "Laying fabric", "Precision cutting", "Assembling side seams", "Final buttoning") else assemblySteps,
                    isApiPowered = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call crashed: ${e.message}. Falling back gracefully.", e)
            generateLocalDesign(occasion, style, bodyType, fabric, budget, colors)
        }
    }

    private fun generateLocalDesign(
        occasion: String,
        style: String,
        bodyType: String,
        fabric: String,
        budget: Double,
        colors: List<String>
    ): AIDesignResult {
        val title = when (style.lowercase()) {
            "elegant" -> "الإمبراطور - Royal $occasion $fabric Masterpiece"
            "minimalist" -> "السحاب - Modern $occasion $fabric Minimal"
            "bold" -> "الجريء - Vanguard Asymmetric $fabric Wear"
            else -> "الأصيل - Ghoureya Hand-Crafted $occasion Design"
        }

        val primaryColor = colors.firstOrNull() ?: "#2C3E50"

        val descriptionStr = "A premium hand-crafted outfit tailored specifically for a $bodyType frame, " +
                "using the finest $fabric fabrics with the chosen $primaryColor theme. Specially designed " +
                "for a high-end $occasion, it features an elegant tailored cut that guarantees exceptional comfort, " +
                "seamlessly transitioning from premium formal to relaxed weekend wear."

        val fabrics = when (occasion.lowercase()) {
            "wedding", "formal" -> listOf("Egyptian Cotton 120s", "Mulberry Sheer Silk", "Italian Merino Wool")
            else -> listOf("Belgian Pure Linen", "Kashmiri Light Cotton", "Heavy Indigo Denim")
        }

        val secrets = listOf(
            "French seams for a clean internal drape & zero fraying",
            "Hand-basted chest canvas that conforms perfectly over time",
            "Slightly padded hand-rolled horn buttons for strong visual weight",
            "Durable dual-stitch reinforcement along major seams"
        )

        val price = budget * 0.85

        val steps = listOf(
            "Phase 1 (Drafting): Draft customized pattern sheets based on your height and width indices.",
            "Phase 2 (Pre-shrunk): Pre-shrink and press the custom $fabric fabric with modern steam plates.",
            "Phase 3 (Cutting): Align fabric grains and carry out single-layered manual scissor cutting.",
            "Phase 4 (Assembling): Stitch the cuffs, collars, and yoke with high-tension premium poly-threads.",
            "Phase 5 (Finishing): Clean any loose threads, press with heavy hot-coals iron and attach custom buttons."
        )

        return AIDesignResult(
            title = title,
            description = descriptionStr,
            suggestedFabrics = fabrics,
            tailoringSecrets = secrets,
            estimatedPrice = price,
            assemblySteps = steps,
            isApiPowered = false
        )
    }
}

data class AIDesignResult(
    val title: String,
    val description: String,
    val suggestedFabrics: List<String>,
    val tailoringSecrets: List<String>,
    val estimatedPrice: Double,
    val assemblySteps: List<String>,
    val isApiPowered: Boolean
)
