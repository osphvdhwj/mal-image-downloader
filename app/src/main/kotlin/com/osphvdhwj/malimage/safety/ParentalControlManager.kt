/**
 * ParentalControlManager.kt
 *
 * Simple parental controls removed; now only age verification metadata is embedded.
 * No PIN protection or filtering.
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.safety

import com.osphvdhwj.malimage.parser.AnimeEntry

class ParentalControlManager {

    companion object {
        // Content rating levels for metadata tagging
        enum class ContentRating(val level: Int, val description: String) {
            PG(1, "General Audiences"),
            PG_13(2, "Teens 13+"),
            R(3, "Mature 17+"),
            X(4, "Adults Only 18+"),
            XXX(5, "Explicit Adult Content")
        }
    }

    /**
     * Always considers content as allowed; no filtering.
     */
    fun isContentAllowed(entry: AnimeEntry): Boolean = true

    /**
     * Return content rating for metadata
     */
    fun getContentRating(entry: AnimeEntry): ContentRating {
        val genres = entry.genres?.lowercase() ?: ""

        return when {
            genres.contains("xxx") || genres.contains("explicit") -> ContentRating.XXX
            genres.contains("hentai") || genres.contains("adult") -> ContentRating.X
            genres.contains("ecchi") || genres.contains("mature") -> ContentRating.R
            genres.contains("teen") || genres.contains("violence") -> ContentRating.PG_13
            else -> ContentRating.PG
        }
    }
}
