/**
 * MainActivity.kt
 *
 * Main activity for the MAL Image Downloader app.
 * Provides navigation between different screens and initializes core services.
 *
 * Features:
 * - Jetpack Compose UI with bottom navigation
 * - Command interface screen
 * - Download progress monitoring
 * - Settings and parental controls
 * - Age verification on startup
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.osphvdhwj.malimage.safety.ParentalControlManager
import com.osphvdhwj.malimage.ui.theme.MALImageDownloaderTheme
import com.osphvdhwj.malimage.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var parentalControlManager: ParentalControlManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        parentalControlManager = ParentalControlManager(this)
        
        setContent {
            MALImageDownloaderTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()
                
                // Check age verification on startup
                LaunchedEffect(Unit) {
                    if (!parentalControlManager.isAgeVerified()) {
                        viewModel.showAgeVerification()
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        navController = navController,
                        viewModel = viewModel,
                        parentalControlManager = parentalControlManager
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    parentalControlManager: ParentalControlManager
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Age verification dialog
    if (uiState.showAgeVerification) {
        AgeVerificationDialog(
            onVerified = { viewModel.hideAgeVerification() },
            onDenied = { 
                viewModel.hideAgeVerification()
                // Could close app or restrict functionality
            },
            parentalControlManager = parentalControlManager
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAL Image Downloader") },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "command",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("command") {
                CommandScreen(
                    viewModel = viewModel,
                    parentalControlManager = parentalControlManager
                )
            }
            composable("progress") {
                DownloadProgressScreen(
                    viewModel = viewModel
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    parentalControlManager = parentalControlManager
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
            label = { Text("Commands") },
            selected = currentRoute == "command",
            onClick = {
                navController.navigate("command") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            label = { Text("Progress") },
            selected = currentRoute == "progress",
            onClick = {
                navController.navigate("progress") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}