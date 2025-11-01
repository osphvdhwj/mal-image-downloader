/**
 * MALApiClient.kt
 *
 * This class handles communication with the MyAnimeList (MAL) API using your MAL Client ID.
 * To minimize downtime, it supports a secondary API as a fallback for redundancy.
 *
 * Features:
 * - Fetch anime/manga details including images
 * - Handles error and retry logic
 * - Easily extendable for additional API endpoints
 *
 * Usage:
 * 1. Initialize MALApiClient with your client ID and secondary API configs.
 * 2. Call fetchAnime or fetchManga methods with MAL IDs.
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MALApiClient(private val malClientId: String, private val secondaryApiUrl: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MALApiClient"
        private const val MAL_BASE_URL = "https://api.myanimelist.net/v2"
    }

    /**
     * Fetch Anime details from MAL API by anime ID.
     * Tries primary API first; if fails, falls back to secondary API.
     *
     * @param animeId MAL Anime ID
     * @return JSON payload String or null if failed
     */
    suspend fun fetchAnime(animeId: Int): String? = withContext(Dispatchers.IO) {
        val primaryUrl = "$MAL_BASE_URL/anime/$animeId?fields=main_picture,title,genres"
        try {
            return@withContext fetchFromUrl(primaryUrl, malClientId) ?: fetchFromSecondary(animeId, "anime")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching anime from primary MAL API", e)
            return@withContext fetchFromSecondary(animeId, "anime")
        }
    }

    /**
     * Fetch Manga details from MAL API by manga ID.
     * Tries primary API first; if fails, falls back to secondary API.
     *
     * @param mangaId MAL Manga ID
     * @return JSON payload String or null if failed
     */
    suspend fun fetchManga(mangaId: Int): String? = withContext(Dispatchers.IO) {
        val primaryUrl = "$MAL_BASE_URL/manga/$mangaId?fields=main_picture,title,genres"
        try {
            return@withContext fetchFromUrl(primaryUrl, malClientId) ?: fetchFromSecondary(mangaId, "manga")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching manga from primary MAL API", e)
            return@withContext fetchFromSecondary(mangaId, "manga")
        }
    }

    /**
     * Internal helper to perform HTTP GET with MAL Client ID authentication
     */
    private fun fetchFromUrl(url: String, clientId: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("X-MAL-CLIENT-ID", clientId)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Unsuccessful response code ${response.code}")
                return null
            }
            return response.body?.string()
        }
    }

    /**
     * Fetch from Secondary API fallback
     *
     * @param id anime or manga ID
     * @param type "anime" or "manga"
     * @return JSON payload String or null
     */
    private fun fetchFromSecondary(id: Int, type: String): String? {
        val url = "$secondaryApiUrl/$type/$id"
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Secondary API failed with status ${response.code}")
                return null
            }
            return response.body?.string()
        }
    }
}
