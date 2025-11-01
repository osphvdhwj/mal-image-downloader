/**
 * ParentalControlManager.kt
 *
 * Manages parental controls and content filtering for the MAL Image Downloader.
 * Provides PIN-protected settings and age-appropriate content filtering.
 *
 * Features:
 * - PIN-based parental lock system
 * - Content rating filtering (PG, PG-13, R, X, XXX)
 * - Hentai content blocking and hiding
 * - Age verification system
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.safety

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.osphvdhwj.malimage.parser.AnimeEntry
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Period

class ParentalControlManager(private val context: Context) {

    companion object {
        private const val TAG = "ParentalControls"
        private const val PREFS_NAME = "parental_controls"
        private const val KEY_ENABLED = "parental_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_MAX_RATING = "max_rating"
        private const val KEY_BLOCK_HENTAI = "block_hentai"
        private const val KEY_HIDE_HENTAI_FOLDERS = "hide_hentai_folders"
        private const val KEY_AGE_VERIFIED = "age_verified"
        private const val KEY_BLOCKED_TAGS = "blocked_tags"
        
        private const val MIN_AGE_REQUIREMENT = 18
        
        // Content rating levels
        enum class ContentRating(val level: Int, val description: String) {
            PG(1, "General Audiences"),
            PG_13(2, "Teens 13+"),
            R(3, "Mature 17+"),
            X(4, "Adults Only 18+"),
            XXX(5, "Explicit Adult Content")
        }
        
        // Blocked content keywords
        private val DEFAULT_BLOCKED_TAGS = setOf(
            "extreme", "gore", "violence", "torture", "abuse",
            "rape", "non-con", "loli", "shota", "bestiality",
            "necro", "scat", "vore", "snuff"
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Enable parental controls with PIN protection
     */
    fun enableParentalControls(pin: String, maxRating: ContentRating = ContentRating.PG_13): Boolean {
        return try {
            val pinHash = hashPin(pin)
            prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_PIN_HASH, pinHash)
                .putInt(KEY_MAX_RATING, maxRating.level)
                .putBoolean(KEY_BLOCK_HENTAI, true)
                .putBoolean(KEY_HIDE_HENTAI_FOLDERS, true)
                .apply()
            
            Log.i(TAG, "Parental controls enabled with rating: ${maxRating.description}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable parental controls", e)
            false
        }
    }

    /**
     * Disable parental controls with PIN verification
     */
    fun disableParentalControls(pin: String): Boolean {
        if (!verifyPin(pin)) {
            return false
        }
        
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .apply()
        
        Log.i(TAG, "Parental controls disabled")
        return true
    }

    /**
     * Verify PIN for parental control access
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    /**
     * Check if parental controls are enabled
     */
    fun isParentalControlsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    /**
     * Check if content is allowed based on parental control settings
     */
    fun isContentAllowed(entry: AnimeEntry): Boolean {
        if (!isParentalControlsEnabled()) {
            return true
        }
        
        // Check if hentai content is blocked
        if (prefs.getBoolean(KEY_BLOCK_HENTAI, true) && isHentaiContent(entry)) {
            return false
        }
        
        // Check content rating
        val maxRating = prefs.getInt(KEY_MAX_RATING, ContentRating.PG_13.level)
        val contentRating = determineContentRating(entry)
        
        if (contentRating.level > maxRating) {
            return false
        }
        
        // Check blocked tags
        if (containsBlockedTags(entry)) {
            return false
        }
        
        return true
    }

    /**
     * Check if hentai folders should be hidden from UI
     */
    fun shouldHideHentaiFolders(): Boolean {
        return isParentalControlsEnabled() && 
               prefs.getBoolean(KEY_HIDE_HENTAI_FOLDERS, true)
    }

    /**
     * Verify user age for adult content access
     */
    fun verifyAge(birthDate: LocalDate): AgeVerificationResult {
        val age = Period.between(birthDate, LocalDate.now()).years
        
        return when {
            age >= MIN_AGE_REQUIREMENT -> {
                prefs.edit().putBoolean(KEY_AGE_VERIFIED, true).apply()
                AgeVerificationResult.VERIFIED
            }
            age < 0 -> AgeVerificationResult.INVALID_DATE
            else -> AgeVerificationResult.UNDERAGE
        }
    }

    /**
     * Check if age has been verified
     */
    fun isAgeVerified(): Boolean {
        return prefs.getBoolean(KEY_AGE_VERIFIED, false)
    }

    /**
     * Get current parental control settings
     */
    fun getSettings(): ParentalControlSettings {
        return ParentalControlSettings(
            enabled = isParentalControlsEnabled(),
            maxRating = ContentRating.values().find { 
                it.level == prefs.getInt(KEY_MAX_RATING, ContentRating.PG_13.level) 
            } ?: ContentRating.PG_13,
            blockHentai = prefs.getBoolean(KEY_BLOCK_HENTAI, true),
            hideHentaiFolders = prefs.getBoolean(KEY_HIDE_HENTAI_FOLDERS, true),
            ageVerified = isAgeVerified()
        )
    }

    /**
     * Update parental control settings (requires PIN if enabled)
     */
    fun updateSettings(settings: ParentalControlSettings, pin: String? = null): Boolean {
        if (isParentalControlsEnabled() && !verifyPin(pin ?: "")) {
            return false
        }
        
        prefs.edit()
            .putInt(KEY_MAX_RATING, settings.maxRating.level)
            .putBoolean(KEY_BLOCK_HENTAI, settings.blockHentai)
            .putBoolean(KEY_HIDE_HENTAI_FOLDERS, settings.hideHentaiFolders)
            .apply()
        
        return true
    }

    /**
     * Add custom blocked tag
     */
    fun addBlockedTag(tag: String) {
        val currentTags = getBlockedTags().toMutableSet()
        currentTags.add(tag.lowercase())
        saveBlockedTags(currentTags)
    }

    /**
     * Remove blocked tag
     */
    fun removeBlockedTag(tag: String) {
        val currentTags = getBlockedTags().toMutableSet()
        currentTags.remove(tag.lowercase())
        saveBlockedTags(currentTags)
    }

    /**
     * Get current blocked tags
     */
    fun getBlockedTags(): Set<String> {
        val tagsString = prefs.getString(KEY_BLOCKED_TAGS, null)
        return if (tagsString != null) {
            tagsString.split(",").map { it.trim() }.toSet()
        } else {
            DEFAULT_BLOCKED_TAGS
        }
    }

    // Private helper methods
    
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun isHentaiContent(entry: AnimeEntry): Boolean {
        val title = entry.title?.lowercase() ?: ""
        val genres = entry.genres?.lowercase() ?: ""
        
        val hentaiKeywords = setOf(
            "hentai", "ecchi", "adult", "18+", "nsfw", "explicit",
            "sexual", "erotic", "mature", "r+", "xxx"
        )
        
        return hentaiKeywords.any { keyword ->
            title.contains(keyword) || genres.contains(keyword)
        }
    }

    private fun determineContentRating(entry: AnimeEntry): ContentRating {
        val title = entry.title?.lowercase() ?: ""
        val genres = entry.genres?.lowercase() ?: ""
        val allText = "$title $genres"
        
        return when {
            allText.contains("xxx") || allText.contains("explicit") -> ContentRating.XXX
            allText.contains("hentai") || allText.contains("adult") -> ContentRating.X
            allText.contains("ecchi") || allText.contains("mature") -> ContentRating.R
            allText.contains("teen") || allText.contains("violence") -> ContentRating.PG_13
            else -> ContentRating.PG
        }
    }

    private fun containsBlockedTags(entry: AnimeEntry): Boolean {
        val title = entry.title?.lowercase() ?: ""
        val genres = entry.genres?.lowercase() ?: ""
        val allText = "$title $genres"
        
        val blockedTags = getBlockedTags()
        
        return blockedTags.any { tag ->
            allText.contains(tag)
        }
    }

    private fun saveBlockedTags(tags: Set<String>) {
        val tagsString = tags.joinToString(",")
        prefs.edit().putString(KEY_BLOCKED_TAGS, tagsString).apply()
    }
}

/**
 * Data class for parental control settings
 */
data class ParentalControlSettings(
    val enabled: Boolean,
    val maxRating: ParentalControlManager.Companion.ContentRating,
    val blockHentai: Boolean,
    val hideHentaiFolders: Boolean,
    val ageVerified: Boolean
)

/**
 * Age verification result
 */
enum class AgeVerificationResult {
    VERIFIED,
    UNDERAGE,
    INVALID_DATE
}