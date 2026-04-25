package com.university.marketplace.data.search

import android.content.Context
import kotlin.math.sqrt

class SemanticSearchEngine(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val dimensions = 384

    fun getEmbedding(text: String): FloatArray {
        val normalizedText = SearchTextNormalizer.normalize(text)
        val baseTokens = SearchTextNormalizer.tokenize(normalizedText)
        val expandedTokens = SearchQueryExpander.expandTokens(baseTokens)
        if (expandedTokens.isEmpty()) return FloatArray(dimensions)

        val vector = FloatArray(dimensions)

        expandedTokens.forEach { token ->
            addTokenContribution(vector, token)
            addTrigramContribution(vector, token)
        }

        return l2Normalize(vector)
    }

    fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        val result = dotProduct / (sqrt(normA) * sqrt(normB))
        return if (result.isNaN()) 0f else result
    }

    private fun addTokenContribution(vector: FloatArray, token: String) {
        if (token.isBlank()) return
        val index = hashToIndex(token, vector.size)
        val tokenWeight = 1.0f + (token.length.coerceAtMost(12) * 0.03f)
        vector[index] += tokenWeight
    }

    private fun addTrigramContribution(vector: FloatArray, token: String) {
        val n = 3
        if (token.length < n) return
        for (i in 0..token.length - n) {
            val ngram = token.substring(i, i + n)
            val index = hashToIndex("ng_$ngram", vector.size)
            vector[index] += 0.35f
        }
    }

    private fun hashToIndex(value: String, size: Int): Int {
        val hash = value.hashCode()
        return (hash and Int.MAX_VALUE) % size
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (element in vector) {
            norm += element * element
        }
        norm = sqrt(norm)
        if (norm == 0f) return vector
        for (i in vector.indices) {
            vector[i] /= norm
        }
        return vector
    }
}
