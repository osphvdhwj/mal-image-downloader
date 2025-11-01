/**
 * SettingsScreen.kt
 *
 * Settings and configuration screen for the MAL Image Downloader app.
 * Provides access to parental controls, API settings, and app preferences.
 *
 * Features:
 * - MAL Client ID configuration
 * - Parental control settings with PIN protection
 * - Download preferences (WiFi only, charging required)
 * - Folder organization settings
 * - Content filtering and age restrictions
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.osphvdhwj.malimage.safety.ParentalControlManager
import com.osphvdhwj.malimage.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    parentalControlManager: ParentalControlManager
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("mal_settings", android.content.Context.MODE_PRIVATE)
    
    var malClientId by remember { mutableStateOf(prefs.getString("mal_client_id", "") ?: "") }
    var secondaryApiUrl by remember { mutableStateOf(prefs.getString("secondary_api_url", "") ?: "") }
    var downloadOnWifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var requireCharging by remember { mutableStateOf(prefs.getBoolean("require_charging", false)) }
    
    // Parental control states
    val parentalSettings = parentalControlManager.getSettings()
    var showPinDialog by remember { mutableStateOf(false) }
    var showParentalSettings by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // API Configuration Section
        item {
            SettingsSection("API Configuration") {
                OutlinedTextField(
                    value = malClientId,
                    onValueChange = { 
                        malClientId = it
                        prefs.edit().putString("mal_client_id", it).apply()
                    },
                    label = { Text("MAL Client ID") },
                    placeholder = { Text("Enter your MyAnimeList Client ID") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = secondaryApiUrl,
                    onValueChange = { 
                        secondaryApiUrl = it
                        prefs.edit().putString("secondary_api_url", it).apply()
                    },
                    label = { Text("Secondary API URL (optional)") },
                    placeholder = { Text("https://api.backup-service.com") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
                
                Text(
                    "The secondary API provides redundancy if the primary MAL API is unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Download Preferences Section
        item {
            SettingsSection("Download Preferences") {
                SettingsSwitch(
                    title = "WiFi Only",
                    description = "Download images only when connected to WiFi",
                    checked = downloadOnWifiOnly,
                    onCheckedChange = { 
                        downloadOnWifiOnly = it
                        prefs.edit().putBoolean("wifi_only", it).apply()
                    },
                    icon = Icons.Default.Wifi
                )
                
                SettingsSwitch(
                    title = "Require Charging",
                    description = "Download images only when device is charging",
                    checked = requireCharging,
                    onCheckedChange = { 
                        requireCharging = it
                        prefs.edit().putBoolean("require_charging", it).apply()
                    },
                    icon = Icons.Default.Battery4Bar
                )
            }
        }
        
        // Parental Controls Section
        item {
            SettingsSection("Parental Controls") {
                SettingsItem(
                    title = "Parental Controls",
                    description = if (parentalSettings.enabled) {
                        "Enabled - Content filtering active"
                    } else {
                        "Disabled - All content accessible"
                    },
                    icon = if (parentalSettings.enabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                    onClick = { 
                        if (parentalSettings.enabled) {
                            showPinDialog = true
                        } else {
                            showParentalSettings = true
                        }
                    }
                )
                
                if (parentalSettings.enabled) {
                    Text(
                        "Max Rating: ${parentalSettings.maxRating.description}\n" +
                        "Hentai Blocked: ${if (parentalSettings.blockHentai) "Yes" else "No"}\n" +
                        "Age Verified: ${if (parentalSettings.ageVerified) "Yes" else "No"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                    )
                }
            }
        }
        
        // About Section
        item {
            SettingsSection("About") {
                SettingsItem(
                    title = "Version",
                    description = "1.0.0",
                    icon = Icons.Default.Info
                )
                
                SettingsItem(
                    title = "GitHub Repository",
                    description = "View source code and report issues",
                    icon = Icons.Default.Code,
                    onClick = {
                        // Open GitHub repository
                        // Note: In real app, you'd use Intent to open browser
                    }
                )
            }
        }
    }
    
    // PIN Dialog for accessing parental controls
    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onPinEntered = { pin ->
                if (parentalControlManager.verifyPin(pin)) {
                    showPinDialog = false
                    showParentalSettings = true
                } else {
                    // Show error - incorrect PIN
                }
            }
        )
    }
    
    // Parental Settings Dialog
    if (showParentalSettings) {
        ParentalSettingsDialog(
            currentSettings = parentalSettings,
            onDismiss = { showParentalSettings = false },
            onSettingsUpdated = { newSettings, pin ->
                parentalControlManager.updateSettings(newSettings, pin)
                showParentalSettings = false
            },
            parentalControlManager = parentalControlManager
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onPinEntered: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter PIN") },
        text = {
            Column {
                Text("Enter your parental control PIN to access settings.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPinEntered(pin) },
                enabled = pin.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}