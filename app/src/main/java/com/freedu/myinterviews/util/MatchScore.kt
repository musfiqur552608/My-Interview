package com.freedu.myinterviews.util

/**
 * Resume ↔ Job-description match scoring. Pure Kotlin, fully offline.
 * Keyword overlap: top JD terms vs resume vocabulary → score + missing skills.
 */
object MatchScore {

    private val STOPWORDS = setOf(
        "the", "and", "for", "with", "you", "your", "our", "are", "will", "have",
        "has", "this", "that", "from", "they", "their", "them", "who", "whom",
        "about", "into", "over", "after", "before", "between", "through", "during",
        "such", "than", "then", "also", "just", "like", "well", "able", "work",
        "working", "team", "teams", "join", "looking", "role", "day", "new",
        "including", "plus", "per", "etc", "within", "across", "both", "each",
        "more", "most", "other", "some", "what", "when", "where", "which",
        "while", "would", "could", "should", "using", "used", "use", "strong",
        "great", "good", "best", "top", "etc", "via", "all", "any", "can",
        "our", "out", "own", "same", "too", "very", "its", "ity", "ion"
    )

    /** Tech terms get a frequency boost so real skills outrank filler words. */
    private val TECH = setOf(
        "kotlin", "java", "compose", "android", "ios", "swift", "flutter", "react",
        "angular", "vue", "typescript", "javascript", "python", "golang", "rust",
        "sql", "nosql", "postgres", "mysql", "mongodb", "redis", "kafka", "graphql",
        "rest", "grpc", "aws", "gcp", "azure", "docker", "kubernetes", "terraform",
        "ci", "cd", "jenkins", "git", "mvvm", "mvi", "clean", "dsa", "system",
        "design", "microservices", "testing", "junit", "espresso", "coroutines",
        "flow", "rxjava", "dagger", "hilt", "room", "retrofit", "firebase",
        "ml", "ai", "tensorflow", "pytorch", "pandas", "spark", "hadoop",
        "agile", "scrum", "figma", "linux", "nginx", "spring", "django",
        "node", "express", "rails", "csharp", "dotnet", "php", "ruby", "scala"
    )

    fun keywords(text: String, top: Int = 30): List<String> {
        if (text.isBlank()) return emptyList()
        val freq = mutableMapOf<String, Int>()
        Regex("[a-zA-Z][a-zA-Z0-9+#.]*").findAll(text.lowercase()).forEach { m ->
            val w = m.value.trimEnd('.', '#')
            if (w.length >= 2 && w !in STOPWORDS) {
                freq[w] = (freq[w] ?: 0) + if (w in TECH) 3 else 1
            }
        }
        return freq.entries.sortedByDescending { it.value }.take(top).map { it.key }
    }

    data class Result(
        val score: Int, // 0..100
        val matched: List<String>,
        val missing: List<String>
    )

    fun score(resume: String, jd: String): Result {
        val jdKeys = keywords(jd, 30)
        if (jdKeys.isEmpty()) return Result(0, emptyList(), emptyList())
        if (resume.isBlank()) return Result(0, emptyList(), jdKeys)
        val resumeVocab = keywords(resume, 400).toSet()
        val matched = jdKeys.filter { it in resumeVocab }
        val missing = jdKeys.filter { it !in resumeVocab }
        return Result(
            score = (matched.size * 100 / jdKeys.size).coerceIn(0, 100),
            matched = matched,
            missing = missing
        )
    }
}
