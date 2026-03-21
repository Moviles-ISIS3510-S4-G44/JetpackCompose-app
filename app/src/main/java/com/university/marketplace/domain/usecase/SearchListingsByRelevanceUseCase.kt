package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import kotlin.math.min

class SearchListingsByRelevanceUseCase {

    fun execute(listings: List<Listing>, query: String): List<Listing> {
        val normalizedQuery = query.normalizedText()
        if (normalizedQuery.isEmpty()) return listings

        val terms = normalizedQuery.split(" ").filter { it.isNotBlank() }

        return listings
            .mapNotNull { listing ->
                val score = listing.relevanceScore(normalizedQuery, terms)
                if (score > 0) listing to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Listing, Int>> { it.second }
                    .thenBy { it.first.title.lowercase() }
            )
            .map { it.first }
    }

    private fun Listing.relevanceScore(normalizedQuery: String, terms: List<String>): Int {
        val titleText = title.normalizedText()
        val descriptionText = description.normalizedText()
        val titleTokens = titleText.tokens()
        val descriptionTokens = descriptionText.tokens()

        var score = 0

        if (titleText.contains(normalizedQuery)) score += 8
        if (descriptionText.contains(normalizedQuery)) score += 5

        terms.forEach { term ->
            if (titleText.startsWith(term)) score += 3
            if (titleText.contains(term)) score += 2
            if (descriptionText.contains(term)) score += 1

            score += bestFuzzyScore(term, titleTokens, exactBonus = 6, fuzzyBonus = 3)
            score += bestFuzzyScore(term, descriptionTokens, exactBonus = 3, fuzzyBonus = 1)
        }

        return score
    }

    private fun String.normalizedText(): String {
        return lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun String.tokens(): List<String> = split(" ").filter { it.isNotBlank() }

    private fun bestFuzzyScore(
        queryTerm: String,
        candidateTokens: List<String>,
        exactBonus: Int,
        fuzzyBonus: Int
    ): Int {
        if (queryTerm.isBlank() || candidateTokens.isEmpty()) return 0

        val bestDistance = candidateTokens.minOf { token -> levenshteinDistance(queryTerm, token) }
        if (bestDistance == 0) return exactBonus

        val maxDistance = allowedDistance(queryTerm.length)
        if (bestDistance > maxDistance) return 0

        return (fuzzyBonus * (maxDistance - bestDistance + 1)).coerceAtLeast(0)
    }

    private fun allowedDistance(length: Int): Int {
        return when {
            length <= 4 -> 1
            length <= 8 -> 2
            else -> 3
        }
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(
                    min(current[j - 1] + 1, previous[j] + 1),
                    previous[j - 1] + cost
                )
            }
            for (j in previous.indices) {
                previous[j] = current[j]
            }
        }

        return previous[b.length]
    }
}

