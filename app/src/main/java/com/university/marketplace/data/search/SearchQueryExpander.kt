package com.university.marketplace.data.search

object SearchQueryExpander {
    private val synonyms: Map<String, Set<String>> = mapOf(
        "computador" to setOf("pc", "laptop", "portatil", "notebook"),
        "portatil" to setOf("laptop", "notebook", "computador"),
        "audifonos" to setOf("auriculares", "headphones", "airpods"),
        "camara" to setOf("fotografia", "dslr", "canon", "sony"),
        "teclado" to setOf("keyboard", "keychron", "mecanico"),
        "libro" to setOf("book", "texto", "novela"),
        "mueble" to setOf("escritorio", "silla", "mesa"),
        "celular" to setOf("movil", "telefono", "smartphone"),
        "nuevo" to setOf("new", "sin uso"),
        "usado" to setOf("used", "segunda mano", "preowned")
    )

    fun expandTokens(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return emptyList()
        val expanded = linkedSetOf<String>()
        tokens.forEach { token ->
            expanded += token
            synonyms[token]?.let { expanded += it }
            synonyms.entries
                .firstOrNull { entry -> entry.value.contains(token) }
                ?.key
                ?.let { expanded += it }
        }
        return expanded.toList()
    }
}

