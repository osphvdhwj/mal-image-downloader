/**
 * FolderOrganizer.kt
 *
 * Organizes downloaded images into proper folder structure based on MAL data.
 * Follows the corrected structure: /MAL/ANIME/ and /MAL/MANGA/ with hentai as subfolders.
 *
 * Folder Structure:
 * - /MAL/ANIME/{Genre}/
 * - /MAL/ANIME/HENTAI/{Subcategory}/
 * - /MAL/MANGA/{Genre}/
 * - /MAL/MANGA/HENTAI/{Subcategory}/
 *
 * Features:
 * - Genre-based organization with hentai detection
 * - Privacy protection with .nomedia files for adult content
 * - Sanitized folder names for filesystem safety
 * - Automatic directory creation
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.storage

import android.os.Environment
import android.util.Log
import com.osphvdhwj.malimage.parser.AnimeEntry
import java.io.File

class FolderOrganizer {

    companion object {
        private const val TAG = "FolderOrganizer"
        private const val BASE_FOLDER = "MAL"
        private const val ANIME_FOLDER = "ANIME"
        private const val MANGA_FOLDER = "MANGA"
        private const val HENTAI_FOLDER = "HENTAI"
        private const val UNKNOWN_GENRE = "Unknown"
        
        // Hentai detection keywords
        private val HENTAI_KEYWORDS = setOf(
            "hentai", "ecchi", "adult", "18+", "nsfw", "explicit",
            "sexual", "erotic", "mature", "r+", "xxx"
        )
        
        // Hentai subcategory mappings
        private val HENTAI_SUBCATEGORIES = mapOf(
            "vanilla" to listOf("vanilla", "romantic", "sweet", "loving"),
            "ntr" to listOf("netorare", "ntr", "cheating", "cuckold", "stolen"),
            "incest" to listOf("incest", "sister", "brother", "family", "siblings"),
            "mother-son" to listOf("mother", "mom", "son", "oyakodon", "maternal"),
            "milf" to listOf("milf", "mature woman", "older woman", "cougar"),
            "bdsm" to listOf("bdsm", "bondage", "domination", "submission", "kinky"),
            "romance" to listOf("romance", "love", "tender", "gentle", "wholesome")
        )
    }

    private val baseDir = File(Environment.getExternalStorageDirectory(), BASE_FOLDER)

    /**
     * Organize content into appropriate folder structure
     * 
     * @param entry MAL anime/manga entry
     * @return Target directory for the image
     */
    fun organizeContent(entry: AnimeEntry): File {
        val mediaType = determineMediaType(entry)
        
        return if (isHentaiContent(entry)) {
            organizeHentaiContent(entry, mediaType)
        } else {
            organizeRegularContent(entry, mediaType)
        }
    }

    /**
     * Determine if content is anime or manga
     */
    private fun determineMediaType(entry: AnimeEntry): String {
        return when (entry.type) {
            1, 2, 3, 4, 5, 6 -> ANIME_FOLDER // TV, OVA, Movie, Special, ONA, Music
            in 11..20 -> MANGA_FOLDER // Manga types
            else -> {
                // Fallback: try to determine from other clues
                val title = entry.title?.lowercase() ?: ""
                val genres = entry.genres?.lowercase() ?: ""
                
                when {
                    title.contains("manga") || genres.contains("manga") -> MANGA_FOLDER
                    title.contains("anime") || genres.contains("anime") -> ANIME_FOLDER
                    else -> ANIME_FOLDER // Default to anime
                }
            }
        }
    }

    /**
     * Check if content contains hentai/adult material
     */
    private fun isHentaiContent(entry: AnimeEntry): Boolean {
        val title = entry.title?.lowercase() ?: ""
        val genres = entry.genres?.lowercase() ?: ""
        
        // Check genres and title for hentai keywords
        return HENTAI_KEYWORDS.any { keyword ->
            title.contains(keyword) || genres.contains(keyword)
        }
    }

    /**
     * Organize hentai content into subcategories
     */
    private fun organizeHentaiContent(entry: AnimeEntry, mediaType: String): File {
        val subcategory = classifyHentaiSubcategory(entry)
        val targetDir = File(baseDir, "$mediaType/$HENTAI_FOLDER/$subcategory")
        
        if (!targetDir.exists()) {
            targetDir.mkdirs()
            // Add .nomedia file for privacy
            createNoMediaFile(targetDir)
            Log.i(TAG, "Created hentai directory with privacy: ${targetDir.path}")
        }
        
        return targetDir
    }

    /**
     * Organize regular (non-hentai) content by genre
     */
    private fun organizeRegularContent(entry: AnimeEntry, mediaType: String): File {
        val primaryGenre = extractPrimaryGenre(entry)
        val targetDir = File(baseDir, "$mediaType/$primaryGenre")
        
        if (!targetDir.exists()) {
            targetDir.mkdirs()
            Log.i(TAG, "Created directory: ${targetDir.path}")
        }
        
        return targetDir
    }

    /**
     * Classify hentai content into subcategories
     */
    private fun classifyHentaiSubcategory(entry: AnimeEntry): String {
        val title = entry.title?.lowercase() ?: ""
        val genres = entry.genres?.lowercase() ?: ""
        val allText = "$title $genres"
        
        // Check for specific subcategory keywords
        HENTAI_SUBCATEGORIES.forEach { (category, keywords) ->
            if (keywords.any { keyword -> allText.contains(keyword) }) {
                return sanitizeFolderName(category.capitalize())
            }
        }
        
        // Default subcategory
        return "Other"
    }

    /**
     * Extract primary genre from MAL entry
     */
    private fun extractPrimaryGenre(entry: AnimeEntry): String {
        val genres = entry.genres
        
        if (genres.isNullOrEmpty()) {
            return UNKNOWN_GENRE
        }
        
        // Split genres by common separators
        val genreList = genres.split(",", ";", "|").map { it.trim() }
        
        // Return first non-empty genre, sanitized
        val primaryGenre = genreList.firstOrNull { it.isNotEmpty() } ?: UNKNOWN_GENRE
        return sanitizeFolderName(primaryGenre)
    }

    /**
     * Sanitize folder name for filesystem safety
     */
    private fun sanitizeFolderName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9\\s-_]"), "") // Remove special chars
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
            .take(50) // Limit length
            .ifEmpty { UNKNOWN_GENRE }
    }

    /**
     * Create .nomedia file to hide folder from gallery apps
     */
    private fun createNoMediaFile(directory: File) {
        try {
            val noMediaFile = File(directory, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
                Log.i(TAG, "Created .nomedia file in: ${directory.path}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create .nomedia file", e)
        }
    }

    /**
     * Get all organized folders for status reporting
     */
    fun getOrganizedFolders(): List<OrganizedFolder> {
        val folders = mutableListOf<OrganizedFolder>()
        
        if (!baseDir.exists()) {
            return folders
        }
        
        // Scan ANIME folders
        val animeDir = File(baseDir, ANIME_FOLDER)
        if (animeDir.exists()) {
            animeDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    val imageCount = countImagesInFolder(folder)
                    folders.add(
                        OrganizedFolder(
                            path = folder.path,
                            name = folder.name,
                            type = ANIME_FOLDER,
                            imageCount = imageCount,
                            isHentai = folder.path.contains(HENTAI_FOLDER)
                        )
                    )
                }
            }
        }
        
        // Scan MANGA folders
        val mangaDir = File(baseDir, MANGA_FOLDER)
        if (mangaDir.exists()) {
            mangaDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    val imageCount = countImagesInFolder(folder)
                    folders.add(
                        OrganizedFolder(
                            path = folder.path,
                            name = folder.name,
                            type = MANGA_FOLDER,
                            imageCount = imageCount,
                            isHentai = folder.path.contains(HENTAI_FOLDER)
                        )
                    )
                }
            }
        }
        
        return folders
    }

    /**
     * Count images in a folder (including subfolders)
     */
    private fun countImagesInFolder(folder: File): Int {
        var count = 0
        
        folder.walkTopDown().forEach { file ->
            if (file.isFile && isImageFile(file)) {
                count++
            }
        }
        
        return count
    }

    /**
     * Check if file is an image
     */
    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf("jpg", "jpeg", "png", "webp", "gif")
    }

    /**
     * Clean up empty folders
     */
    fun cleanupEmptyFolders() {
        if (!baseDir.exists()) return
        
        baseDir.walkBottomUp().forEach { folder ->
            if (folder.isDirectory && folder != baseDir) {
                val contents = folder.listFiles()
                if (contents.isNullOrEmpty() || 
                   (contents.size == 1 && contents[0].name == ".nomedia")) {
                    folder.deleteRecursively()
                    Log.i(TAG, "Removed empty folder: ${folder.path}")
                }
            }
        }
    }
}

/**
 * Data class representing an organized folder
 */
data class OrganizedFolder(
    val path: String,
    val name: String,
    val type: String, // ANIME or MANGA
    val imageCount: Int,
    val isHentai: Boolean
)