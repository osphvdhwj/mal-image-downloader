/**
 * AgeVerificationDialog.kt
 *
 * Age verification dialog for the MAL Image Downloader app.
 * Required for accessing adult content and ensuring compliance with age restrictions.
 *
 * Features:
 * - Birth date input with validation
 * - Age calculation and verification
 * - Compliance with 18+ content requirements
 * - Integration with parental control system
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.osphvdhwj.malimage.safety.AgeVerificationResult
import com.osphvdhwj.malimage.safety.ParentalControlManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun AgeVerificationDialog(
    onVerified: () -> Unit,
    onDenied: () -> Unit,
    parentalControlManager: ParentalControlManager
) {
    var birthDate by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDenied,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.warning,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Age Verification Required",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "This application may contain adult content. You must be 18 years or older to use this app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { 
                        birthDate = it
                        showError = false
                    },
                    label = { Text("Birth Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    supportingText = { Text("Enter your birth date in YYYY-MM-DD format") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy notice
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Privacy Notice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your birth date is used only for age verification and is stored locally on your device. It is not shared with any third parties.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val date = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
                        val result = parentalControlManager.verifyAge(date)
                        
                        when (result) {
                            AgeVerificationResult.VERIFIED -> {
                                onVerified()
                            }
                            AgeVerificationResult.UNDERAGE -> {
                                showError = true
                                errorMessage = "You must be 18 or older to use this application."
                            }
                            AgeVerificationResult.INVALID_DATE -> {
                                showError = true
                                errorMessage = "Please enter a valid birth date."
                            }
                        }
                    } catch (e: DateTimeParseException) {
                        showError = true
                        errorMessage = "Please enter date in YYYY-MM-DD format (e.g., 2000-01-01)"
                    }
                },
                enabled = birthDate.isNotBlank()
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDenied) {
                Text("Exit App")
            }
        }
    )
}

@Composable
fun ParentalSettingsDialog(
    currentSettings: com.osphvdhwj.malimage.safety.ParentalControlSettings,
    onDismiss: () -> Unit,
    onSettingsUpdated: (com.osphvdhwj.malimage.safety.ParentalControlSettings, String?) -> Unit,
    parentalControlManager: ParentalControlManager
) {
    var enableControls by remember { mutableStateOf(currentSettings.enabled) }
    var maxRating by remember { mutableStateOf(currentSettings.maxRating) }
    var blockHentai by remember { mutableStateOf(currentSettings.blockHentai) }
    var hideHentaiFolders by remember { mutableStateOf(currentSettings.hideHentaiFolders) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPinError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parental Control Settings") },
        text = {
            LazyColumn {
                item {
                    // Enable/Disable Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Parental Controls")
                        Switch(
                            checked = enableControls,
                            onCheckedChange = { enableControls = it }
                        )
                    }
                    
                    if (enableControls) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Content Rating Selection
                        Text(
                            "Maximum Content Rating",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        ParentalControlManager.Companion.ContentRating.values().forEach { rating ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = maxRating == rating,
                                    onClick = { maxRating = rating }
                                )
                                Column(
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(rating.name)
                                    Text(
                                        rating.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Additional Options
                        SettingsSwitch(
                            title = "Block Hentai Content",
                            description = "Prevent download and access to adult content",
                            checked = blockHentai,
                            onCheckedChange = { blockHentai = it },
                            icon = Icons.Default.Block
                        )
                        
                        SettingsSwitch(
                            title = "Hide Hentai Folders",
                            description = "Hide adult content folders from app interface",
                            checked = hideHentaiFolders,
                            onCheckedChange = { hideHentaiFolders = it },
                            icon = Icons.Default.VisibilityOff
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // PIN Setup
                        Text(
                            "PIN Protection",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { 
                                pin = it
                                showPinError = false
                            },
                            label = { Text(if (currentSettings.enabled) "Current PIN" else "Set PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (!currentSettings.enabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { 
                                    confirmPin = it
                                    showPinError = false
                                },
                                label = { Text("Confirm PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword
                                ),
                                isError = showPinError,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (showPinError) {
                                Text(
                                    "PINs do not match",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (enableControls && !currentSettings.enabled) {
                        // Setting up new PIN
                        if (pin != confirmPin) {
                            showPinError = true
                            return@TextButton
                        }
                        if (pin.length < 4) {
                            showPinError = true
                            return@TextButton
                        }
                    }
                    
                    val newSettings = com.osphvdhwj.malimage.safety.ParentalControlSettings(
                        enabled = enableControls,
                        maxRating = maxRating,
                        blockHentai = blockHentai,
                        hideHentaiFolders = hideHentaiFolders,
                        ageVerified = currentSettings.ageVerified
                    )
                    
                    onSettingsUpdated(newSettings, pin.takeIf { it.isNotBlank() })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}