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

    suspend fun generateFashionSketch(promptText: String): AIDesignResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder! Running robust local rule-based creative fashion sketch generator.")
            return@withContext generateLocalSketch(promptText)
        }

        val systemPrompt = """
            You are an elite haute couture tech pack creator and bespoke tailor drafting sketches.
            Translate the user's plain text description of a clothing item into a precise, high-end tailors design sketch specification.
            
            Return a valid raw JSON object ONLY, containing these exact keys:
            "title" (luxurious brand-like English/Arabic title, e.g. "Emerald Dynasty Trenchcoat"),
            "description" (detailed rich tailor analysis of how the folds drape, fabric movement, and styling),
            "suggestedFabrics" (string list of 2-3 specific fabrics),
            "tailoringSecrets" (string list of 3 craftsmanship secrets),
            "estimatedEGPPrice" (realistic fashion house custom EGP price),
            "assemblySteps" (string list of 5 assembly steps),
            "garmentType" (must be exactly one of: "shirt", "dress", "jacket", "kaftan", "suit"),
            "primaryColorHex" (the main garment color found or inferred from prompt in hex format, e.g. "#1B4F72"),
            "secondaryColorHex" (the accent/zipper/stitching color in hex format, e.g. "#F39C12"),
            "sleeveStyle" (must be exactly one of: "long", "short", "sleeveless"),
            "necklineStyle" (must be exactly one of: "high-collar", "v-neck", "round", "lapel"),
            "designAccents" (string list of 2-3 specific decorative accents like "gold cuffs", "brass button line", "asymmetric zipper")

            Do not prepend or append markdown code block ticks. Output only raw JSON.
        """.trimIndent()

        val fullPrompt = "User's request: $promptText"

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", fullPrompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
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
                    Log.e(TAG, "API request failed with code ${response.code}. Falling back to local sketch.")
                    return@withContext generateLocalSketch(promptText)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val textResponse = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                val cleanJson = if (textResponse.startsWith("```")) {
                    textResponse.removeSurrounding("```json", "```").trim()
                } else textResponse

                val parsed = JSONObject(cleanJson)
                val title = parsed.optString("title", "Bespoke Silhouette Sketch")
                val descriptionText = parsed.optString("description", "A custom conceptual sketch built to details.")
                val suggestedFabrics = mutableListOf<String>()
                parsed.optJSONArray("suggestedFabrics")?.let { arr ->
                    for (i in 0 until arr.length()) suggestedFabrics.add(arr.getString(i))
                }
                val tailoringSecrets = mutableListOf<String>()
                parsed.optJSONArray("tailoringSecrets")?.let { arr ->
                    for (i in 0 until arr.length()) tailoringSecrets.add(arr.getString(i))
                }
                val estimatedEGPPrice = parsed.optDouble("estimatedEGPPrice", 3200.0)
                val assemblySteps = mutableListOf<String>()
                parsed.optJSONArray("assemblySteps")?.let { arr ->
                    for (i in 0 until arr.length()) assemblySteps.add(arr.getString(i))
                }

                // Sketch Specific Parsing
                val garmentType = parsed.optString("garmentType", "shirt")
                val primaryColorHex = parsed.optString("primaryColorHex", "#2C3E50")
                val secondaryColorHex = parsed.optString("secondaryColorHex", "#E67E22")
                val sleeveStyle = parsed.optString("sleeveStyle", "long")
                val necklineStyle = parsed.optString("necklineStyle", "round")
                val designAccents = mutableListOf<String>()
                parsed.optJSONArray("designAccents")?.let { arr ->
                    for (i in 0 until arr.length()) designAccents.add(arr.getString(i))
                }

                AIDesignResult(
                    title = title,
                    description = descriptionText,
                    suggestedFabrics = if (suggestedFabrics.isEmpty()) listOf("Premium Linen") else suggestedFabrics,
                    tailoringSecrets = if (tailoringSecrets.isEmpty()) listOf("French seams") else tailoringSecrets,
                    estimatedPrice = estimatedEGPPrice,
                    assemblySteps = if (assemblySteps.isEmpty()) listOf("Measurement drafting", "Precision cutting", "Assembling side seams") else assemblySteps,
                    isApiPowered = true,
                    garmentType = garmentType,
                    primaryColorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex,
                    sleeveStyle = sleeveStyle,
                    necklineStyle = necklineStyle,
                    designAccents = if (designAccents.isEmpty()) listOf("Handcraft seams") else designAccents
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Sketch call crashed: ${e.message}. Falling back gracefully.", e)
            generateLocalSketch(promptText)
        }
    }

    private fun generateLocalSketch(promptText: String): AIDesignResult {
        val lower = promptText.lowercase()
        
        val garmentType = when {
            lower.contains("kaftan") || lower.contains("قفطان") || lower.contains("robe") -> "kaftan"
            lower.contains("dress") || lower.contains("فستان") || lower.contains("skirt") -> "dress"
            lower.contains("jacket") || lower.contains("جاكيت") || lower.contains("coat") || lower.contains("معطف") -> "jacket"
            lower.contains("suit") || lower.contains("بدلة") || lower.contains("blazer") -> "suit"
            else -> "shirt"
        }

        val pColor = when {
            lower.contains("green") || lower.contains("أخضر") || lower.contains("emerald") -> "#1E5E3A"
            lower.contains("blue") || lower.contains("أزرق") || lower.contains("navy") || lower.contains("كحلي") -> "#2B4C7E"
            lower.contains("red") || lower.contains("أحمر") || lower.contains("burgundy") -> "#8B1E1E"
            lower.contains("gold") || lower.contains("ذهبي") -> "#D4AF37"
            lower.contains("white") || lower.contains("أبيض") -> "#F5F5F5"
            lower.contains("black") || lower.contains("أسود") -> "#1A1A1A"
            lower.contains("yellow") || lower.contains("أصفر") -> "#E1B12C"
            lower.contains("purple") || lower.contains("بنفسجي") -> "#6D214F"
            else -> "#2C3E50"
        }

        val sColor = when {
            lower.contains("gold") || lower.contains("ذهبي") || lower.contains("yellow") -> "#D4AF37"
            lower.contains("silver") || lower.contains("فضي") || lower.contains("white") -> "#BDC3C7"
            lower.contains("navy") || lower.contains("black") -> "#2C3E50"
            else -> "#E67E22"
        }

        val sleeve = when {
            lower.contains("sleeveless") || lower.contains("بدون أكمام") -> "sleeveless"
            lower.contains("short") || lower.contains("قصيرة") -> "short"
            else -> "long"
        }

        val neckline = when {
            lower.contains("high-collar") || lower.contains("high collar") || lower.contains("high neck") || lower.contains("ياقة عالية") || lower.contains("ياقة مرتفعة") || lower.contains("mandarin") -> "high-collar"
            lower.contains("v-neck") || lower.contains("v neck") || lower.contains("ياقة v") -> "v-neck"
            lower.contains("lapel") || lower.contains("blazer") || lower.contains("collar") || lower.contains("بدلة") -> "lapel"
            else -> "round"
        }

        val accents = mutableListOf<String>()
        if (lower.contains("button") || lower.contains("أزرار")) accents.add("Premium Tailored Buttons")
        if (lower.contains("zip") || lower.contains("سحاب") || lower.contains("سوستة")) accents.add("Polished Brass Zipper")
        if (lower.contains("embroid") || lower.contains("تطريز") || lower.contains("embellish")) accents.add("Fine Needlework Embroidery")
        if (lower.contains("pocket") || lower.contains("جيب") || lower.contains("جيوب")) accents.add("Flapped Utility Pockets")
        if (accents.isEmpty()) {
            accents.add("Hand-finished Stitching")
            accents.add("Contrast edge piping")
        }

        val designTitle = "Bespoke ${garmentType.replaceFirstChar { it.uppercase() }} Concept Sketch"
        val desc = "Custom concept curated based on: \"$promptText\". Fine drafted in proportion class 1:10 with precise seam specifications."

        return AIDesignResult(
            title = designTitle,
            description = desc,
            suggestedFabrics = listOf("Giza Premium Cotton", "Pure Italian Linen", "Mulberry Silk Blend"),
            tailoringSecrets = listOf("French seams to prevent inside skin abrasion", "Reinforced interlining around structural collar joints", "Dual-needle tension stitches"),
            estimatedPrice = 2800.0,
            assemblySteps = listOf(
                "Measure standard width and sleeve coordinates.",
                "Draw patterns on standard craft paper using custom geometry parameters.",
                "Cut out primary and secondary grain panels manually.",
                "Connect shoulders with soft interlocking blind-stitch threads.",
                "Detail the cuffs and neckline according to sketch blueprint specifications."
            ),
            isApiPowered = false,
            garmentType = garmentType,
            primaryColorHex = pColor,
            secondaryColorHex = sColor,
            sleeveStyle = sleeve,
            necklineStyle = neckline,
            designAccents = accents
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
    val isApiPowered: Boolean,
    val garmentType: String? = null,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val sleeveStyle: String? = null,
    val necklineStyle: String? = null,
    val designAccents: List<String>? = null
)
