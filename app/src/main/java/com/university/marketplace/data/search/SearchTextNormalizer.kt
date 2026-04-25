package com.university.marketplace.data.search

import java.text.Normalizer

object SearchTextNormalizer {
    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun tokenize(input: String): List<String> {
        val normalized = normalize(input)
        if (normalized.isBlank()) return emptyList()
        return normalized.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}

