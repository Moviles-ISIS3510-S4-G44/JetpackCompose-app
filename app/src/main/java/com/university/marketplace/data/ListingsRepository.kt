package com.university.marketplace.data

import com.university.marketplace.data.api.CreateListingDto
import com.university.marketplace.data.api.ListingsApi
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.api.SearchIntent
import com.university.marketplace.data.local.ListingDao
import com.university.marketplace.data.local.SearchCacheEntity
import com.university.marketplace.data.local.toDomain
import com.university.marketplace.data.local.toEntity
import com.university.marketplace.data.mappers.toDomain
import com.university.marketplace.data.search.SearchQueryExpander
import com.university.marketplace.data.search.SearchTextNormalizer
import com.university.marketplace.data.search.SemanticSearchEngine
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ListingsRepository(
    private val api: ListingsApi = NetworkModule.listingsApi,
    private val groqApi: GroqApi = NetworkModule.groqApi,
    private val dao: ListingDao,
    private val semanticSearchEngine: SemanticSearchEngine
) : ListingRepository {
    override suspend fun getActiveListings(): List<Listing> {
        return api.getListings(status = "published").map { it.toDomain() }
    }

    override suspend fun getListingById(id: String): Listing {
        return dao.getListingById(id)?.toDomain() ?: api.getListingById(id).toDomain()
    }

    override suspend fun parseSearchIntent(query: String): SearchIntent? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Extract search filters from the following query in JSON format. Use these fields:
                    - product_name (string)
                    - max_price (number)
                    - category (string)
                    - condition (string: "new" or "used")
                    - proximity_preference (number in km)
                    - sort_order (string: "relevance", "price_low", "price_high")
                    
                    Return ONLY the JSON object.
                    Query: "$query"
                """.trimIndent()

                val response = groqApi.completeChat(
                    apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}",
                    request = GroqRequest(
                        messages = listOf(GroqMessage(role = "user", content = prompt))
                    )
                )
                
                val content = response.choices.firstOrNull()?.message?.content ?: return@withContext null
                // Clean markdown if present
                val json = content.substringAfter("```json").substringBefore("```").trim()
                val finalJson = if (json.startsWith("{")) json else content
                
                moshi.adapter(SearchIntent::class.java).fromJson(finalJson)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun searchListings(query: String): Flow<List<Listing>> = flow {
        val normalizedQuery = SearchTextNormalizer.normalize(query)
        val initialQueryTokens = SearchTextNormalizer.tokenize(normalizedQuery)

        if (normalizedQuery.isBlank()) {
            emit(dao.getActiveListingsList().map { it.toDomain() })
            return@flow
        }

        // 1. Check Query Cache
        val cachedSearch = dao.getSearchCache(normalizedQuery)
        if (cachedSearch != null && (System.currentTimeMillis() - cachedSearch.timestamp) < 10 * 60 * 1000) {
            val resultIds = listAdapter.fromJson(cachedSearch.resultIdsJson) ?: emptyList()
            val listings = resultIds.mapNotNull { id -> dao.getListingById(id)?.toDomain() }
            if (listings.isNotEmpty()) {
                emit(listings)
                // Still fall through to refresh the ranking if needed, or return here for strict cache
                // return@flow
            }
        }

        // 2. Immediate emit from local listings if no query cache or to provide baseline
        val cachedEntities = dao.getActiveListingsList()
        if (cachedSearch == null) emit(cachedEntities.map { it.toDomain() })

        val vocabulary = buildVocabulary(cachedEntities.map { it.toDomain() })
        val correctedTokens = initialQueryTokens.map { token -> autocorrectToken(token, vocabulary) }
        val expandedQueryTokens = SearchQueryExpander.expandTokens(correctedTokens)
        val semanticQuery = expandedQueryTokens.joinToString(" ")

        // 3. Parallel execution of Intent Parsing and Semantic Search
        coroutineScope {
            val intentDeferred = async { parseSearchIntent(query) }
            val queryEmbeddingDeferred = async { semanticSearchEngine.getEmbedding(semanticQuery) }

            val intent = intentDeferred.await()
            val queryEmbedding = queryEmbeddingDeferred.await()

            val rankedListings = cachedEntities.map { entity ->
                val listing = entity.toDomain()
                val semanticScore = if (entity.embedding != null) {
                    semanticSearchEngine.calculateCosineSimilarity(queryEmbedding, entity.embedding)
                } else {
                    0f
                }
                val lexicalScore = textSimilarityScore(listing, expandedQueryTokens)
                val phraseBoost = if (SearchTextNormalizer.normalize("${listing.title} ${listing.description}")
                    .contains(normalizedQuery)
                ) {
                    0.2f
                } else {
                    0f
                }
                val finalScore = (semanticScore * 0.6f) + (lexicalScore * 0.35f) + (phraseBoost * 0.05f)
                listing to finalScore
            }.filter { (listing, finalScore) ->
                var matches = finalScore >= 0.16f
                
                // Overlay Groq Intent Filters
                intent?.let {
                    if (it.max_price != null && listing.price > it.max_price) matches = false
                    if (it.category != null && !listing.categoryId.contains(it.category, ignoreCase = true)) matches = false
                    if (it.condition != null && !listing.condition.equals(it.condition, ignoreCase = true)) matches = false
                }
                matches
            }.sortedByDescending { pair ->
                when (intent?.sort_order) {
                    "price_low" -> -pair.first.price.toFloat()
                    "price_high" -> pair.first.price.toFloat()
                    else -> pair.second
                }
            }.map { it.first }

            // 4. Update Cache
            val resultIdsJson = listAdapter.toJson(rankedListings.map { it.id })
            val intentJson = moshi.adapter(SearchIntent::class.java).toJson(intent)
            dao.insertSearchCache(SearchCacheEntity(normalizedQuery, resultIdsJson, intentJson))

            emit(rankedListings)
        }
    }.flowOn(Dispatchers.IO)

    private fun textSimilarityScore(listing: Listing, expandedQueryTokens: List<String>): Float {
        if (expandedQueryTokens.isEmpty()) return 0f
        val listingText = SearchTextNormalizer.normalize(
            "${listing.title} ${listing.description} ${listing.condition} ${listing.categoryId}"
        )
        val matched = expandedQueryTokens.count { token -> listingText.contains(token) }
        return matched.toFloat() / expandedQueryTokens.size.toFloat()
    }

    private fun buildVocabulary(listings: List<Listing>): Set<String> {
        return listings
            .flatMap { listing ->
                SearchTextNormalizer.tokenize(
                    "${listing.title} ${listing.description} ${listing.condition} ${listing.categoryId}"
                )
            }
            .filter { it.length >= 3 }
            .toSet()
    }

    private fun autocorrectToken(token: String, vocabulary: Set<String>): String {
        if (token.length < 4 || vocabulary.contains(token)) return token
        var bestCandidate = token
        var bestDistance = Int.MAX_VALUE
        vocabulary.forEach { candidate ->
            if (kotlin.math.abs(candidate.length - token.length) > 2) return@forEach
            val distance = levenshteinDistance(token, candidate)
            if (distance < bestDistance) {
                bestDistance = distance
                bestCandidate = candidate
            }
        }
        return if (bestDistance <= 2) bestCandidate else token
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var previous = i - 1
            costs[0] = i
            for (j in 1..b.length) {
                val current = costs[j]
                val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
                costs[j] = minOf(
                    costs[j] + 1,
                    costs[j - 1] + 1,
                    previous + substitutionCost
                )
                previous = current
            }
        }
        return costs[b.length]
    }

    override suspend fun searchListings(
        q: String?,
        categoryId: String?,
        condition: String?,
        minPrice: Int?,
        maxPrice: Int?
    ): List<Listing> {
        return api.getListings(
            q = q,
            categoryId = categoryId,
            condition = condition,
            minPrice = minPrice,
            maxPrice = maxPrice,
            status = "published"
        ).map { it.toDomain() }
    }

    override suspend fun getMyListings(): List<Listing> {
        return api.getMyListings().map { it.toDomain() }
    }

    override suspend fun createListing(
        sellerId: String,
        categoryId: String,
        title: String,
        description: String,
        price: Int,
        condition: String,
        images: List<String>,
        location: String
    ): Listing {
        return api.createListing(
            CreateListingDto(
                sellerId = sellerId,
                categoryId = categoryId,
                title = title,
                description = description,
                price = price,
                condition = condition,
                images = images,
                location = location
            )
        ).toDomain()
    }
}
