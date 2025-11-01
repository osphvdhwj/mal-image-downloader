/**
 * MALXmlParser.kt
 *
 * This class handles parsing of MyAnimeList (MAL) XML export files.
 * It extracts key anime/manga information including IDs, titles, genres, images, and tags.
 *
 * Features:
 * - XML parsing with error handling
 * - Data class models for MAL entries
 * - Supports both Anime and Manga MAL exports
 *
 * Usage:
 * 1. Initialize MALXmlParser.
 * 2. Call parseXmlFile() with the local MAL XML file.
 * 3. Receives a list of AnimeEntry objects.
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.parser

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import java.io.File

/**
 * Data class representing a single MAL anime or manga entry.
 */
@Root(name = "anime", strict = false)
data class AnimeEntry(
    @field:Element(name = "series_animedb_id", required = false)
    var id: Int? = null,

    @field:Element(name = "series_title", required = false)
    var title: String? = null,

    @field:Element(name = "series_image", required = false)
    var imageUrl: String? = null,

    @field:ElementList(entry = "my_tags", inline = true, required = false)
    var tags: List<String>? = null,

    @field:Element(name = "series_type", required = false)
    var type: Int? = null,

    @field:Element(name = "series_genres", required = false)
    var genres: String? = null
)

/**
 * Root element for MAL XML file.
 */
@Root(name = "myanimelist", strict = false)
data class MALExport(
    @field:ElementList(name = "anime", inline = true, required = false)
    var animeList: List<AnimeEntry>? = null,

    @field:ElementList(name = "manga", inline = true, required = false)
    var mangaList: List<AnimeEntry>? = null
)

/**
 * Parser class for MAL XML files.
 */
class MALXmlParser {

    /**
     * Parse the MAL XML file and extract anime and manga entries.
     *
     * @param xmlFile Local MAL XML file
     * @return MALExport object containing lists of anime and manga entries
     */
    fun parseXmlFile(xmlFile: File): MALExport? {
        return try {
            val serializer = Persister()
            serializer.read(MALExport::class.java, xmlFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
