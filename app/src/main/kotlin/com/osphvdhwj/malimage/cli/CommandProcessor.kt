/**
 * CommandProcessor.kt
 *
 * Processes CLI-style commands for power users to control the MAL Image Downloader.
 * Provides a lightweight interface for loading XML, managing downloads, and organizing content.
 *
 * Supported Commands:
 * - load-xml <path>: Load MAL XML export file
 * - download-all: Start downloading all loaded images
 * - organize anime|manga: Organize images by type
 * - status: Show download and organization status
 * - settings: Manage app configuration
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.cli

import android.content.Context
import com.osphvdhwj.malimage.download.DownloadManager
import com.osphvdhwj.malimage.parser.AnimeEntry
import com.osphvdhwj.malimage.parser.MALXmlParser
import com.osphvdhwj.malimage.storage.FolderOrganizer
import java.io.File

class CommandProcessor(private val context: Context) {

    private val downloadManager = DownloadManager(context)
    private val xmlParser = MALXmlParser()
    private val folderOrganizer = FolderOrganizer()
    
    // Store loaded entries for batch operations
    private var loadedAnimeEntries = mutableListOf<AnimeEntry>()
    private var loadedMangaEntries = mutableListOf<AnimeEntry>()

    companion object {
        private val SUPPORTED_COMMANDS = mapOf(
            "load-xml" to "Load MAL XML export file: load-xml <path>",
            "download-all" to "Download all loaded images",
            "organize" to "Organize images: organize anime|manga|anime-hentai|manga-hentai",
            "status" to "Show download and folder status",
            "settings" to "Manage settings: settings show|set key=value",
            "help" to "Show this help message",
            "clear" to "Clear loaded entries",
            "count" to "Show count of loaded entries"
        )
    }

    /**
     * Process a command and return the result
     * 
     * @param command The command string to process
     * @return CommandResult with success status and message
     */
    fun processCommand(command: String): CommandResult {
        val parts = command.trim().split(" ")
        if (parts.isEmpty()) {
            return CommandResult.error("Empty command")
        }

        val commandName = parts[0].lowercase()
        val args = parts.drop(1)

        return when (commandName) {
            "load-xml" -> processLoadXml(args)
            "download-all" -> processDownloadAll()
            "organize" -> processOrganize(args)
            "status" -> processStatus()
            "settings" -> processSettings(args)
            "help" -> processHelp()
            "clear" -> processClear()
            "count" -> processCount()
            else -> CommandResult.error("Unknown command: $commandName. Type 'help' for available commands.")
        }
    }

    /**
     * Load MAL XML export file
     */
    private fun processLoadXml(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Usage: load-xml <path>")
        }

        val xmlPath = args.joinToString(" ")
        val xmlFile = File(xmlPath)

        if (!xmlFile.exists()) {
            return CommandResult.error("File not found: $xmlPath")
        }

        return try {
            val malExport = xmlParser.parseXmlFile(xmlFile)
            
            if (malExport == null) {
                return CommandResult.error("Failed to parse XML file")
            }

            // Clear previous entries
            loadedAnimeEntries.clear()
            loadedMangaEntries.clear()

            // Load anime entries
            malExport.animeList?.let { animeList ->
                loadedAnimeEntries.addAll(animeList.filter { it.imageUrl != null })
            }

            // Load manga entries
            malExport.mangaList?.let { mangaList ->
                loadedMangaEntries.addAll(mangaList.filter { it.imageUrl != null })
            }

            val totalLoaded = loadedAnimeEntries.size + loadedMangaEntries.size
            CommandResult.success(
                "Successfully loaded $totalLoaded entries\n" +
                "Anime: ${loadedAnimeEntries.size}\n" +
                "Manga: ${loadedMangaEntries.size}"
            )
        } catch (e: Exception) {
            CommandResult.error("Error loading XML: ${e.message}")
        }
    }

    /**
     * Download all loaded images
     */
    private fun processDownloadAll(): CommandResult {
        val totalEntries = loadedAnimeEntries.size + loadedMangaEntries.size
        
        if (totalEntries == 0) {
            return CommandResult.error("No entries loaded. Use 'load-xml' first.")
        }

        val allEntries = loadedAnimeEntries + loadedMangaEntries
        val workIds = downloadManager.enqueueDownloads(allEntries)

        return CommandResult.success(
            "Enqueued $totalEntries downloads\n" +
            "Queue IDs: ${workIds.size}\n" +
            "Use 'status' to monitor progress"
        )
    }

    /**
     * Organize content by type
     */
    private fun processOrganize(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Usage: organize anime|manga|anime-hentai|manga-hentai")
        }

        val organizeType = args[0].lowercase()
        
        return when (organizeType) {
            "anime" -> {
                val folders = folderOrganizer.getOrganizedFolders()
                    .filter { it.type == "ANIME" && !it.isHentai }
                CommandResult.success(
                    "Anime folders organized: ${folders.size}\n" +
                    folders.joinToString("\n") { "${it.name}: ${it.imageCount} images" }
                )
            }
            "manga" -> {
                val folders = folderOrganizer.getOrganizedFolders()
                    .filter { it.type == "MANGA" && !it.isHentai }
                CommandResult.success(
                    "Manga folders organized: ${folders.size}\n" +
                    folders.joinToString("\n") { "${it.name}: ${it.imageCount} images" }
                )
            }
            "anime-hentai" -> {
                val folders = folderOrganizer.getOrganizedFolders()
                    .filter { it.type == "ANIME" && it.isHentai }
                CommandResult.success(
                    "Anime hentai folders organized: ${folders.size}\n" +
                    folders.joinToString("\n") { "${it.name}: ${it.imageCount} images" }
                )
            }
            "manga-hentai" -> {
                val folders = folderOrganizer.getOrganizedFolders()
                    .filter { it.type == "MANGA" && it.isHentai }
                CommandResult.success(
                    "Manga hentai folders organized: ${folders.size}\n" +
                    folders.joinToString("\n") { "${it.name}: ${it.imageCount} images" }
                )
            }
            else -> CommandResult.error("Invalid organize type: $organizeType")
        }
    }

    /**
     * Show status of downloads and organization
     */
    private fun processStatus(): CommandResult {
        val downloadStatus = downloadManager.getDownloadStatus()
        val runningProgress = downloadManager.getRunningDownloadsProgress()
        val organizedFolders = folderOrganizer.getOrganizedFolders()
        
        val statusText = buildString {
            appendLine("=== Download Status ===")
            appendLine("Total: ${downloadStatus.total}")
            appendLine("Queued: ${downloadStatus.queued}")
            appendLine("Running: ${downloadStatus.running}")
            appendLine("Succeeded: ${downloadStatus.succeeded}")
            appendLine("Failed: ${downloadStatus.failed}")
            appendLine("Success Rate: ${String.format("%.1f", downloadStatus.successRate * 100)}%")
            
            if (runningProgress.isNotEmpty()) {
                appendLine("\n=== Running Downloads ===")
                runningProgress.forEach { progress ->
                    appendLine("${progress.status} (${progress.progress}%)")
                }
            }
            
            appendLine("\n=== Organized Folders ===")
            val animeRegular = organizedFolders.filter { it.type == "ANIME" && !it.isHentai }
            val animeHentai = organizedFolders.filter { it.type == "ANIME" && it.isHentai }
            val mangaRegular = organizedFolders.filter { it.type == "MANGA" && !it.isHentai }
            val mangaHentai = organizedFolders.filter { it.type == "MANGA" && it.isHentai }
            
            appendLine("Anime Regular: ${animeRegular.size} folders, ${animeRegular.sumOf { it.imageCount }} images")
            appendLine("Anime Hentai: ${animeHentai.size} folders, ${animeHentai.sumOf { it.imageCount }} images")
            appendLine("Manga Regular: ${mangaRegular.size} folders, ${mangaRegular.sumOf { it.imageCount }} images")
            appendLine("Manga Hentai: ${mangaHentai.size} folders, ${mangaHentai.sumOf { it.imageCount }} images")
            
            appendLine("\n=== Loaded Entries ===")
            appendLine("Anime: ${loadedAnimeEntries.size}")
            appendLine("Manga: ${loadedMangaEntries.size}")
        }
        
        return CommandResult.success(statusText)
    }

    /**
     * Manage app settings
     */
    private fun processSettings(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Usage: settings show|set key=value")
        }

        val action = args[0].lowercase()
        
        return when (action) {
            "show" -> {
                val prefs = context.getSharedPreferences("mal_settings", Context.MODE_PRIVATE)
                val settings = prefs.all
                
                if (settings.isEmpty()) {
                    CommandResult.success("No settings configured")
                } else {
                    val settingsText = settings.entries.joinToString("\n") { "${it.key}=${it.value}" }
                    CommandResult.success("Current settings:\n$settingsText")
                }
            }
            "set" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: settings set key=value")
                }
                
                val keyValue = args.drop(1).joinToString(" ")
                val parts = keyValue.split("=", limit = 2)
                
                if (parts.size != 2) {
                    return CommandResult.error("Invalid format. Use: key=value")
                }
                
                val key = parts[0].trim()
                val value = parts[1].trim()
                
                val prefs = context.getSharedPreferences("mal_settings", Context.MODE_PRIVATE)
                prefs.edit().putString(key, value).apply()
                
                CommandResult.success("Set $key = $value")
            }
            else -> CommandResult.error("Unknown settings action: $action")
        }
    }

    /**
     * Show help message
     */
    private fun processHelp(): CommandResult {
        val helpText = buildString {
            appendLine("MAL Image Downloader Commands:")
            appendLine()
            SUPPORTED_COMMANDS.forEach { (command, description) ->
                appendLine("$command - $description")
            }
            appendLine()
            appendLine("Examples:")
            appendLine("load-xml /storage/emulated/0/Download/mal_export.xml")
            appendLine("download-all")
            appendLine("organize anime")
            appendLine("settings set mal_client_id=your_client_id")
        }
        
        return CommandResult.success(helpText)
    }

    /**
     * Clear loaded entries
     */
    private fun processClear(): CommandResult {
        val totalCleared = loadedAnimeEntries.size + loadedMangaEntries.size
        loadedAnimeEntries.clear()
        loadedMangaEntries.clear()
        
        return CommandResult.success("Cleared $totalCleared loaded entries")
    }

    /**
     * Show count of loaded entries
     */
    private fun processCount(): CommandResult {
        return CommandResult.success(
            "Loaded entries:\n" +
            "Anime: ${loadedAnimeEntries.size}\n" +
            "Manga: ${loadedMangaEntries.size}\n" +
            "Total: ${loadedAnimeEntries.size + loadedMangaEntries.size}"
        )
    }
}

/**
 * Result of command processing
 */
data class CommandResult(
    val success: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = CommandResult(true, message)
        fun error(message: String) = CommandResult(false, message)
    }
}