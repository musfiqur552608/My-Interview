package com.freedu.myinterviews.ai

import com.freedu.myinterviews.util.MatchScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Interview coach. Offline-first: question sets are generated on-device from the
 * role title + JD keywords (no account, no network). Optionally, if the user pastes
 * an OpenAI-compatible API key in Settings, [coachReply] upgrades follow-up feedback
 * to a real model call — failures always fall back to offline content.
 */
object AiCoach {

    private val ROLE_BANKS: List<Pair<String, List<String>>> = listOf(
        "android" to listOf(
            "Explain the Activity/Fragment lifecycle and a bug you fixed around it.",
            "How do you handle configuration changes without losing UI state?",
            "Compare MVVM and MVI for a large Android codebase.",
            "How would you debug a RecyclerView jank issue in production?"
        ),
        "ios" to listOf(
            "Explain ARC and a retain cycle you debugged.",
            "How do you manage state in SwiftUI at scale?",
            "Describe your approach to background tasks and push notifications."
        ),
        "backend" to listOf(
            "Design a rate limiter for a public API.",
            "SQL vs NoSQL: how do you choose for a new service?",
            "How do you trace a latency spike across microservices?"
        ),
        "frontend" to listOf(
            "How does the browser render a page, and where do bottlenecks hide?",
            "Explain your state-management strategy in a large SPA.",
            "How do you guarantee accessibility in your components?"
        ),
        "data" to listOf(
            "Walk me through a pipeline you built from ingestion to serving.",
            "How do you validate data quality before it reaches stakeholders?",
            "Explain a trade-off between batch and streaming for a past project."
        ),
        "ml" to listOf(
            "How do you detect and fix training-serving skew?",
            "Explain how you would evaluate a model beyond accuracy.",
            "Describe taking an ML model from notebook to production."
        ),
        "devops" to listOf(
            "Walk me through your CI/CD pipeline and rollback strategy.",
            "How would you debug a Kubernetes CrashLoopBackOff?",
            "Explain infrastructure-as-code practices you follow."
        ),
        "qa" to listOf(
            "How do you decide what to automate vs test manually?",
            "Describe a flaky-test strategy that worked for you.",
            "How do you test an API contract across services?"
        ),
        "manager" to listOf(
            "How do you run 1:1s that engineers actually value?",
            "Describe resolving a conflict between two senior engineers.",
            "How do you balance delivery pressure with tech debt?"
        )
    )

    private val GENERIC_TECH = listOf(
        "Tell me about the most challenging bug you fixed recently.",
        "How do you keep code quality high under deadline pressure?",
        "Describe a system you designed and what you would change today."
    )

    private val BEHAVIORAL = listOf(
        "Tell me about yourself in two minutes.",
        "Why this company and why this role?",
        "Describe a disagreement with a teammate and how it resolved.",
        "Tell me about a failure and what you learned.",
        "Where do you want to grow in the next year?"
    )

    /** Deterministic offline question set: role bank + JD keywords + behavioral. */
    fun generateQuestions(jobTitle: String, jd: String, count: Int = 8): List<String> {
        val t = jobTitle.lowercase()
        val roleQs = ROLE_BANKS.firstOrNull { (k, _) -> k in t }?.second.orEmpty()
        val jdKeys = MatchScore.keywords("$jobTitle $jd", 12).filter { it.length > 2 }
        val jdQs = jdKeys.take(3).map { "Walk me through your hands-on experience with $it." }
        return (roleQs.take(3) + jdQs + GENERIC_TECH.take(2) + BEHAVIORAL.take(3))
            .distinct().take(count.coerceIn(3, 12))
    }

    /** Average of 1–5 self-ratings → 0–100 session score. */
    fun scorePractice(ratings: List<Int>): Int {
        if (ratings.isEmpty()) return 0
        return ((ratings.average() / 5.0) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Optional LLM feedback. Returns null on any failure (no key, offline, bad
     * response) so callers silently fall back to offline coaching.
     */
    suspend fun coachReply(
        apiKey: String,
        jobTitle: String,
        history: List<Pair<String, String>> // question → your answer
    ): String? {
        if (apiKey.isBlank() || history.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val msgs = JSONArray()
                    .put(JSONObject().put("role", "system").put(
                        "content",
                        "You are a concise interview coach for a $jobTitle candidate. " +
                            "Give 3 bullets: strengths, one fix, and a model one-liner. Under 120 words."
                    ))
                history.takeLast(4).forEach { (q, a) ->
                    msgs.put(JSONObject().put("role", "user").put("content", "Q: $q\nMy answer: $a"))
                }
                val body = JSONObject()
                    .put("model", "gpt-4o-mini")
                    .put("messages", msgs)
                    .put("max_tokens", 220)
                    .toString()
                val conn = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000; readTimeout = 20_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                }
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode !in 200..299) return@runCatching null
                val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                JSONObject(text).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content")
            }.getOrNull()
        }
    }
}
