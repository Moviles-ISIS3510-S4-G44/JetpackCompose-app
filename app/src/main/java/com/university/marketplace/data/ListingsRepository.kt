package com.university.marketplace.data

import com.university.marketplace.data.api.CreateListingDto
import com.university.marketplace.data.api.ListingsApi
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.api.SearchIntent
import com.university.marketplace.data.api.GroqApi
import com.university.marketplace.data.api.GroqRequest
import com.university.marketplace.data.api.GroqMessage
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
import com.university.marketplace.BuildConfig
import android.util.LruCache
import kotlin.math.min
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ListingsRepository(
    private val api: ListingsApi = NetworkModule.listingsApi,
    private val groqApi: GroqApi = NetworkModule.groqApi,
    private val dao: ListingDao,
    private val semanticSearchEngine: SemanticSearchEngine
) : ListingRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val listAdapter = moshi.adapter<List<String>>(listType)

    private val embeddingCache: LruCache<String, FloatArray> by lazy {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = min(maxMemoryKb / 8, 8 * 1024)
        object : LruCache<String, FloatArray>(cacheSizeKb) {
            override fun sizeOf(key: String, value: FloatArray): Int {
                return (value.size * 4) / 1024
            }
        }
    }

    override fun getActiveListings(): Flow<List<Listing>> {
        return dao.getActiveListings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshListings() {
        withContext(Dispatchers.IO) {
            try {
                val remoteListings = api.getListings()
                val entities = remoteListings.map { dto ->
                    val domain = dto.toDomain()
                    val cached = embeddingCache.get(domain.id)
                    val embedding = if (cached != null) {
                        cached
                    } else {
                        val emb = withContext(Dispatchers.Default) {
                            semanticSearchEngine.getEmbedding("${domain.title} ${domain.description}")
                        }
                        embeddingCache.put(domain.id, emb)
                        emb
                    }
                    domain.toEntity(embedding)
                }
                dao.insertListings(entities)
                dao.deleteStaleListings(System.currentTimeMillis() - 10 * 60 * 1000)
                dao.deleteStaleSearchCache(System.currentTimeMillis() - 10 * 60 * 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getListingById(id: String): Listing {
        return dao.getListingById(id)?.toDomain() ?: api.getListingById(id).toDomain()
    }

    override suspend fun searchListings(query: String): Flow<List<Listing>> {
        TODO("Not yet implemented")
    }

    override fun searchListingsFlow(query: String): Flow<List<Listing>> = flow {
        val normalizedQuery = SearchTextNormalizer.normalize(query)
        val initialQueryTokens = SearchTextNormalizer.tokenize(normalizedQuery)

        if (normalizedQuery.isBlank()) {
            emit(dao.getActiveListingsList().map { it.toDomain() })
            return@flow
        }

        val cachedSearch = dao.getSearchCache(normalizedQuery)
        if (cachedSearch != null && (System.currentTimeMillis() - cachedSearch.timestamp) < 10 * 60 * 1000) {
            val resultIds = listAdapter.fromJson(cachedSearch.resultIdsJson) ?: emptyList()
            val listings = resultIds.mapNotNull { id -> dao.getListingById(id)?.toDomain() }
            if (listings.isNotEmpty()) {
                emit(listings)
            }
        }

        val cachedEntities = dao.getActiveListingsList()
        cachedEntities.forEach { entity ->
            entity.embedding?.let { embeddingBytes ->
                if (embeddingCache.get(entity.id) == null) {
                    byteArrayToFloatArray(embeddingBytes)?.let { floatArray ->
                        embeddingCache.put(entity.id, floatArray)
                    }
                }
            }
        }
        if (cachedSearch == null) {
            emit(cachedEntities.map { it.toDomain() })
        }

        val vocabulary = buildVocabulary(cachedEntities.map { it.toDomain() })
        val correctedTokens = initialQueryTokens.map { token -> autocorrectToken(token, vocabulary) }
        val expandedQueryTokens = SearchQueryExpander.expandTokens(correctedTokens)
        val semanticQuery = expandedQueryTokens.joinToString(" ")

        coroutineScope {
            val intentDeferred = async { parseSearchIntent(query) }
            val queryCacheKey = "q:$semanticQuery"
            val cachedQueryEmbedding = embeddingCache.get(queryCacheKey)
            val queryEmbeddingDeferred = if (cachedQueryEmbedding != null) {
                async { cachedQueryEmbedding }
            } else {
                async(Dispatchers.Default) {
                    val emb = semanticSearchEngine.getEmbedding(semanticQuery)
                    embeddingCache.put(queryCacheKey, emb)
                    emb
                }
            }

            val intent = intentDeferred.await()
            val queryEmbedding = queryEmbeddingDeferred.await()
            val maxPriceFromIntent = intent?.max_price ?: extractMaxPriceFromQuery(query)

            val rankedListings = cachedEntities.map { entity ->
                val listing = entity.toDomain()
                val semanticScore = if (entity.embedding != null) {
                    byteArrayToFloatArray(entity.embedding)?.let {
                        semanticSearchEngine.calculateCosineSimilarity(queryEmbedding, it)
                    } ?: 0f
                } else {
                    0f
                }
                val lexicalScore = textSimilarityScore(listing, expandedQueryTokens)
                val phraseBoost = if (
                    SearchTextNormalizer.normalize("${listing.title} ${listing.description}").contains(normalizedQuery)
                ) {
                    0.2f
                } else {
                    0f
                }
                val finalScore = (semanticScore * 0.6f) + (lexicalScore * 0.35f) + (phraseBoost * 0.05f)
                listing to finalScore
            }.filter { (listing, finalScore) ->
                var matches = finalScore >= 0.16f
                intent?.let {
                    if (it.category != null && !matchesIntentCategory(listing, it.category)) matches = false
                    if (it.condition != null && !listing.condition.equals(it.condition, ignoreCase = true)) matches = false
                }
                if (maxPriceFromIntent != null && listing.price > maxPriceFromIntent) matches = false
                matches
            }.sortedByDescending { pair ->
                when (intent?.sort_order) {
                    "price_low" -> -pair.first.price.toFloat()
                    "price_high" -> pair.first.price.toFloat()
                    else -> pair.second
                }
            }.map { it.first }

            val resultIdsJson = listAdapter.toJson(rankedListings.map { it.id })
            val intentJson = moshi.adapter(SearchIntent::class.java).toJson(intent)
            dao.insertSearchCache(SearchCacheEntity(normalizedQuery, resultIdsJson, intentJson))

            emit(rankedListings)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchListingsFiltered(
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

    private suspend fun parseSearchIntent(query: String): SearchIntent? {
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
                    request = GroqRequest(messages = listOf(GroqMessage(role = "user", content = prompt)))
                )

                val content = response.choices.firstOrNull()?.message?.content ?: return@withContext null
                val json = content.substringAfter("```json").substringBefore("```").trim()
                val finalJson = if (json.startsWith("{")) json else content
                moshi.adapter(SearchIntent::class.java).fromJson(finalJson)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

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

    private fun matchesIntentCategory(listing: Listing, categoryIntent: String): Boolean {
        val normalizedIntent = SearchTextNormalizer.normalize(categoryIntent)
        if (normalizedIntent.isBlank()) return true

        val intentTokens = SearchQueryExpander.expandTokens(
            SearchTextNormalizer.tokenize(normalizedIntent)
        )

        val listingText = SearchTextNormalizer.normalize(
            "${listing.categoryId} ${listing.title} ${listing.description}"
        )

        val semanticCategoryGroups = mapOf(
            "electronica" to setOf("electronica", "electronic", "laptop", "pc", "monitor", "teclado", "mouse", "camara", "celular", "smartphone", "tablet"),
            "libros" to setOf("libro", "book", "novela", "texto", "manual", "kindle"),
            "muebles" to setOf("mueble", "escritorio", "mesa", "silla", "sofa", "lampara"),
            "accesorios" to setOf("accesorio", "mochila", "cable", "usb", "audifono", "auricular", "airpods")
        )

        val categoryGroupMatch = semanticCategoryGroups.any { (group, keywords) ->
            val intentMatchesGroup = intentTokens.any { it == group || keywords.contains(it) }
            intentMatchesGroup && keywords.any { keyword -> listingText.contains(keyword) }
        }

        return categoryGroupMatch || intentTokens.any { token -> listingText.contains(token) }
    }

    private fun extractMaxPriceFromQuery(query: String): Double? {
        val normalized = SearchTextNormalizer.normalize(query)
        val regex = "(\\d+[\\d.]*)\\s*(k|mil|m|millon|millones)?".toRegex()
        val match = regex.find(normalized) ?: return null
        val numberRaw = match.groupValues[1].replace(".", "")
        val base = numberRaw.toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues.getOrNull(2).orEmpty()) {
            "k", "mil" -> 1_000.0
            "m", "millon", "millones" -> 1_000_000.0
            else -> 1.0
        }
        val value = base * multiplier
        return if (value > 0.0) value else null
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
                costs[j] = minOf(costs[j] + 1, costs[j - 1] + 1, previous + substitutionCost)
                previous = current
            }
        }
        return costs[b.length]
    }

    private fun byteArrayToFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.getFloat(i * 4)
        }
        return floats
    }
}
