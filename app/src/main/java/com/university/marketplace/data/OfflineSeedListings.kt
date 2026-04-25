package com.university.marketplace.data

import com.university.marketplace.domain.Listing

internal object OfflineSeedListings {
    val items: List<Listing> = listOf(
        Listing(
            id = "offline-1",
            sellerId = "seller-1",
            categoryId = "Books",
            title = "Calculus Textbook",
            description = "Libro de calculo en buen estado para cursos iniciales.",
            price = 45.0,
            condition = "used",
            images = listOf("https://picsum.photos/seed/book/800/600"),
            status = "published",
            latitude = 4.601,
            longitude = -74.065
        ),
        Listing(
            id = "offline-2",
            sellerId = "seller-2",
            categoryId = "Electronics",
            title = "Laptop Stand",
            description = "Soporte plegable para laptop, liviano y facil de llevar.",
            price = 18.0,
            condition = "new",
            images = listOf("https://picsum.photos/seed/stand/800/600"),
            status = "published",
            latitude = 4.602,
            longitude = -74.066
        ),
        Listing(
            id = "offline-3",
            sellerId = "seller-3",
            categoryId = "Study",
            title = "Scientific Calculator",
            description = "Calculadora cientifica con funciones para algebra y estadistica.",
            price = 32.0,
            condition = "used",
            images = listOf("https://picsum.photos/seed/calculator/800/600"),
            status = "published",
            latitude = 4.603,
            longitude = -74.064
        ),
        Listing(
            id = "offline-4",
            sellerId = "seller-4",
            categoryId = "Furniture",
            title = "Study Chair",
            description = "Silla comoda para estudiar por largas jornadas.",
            price = 60.0,
            condition = "used",
            images = listOf("https://picsum.photos/seed/chair/800/600"),
            status = "published",
            latitude = 4.600,
            longitude = -74.067
        )
    )
}

