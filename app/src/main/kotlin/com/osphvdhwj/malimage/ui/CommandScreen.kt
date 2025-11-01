/**
 * CommandScreen.kt
 *
 * Command interface screen for the MAL Image Downloader app.
 * Provides a CLI-style interface for power users with command history and help.
 *
 * Features:
 * - Command input with auto-completion suggestions
 * - Command history and scrolling
 * - Real-time command feedback
 * - Help system with command examples
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.osphvdhwj.malimage.cli.CommandProcessor
import com.osphvdhwj.malimage.cli.CommandResult
import com.osphvdhwj.malimage.safety.ParentalControlManager
import com.osphvdhwj.malimage.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandScreen(
    viewModel: MainViewModel,
    parentalControlManager: ParentalControlManager
) {
    val context = LocalContext.current
    val commandProcessor = remember { CommandProcessor(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var currentCommand by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<CommandHistoryItem>()) }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new commands are added
    LaunchedEffect(commandHistory.size) {
        if (commandHistory.isNotEmpty()) {
            listState.animateScrollToItem(commandHistory.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with help button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Command Interface",
                style = MaterialTheme.typography.headlineSmall
            )
            
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val helpResult = commandProcessor.processCommand("help")
                        commandHistory = commandHistory + CommandHistoryItem(
                            command = "help",
                            result = helpResult,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Help, contentDescription = "Help")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Command history display
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (commandHistory.isEmpty()) {
                    item {
                        Text(
                            "Welcome to MAL Image Downloader!\n" +
                            "Type 'help' for available commands.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                items(commandHistory) { historyItem ->
                    CommandHistoryItem(historyItem)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Command input
        OutlinedTextField(
            value = currentCommand,
            onValueChange = { currentCommand = it },
            label = { Text("Enter command") },
            placeholder = { Text("e.g., load-xml /path/to/file.xml") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (currentCommand.isNotBlank()) {
                            coroutineScope.launch {
                                val result = commandProcessor.processCommand(currentCommand)
                                commandHistory = commandHistory + CommandHistoryItem(
                                    command = currentCommand,
                                    result = result,
                                    timestamp = System.currentTimeMillis()
                                )
                                currentCommand = ""
                            }
                        }
                    },
                    enabled = currentCommand.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Execute")
                }
            },
            singleLine = true
        )
        
        // Quick command buttons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SuggestionChip(
                    onClick = { currentCommand = "status" },
                    label = { Text("status") }
                )
            }
            item {
                SuggestionChip(
                    onClick = { currentCommand = "download-all" },
                    label = { Text("download-all") }
                )
            }
            item {
                SuggestionChip(
                    onClick = { currentCommand = "organize anime" },
                    label = { Text("organize anime") }
                )
            }
            item {
                SuggestionChip(
                    onClick = { currentCommand = "help" },
                    label = { Text("help") }
                )
            }
        }
    }
}

@Composable
fun CommandHistoryItem(historyItem: CommandHistoryItem) {
    Column {
        // Command input
        Text(
            "❯ ${historyItem.command}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Command result
        Text(
            historyItem.result.message,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = if (historyItem.result.success) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(start = 8.dp)
        )
        
        Divider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Data class for command history items
 */
data class CommandHistoryItem(
    val command: String,
    val result: CommandResult,
    val timestamp: Long
)