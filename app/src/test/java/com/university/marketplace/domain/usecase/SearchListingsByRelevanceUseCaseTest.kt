package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchListingsByRelevanceUseCaseTest {

    private val useCase = SearchListingsByRelevanceUseCase()

    @Test
    fun `returns fuzzy match for small typo`() {
        val listings = listOf(
            listing(id = "1", title = "Calculus Textbook"),
            listing(id = "2", title = "Desk Lamp")
        )

        val result = useCase.execute(listings, "calclus")

        assertTrue(result.isNotEmpty())
        assertEquals("1", result.first().id)
    }

    @Test
    fun `ranks exact matches before fuzzy matches`() {
        val listings = listOf(
            listing(id = "1", title = "Calculus Advanced Notes"),
            listing(id = "2", title = "Calclus Workbook")
        )

        val result = useCase.execute(listings, "calculus")

        assertEquals("1", result.first().id)
    }

    @Test
    fun `returns original list when query is blank`() {
        val listings = listOf(
            listing(id = "1", title = "A"),
            listing(id = "2", title = "B")
        )

        val result = useCase.execute(listings, "   ")

        assertEquals(listings, result)
    }

    @Test
    fun `returns empty list when there are no relevant matches`() {
        val listings = listOf(
            listing(id = "1", title = "Chair"),
            listing(id = "2", title = "Table")
        )

        val result = useCase.execute(listings, "xqzpt")

        assertTrue(result.isEmpty())
    }

    private fun listing(id: String, title: String): Listing {
        return Listing(
            id = id,
            sellerId = "seller",
            categoryId = "cat",
            title = title,
            description = "description",
            price = 10.0,
            condition = "used",
            images = emptyList(),
            status = "active",
            latitude = null,
            longitude = null
        )
    }
}

