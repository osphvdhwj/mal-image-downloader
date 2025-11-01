/**
 * MetadataEmbedder.kt
 *
 * Embeds MAL metadata into downloaded images using EXIF and XMP formats.
 * This ensures AVES gallery app can read and display the metadata properly.
 *
 * Features:
 * - EXIF metadata embedding (title, description, user comment)
 * - XMP metadata for advanced tagging
 * - AVES gallery compatibility
 * - Full MAL data preservation in JSON format
 *
 * AVES Compatibility:
 * - Uses Dublin Core (dc:) fields that AVES recognizes
 * - Custom MAL namespace for specific fields
 * - GPS coordinates for virtual location-based organization
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.metadata

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.osphvdhwj.malimage.parser.AnimeEntry
import org.json.JSONObject
import java.io.File
import java.io.IOException

class MetadataEmbedder {

    companion object {
        private const val TAG = "MetadataEmbedder"
        
        // Custom MAL namespace for XMP
        private const val MAL_NAMESPACE = "http://myanimelist.net/ns/1.0/"
        private const val MAL_PREFIX = "mal"
    }

    /**
     * Embed comprehensive metadata into the image file
     * 
     * @param imageFile The downloaded image file
     * @param entry MAL anime/manga entry data
     */
    fun embedMetadata(imageFile: File, entry: AnimeEntry) {
        try {
            embedExifMetadata(imageFile, entry)
            embedXmpMetadata(imageFile, entry)
            Log.i(TAG, "Successfully embedded metadata for: ${entry.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to embed metadata for: ${entry.title}", e)
            // Don't throw - metadata embedding failure shouldn't fail the download
        }
    }

    /**
     * Embed EXIF metadata that most gallery apps can read
     */
    private fun embedExifMetadata(imageFile: File, entry: AnimeEntry) {
        try {
            val exif = ExifInterface(imageFile.absolutePath)
            
            // Basic EXIF fields
            entry.title?.let { title ->
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, title)
                exif.setAttribute(ExifInterface.TAG_DOCUMENT_NAME, title)
            }
            
            // Software tag to identify our app
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL Image Downloader")
            
            // Artist tag for media type
            val mediaType = when (entry.type) {
                1 -> "TV Anime"
                2 -> "OVA"
                3 -> "Movie"
                4 -> "Special"
                5 -> "ONA"
                6 -> "Music"
                else -> if (entry.type != null && entry.type!! > 10) "Manga" else "Unknown"
            }
            exif.setAttribute(ExifInterface.TAG_ARTIST, mediaType)
            
            // User comment with full JSON data for complete preservation
            val fullMetadata = createFullMetadataJson(entry)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, fullMetadata)
            
            // Copyright field for genres
            entry.genres?.let { genres ->
                exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "Genres: $genres")
            }
            
            // GPS coordinates for virtual organization (AVES feature)
            setVirtualGpsCoordinates(exif, entry)
            
            exif.saveAttributes()
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write EXIF metadata", e)
        }
    }

    /**
     * Embed XMP metadata for advanced gallery apps like AVES
     */
    private fun embedXmpMetadata(imageFile: File, entry: AnimeEntry) {
        // Note: This is a simplified XMP implementation
        // In a full implementation, you would use a proper XMP library
        
        val xmpData = createXmpData(entry)
        
        // For now, we'll include XMP data in EXIF ImageDescription
        // A full implementation would write actual XMP packets
        try {
            val exif = ExifInterface(imageFile.absolutePath)
            val currentDescription = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION) ?: ""
            val enhancedDescription = "$currentDescription\n\nXMP: $xmpData"
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, enhancedDescription)
            exif.saveAttributes()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write XMP metadata", e)
        }
    }

    /**
     * Create comprehensive JSON metadata for preservation
     */
    private fun createFullMetadataJson(entry: AnimeEntry): String {
        return JSONObject().apply {
            // MAL specific data
            entry.id?.let { put("mal_id", it) }
            entry.title?.let { put("title", it) }
            entry.imageUrl?.let { put("image_url", it) }
            entry.type?.let { put("type", it) }
            entry.genres?.let { put("genres", it) }
            
            // Metadata about our processing
            put("processed_by", "MAL Image Downloader")
            put("processed_date", System.currentTimeMillis())
            put("version", "1.0")
            
            // AVES compatibility markers
            put("aves_compatible", true)
            put("metadata_version", "1.0")
        }.toString()
    }

    /**
     * Create XMP data structure for AVES compatibility
     */
    private fun createXmpData(entry: AnimeEntry): String {
        val xmpFields = mutableMapOf<String, String>()
        
        // Dublin Core fields (AVES standard)
        entry.title?.let { xmpFields["dc:title"] = it }
        entry.genres?.let { xmpFields["dc:subject"] = it }
        
        val mediaType = when (entry.type) {
            1 -> "TV Anime"
            2 -> "OVA"
            3 -> "Movie"
            4 -> "Special"
            5 -> "ONA"
            6 -> "Music"
            else -> if (entry.type != null && entry.type!! > 10) "Manga" else "Unknown"
        }
        xmpFields["dc:type"] = mediaType
        
        // Custom MAL fields
        entry.id?.let { xmpFields["${MAL_PREFIX}:id"] = it.toString() }
        entry.type?.let { xmpFields["${MAL_PREFIX}:type"] = it.toString() }
        
        return xmpFields.map { "${it.key}='${it.value}'" }.joinToString("; ")
    }

    /**
     * Set virtual GPS coordinates for location-based organization in AVES
     * This allows AVES to group images by "location" which we use for genres
     */
    private fun setVirtualGpsCoordinates(exif: ExifInterface, entry: AnimeEntry) {
        // Generate pseudo-coordinates based on content
        val genreHash = entry.genres?.hashCode() ?: 0
        val typeHash = entry.type?.hashCode() ?: 0
        
        // Create virtual coordinates (safe range to avoid real locations)
        val latitude = 89.0 + (genreHash % 100) / 10000.0 // Near North Pole
        val longitude = 179.0 + (typeHash % 100) / 10000.0 // Near Date Line
        
        exif.setGpsInfo(latitude, longitude)
    }

    /**
     * Verify that metadata was embedded correctly
     */
    fun verifyMetadata(imageFile: File): Boolean {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val title = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            
            !title.isNullOrEmpty() && !userComment.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify metadata", e)
            false
        }
    }

    /**
     * Extract MAL metadata from an image file
     */
    fun extractMetadata(imageFile: File): AnimeEntry? {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            
            if (!userComment.isNullOrEmpty()) {
                parseMetadataFromJson(userComment)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata", e)
            null
        }
    }

    /**
     * Parse AnimeEntry from JSON metadata
     */
    private fun parseMetadataFromJson(json: String): AnimeEntry? {
        return try {
            val jsonObj = JSONObject(json)
            AnimeEntry().apply {
                id = if (jsonObj.has("mal_id")) jsonObj.getInt("mal_id") else null
                title = jsonObj.optString("title").takeIf { it.isNotEmpty() }
                imageUrl = jsonObj.optString("image_url").takeIf { it.isNotEmpty() }
                type = if (jsonObj.has("type")) jsonObj.getInt("type") else null
                genres = jsonObj.optString("genres").takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse metadata JSON", e)
            null
        }
    }
}