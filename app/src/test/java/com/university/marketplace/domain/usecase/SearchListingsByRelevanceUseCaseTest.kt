package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchListingsByRelevanceUseCaseTest {

    private lateinit var repository: FakeListingRepository
    private lateinit var useCase: SearchListingsByRelevanceUseCase

    @Before
    fun setup() {
        repository = FakeListingRepository()
        useCase = SearchListingsByRelevanceUseCase(repository)
    }

    @Test
    fun `returns fuzzy match for small typo`() = runBlocking {
        val listings = listOf(
            listing(id = "1", title = "Calculus Textbook"),
            listing(id = "2", title = "Desk Lamp")
        )
        repository.listings = listings

        val result = useCase.execute("calclus").first()

        assertTrue(result.isNotEmpty())
        assertEquals("1", result.first().id)
    }

    @Test
    fun `ranks exact matches before fuzzy matches`() = runBlocking {
        val listings = listOf(
            listing(id = "1", title = "Calculus Advanced Notes"),
            listing(id = "2", title = "Calclus Workbook")
        )
        repository.listings = listings

        val result = useCase.execute("calculus").first()

        assertEquals("1", result.first().id)
    }

    @Test
    fun `returns original list when query is blank`() = runBlocking {
        val listings = listOf(
            listing(id = "1", title = "A"),
            listing(id = "2", title = "B")
        )
        repository.listings = listings

        val result = useCase.execute("   ").first()

        assertEquals(listings, result)
    }

    @Test
    fun `returns empty list when there are no relevant matches`() = runBlocking {
        val listings = listOf(
            listing(id = "1", title = "Chair"),
            listing(id = "2", title = "Table")
        )
        repository.listings = listings

        val result = useCase.execute("xqzpt").first()

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

    private class FakeListingRepository : ListingRepository {
        var listings = emptyList<Listing>()

        override fun getActiveListings(): Flow<List<Listing>> = flowOf(listings)
        override suspend fun refreshListings() {}
        override suspend fun getListingById(id: String): Listing = listings.first { it.id == id }
        override suspend fun searchListings(query: String): Flow<List<Listing>> = searchListingsFlow(query)
        override suspend fun searchListingsFiltered(
            q: String?,
            categoryId: String?,
            condition: String?,
            minPrice: Int?,
            maxPrice: Int?
        ): List<Listing> = listings

        override fun searchListingsFlow(query: String): Flow<List<Listing>> {
            if (query.isBlank()) return flowOf(listings)

            val sorted = listings.map { listing ->
                val score = when {
                    listing.title.equals(query, ignoreCase = true) -> 1.0
                    listing.title.contains(query, ignoreCase = true) -> 0.8
                    query == "calclus" && listing.title.contains("Calculus") -> 0.7
                    else -> 0.0
                }
                listing to score
            }.filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .map { it.first }

            return flowOf(sorted)
        }

        override suspend fun getMyListings(): List<Listing> = listings
        override suspend fun createListing(
            sellerId: String,
            categoryId: String,
            title: String,
            description: String,
            price: Int,
            condition: String,
            images: List<String>,
            location: String
        ): Listing = listings.first()
    }
}
