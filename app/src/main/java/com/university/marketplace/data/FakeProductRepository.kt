package com.university.marketplace.data

import com.university.marketplace.domain.Product

class FakeProductRepository : ProductRepository {
    private val products = listOf(
        Product(
            id = "1",
            name = "Calculus Textbook 11th Ed",
            description = "Libro de calculo en buen estado, ideal para Algebra y Calculo I.",
            price = 45.0,
            imageUrl = "https://m.media-amazon.com/images/I/61UY0qFfUWL._AC_UF1000,1000_QL80_.jpg",
            latitude = 4.601,
            longitude = -74.065,
            rating = 4.8,
            isFeatured = true,
            category = "Books",
            locationLabel = "Bloque ML - Biblioteca"
        ),
        Product(
            id = "2",
            name = "MacBook Pro 13-inch M2",
            description = "Laptop para desarrollo y diseno, bateria saludable y cargador original.",
            price = 850.0,
            imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.602,
            longitude = -74.066,
            rating = 5.0,
            isFeatured = true,
            category = "Electronics",
            locationLabel = "Bloque B - Primer Piso"
        ),
        Product(
            id = "3",
            name = "Study Lamp",
            description = "Lampara LED para escritorio con intensidad ajustable y puerto USB.",
            price = 15.0,
            imageUrl = "https://exitocol.vtexassets.com/arquivos/ids/31302025/lampara-de-escritorio-con-base-pequena-con-pinza-negra.jpg?v=638961816385470000",
            latitude = 4.603,
            longitude = -74.067,
            rating = 4.2,
            isFeatured = false,
            category = "Furniture",
            locationLabel = "City U - Torre 2"
        ),
        Product(
            id = "4",
            name = "Scientific Calculator",
            description = "Calculadora cientifica con funciones estadisticas y modo examen.",
            price = 30.0,
            imageUrl = "https://panamericana.vtexassets.com/arquivos/ids/443468/calculadora-cientifica-casio-fx-82laplus-bk-negra-4971850089926.jpg?v=637910931168430000",
            latitude = 4.604,
            longitude = -74.068,
            rating = 4.7,
            isFeatured = false,
            category = "Electronics",
            locationLabel = "Ingenieria - Laboratorio 1"
        ),
        Product(
            id = "5",
            name = "Ergonomic Chair",
            description = "Silla ergonomica con soporte lumbar, perfecta para largas jornadas de estudio.",
            price = 120.0,
            imageUrl = "https://media.falabella.com/falabellaCO/118562847_02/w=1500,h=1500,fit=cover",
            latitude = 4.605,
            longitude = -74.069,
            rating = 4.9,
            isFeatured = false,
            category = "Furniture",
            locationLabel = "Edificio C - Entrada principal"
        )
    )

    override fun getProducts(): List<Product> = products
    
    override fun getFeaturedProducts(): List<Product> = products.filter { it.isFeatured }
    
    override fun getRecentProducts(): List<Product> = products.filter { !it.isFeatured }

    override fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return products

        val normalizedQuery = query.normalizeForSearch()
        val terms = normalizedQuery.split(" ").filter { it.isNotBlank() }

        return products
            .mapNotNull { product ->
                val score = product.calculateRelevance(normalizedQuery, terms)
                if (score > 0.0) product to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Product, Double>> { it.second }
                    .thenByDescending { it.first.rating }
                    .thenBy { it.first.price }
            )
            .map { it.first }
    }

    override fun getProductById(productId: String): Product? = products.find { it.id == productId }

    private fun Product.calculateRelevance(normalizedQuery: String, terms: List<String>): Double {
        val normalizedName = name.normalizeForSearch()
        val normalizedDescription = description.normalizeForSearch()

        var score = 0.0

        if (normalizedName == normalizedQuery) score += 8.0
        if (normalizedName.startsWith(normalizedQuery)) score += 5.0
        if (normalizedName.contains(normalizedQuery)) score += 3.0
        if (normalizedDescription.contains(normalizedQuery)) score += 2.0

        terms.forEach { term ->
            if (normalizedName.contains(term)) score += 1.5
            if (normalizedDescription.contains(term)) score += 1.0
        }

        return score
    }

    private fun String.normalizeForSearch(): String {
        return lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
