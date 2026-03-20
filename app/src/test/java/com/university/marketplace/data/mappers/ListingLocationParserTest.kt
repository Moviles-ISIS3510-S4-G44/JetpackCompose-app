package com.university.marketplace.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListingLocationParserTest {

    @Test
    fun `parse returns coordinates for valid string`() {
        val result = ListingLocationParser.parse(" 4.7110 , -74.0721 ")

        requireNotNull(result)
        assertEquals(4.7110, result.latitude, 0.0001)
        assertEquals(-74.0721, result.longitude, 0.0001)
    }

    @Test
    fun `parse returns coordinates for valid object`() {
        val result = ListingLocationParser.parse(
            mapOf(
                "latitude" to 4.7110,
                "longitude" to -74.0721
            )
        )

        requireNotNull(result)
        assertEquals(4.7110, result.latitude, 0.0001)
        assertEquals(-74.0721, result.longitude, 0.0001)
    }

    @Test
    fun `parse returns null for malformed string`() {
        val result = ListingLocationParser.parse("4.7110|-74.0721")

        assertNull(result)
    }

    @Test
    fun `parse returns null for empty or null location`() {
        val emptyResult = ListingLocationParser.parse("   ")
        val nullResult = ListingLocationParser.parse(null)

        assertNull(emptyResult)
        assertNull(nullResult)
    }

    @Test
    fun `parse returns null for out of range coordinates`() {
        val result = ListingLocationParser.parse("95.0,-74.0721")

        assertNull(result)
    }
}

