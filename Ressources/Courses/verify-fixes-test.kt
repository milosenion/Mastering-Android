package com.milosenion.avelios.star_wars.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milosenion.avelios.star_wars._core.data.service.DataStoreService
import com.ramcosta.composedestinations.annotation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Debug ViewModel
@HiltViewModel
class DebugViewModel @Inject constructor(
    val dataStoreService: DataStoreService
) : ViewModel() {
    
    private val _preferences = MutableStateFlow<Map<String, String>>(emptyMap())
    val preferences = _preferences.asStateFlow()
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()
    
    fun loadPreferences() {
        viewModelScope.launch {
            try {
                val prefs = dataStoreService.getFormattedPreferences()
                _preferences.value = prefs.associate { pref ->
                    val parts = pref.split(": ", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else pref to ""
                }
                addLog("Loaded ${prefs.size} preferences")
            } catch (e: Exception) {
                addLog("Error loading preferences: ${e.message}")
            }
        }
    }
    
    fun clearSyncTimes() {
        viewModelScope.launch {
            try {
                dataStoreService.clearSyncTimes()
                addLog("Cleared all sync times")
                loadPreferences() // Reload to show changes
            } catch (e: Exception) {
                addLog("Error clearing sync times: ${e.message}")
            }
        }
    }
    
    fun clearAllPreferences() {
        viewModelScope.launch {
            try {
                dataStoreService.clearAll()
                addLog("Cleared all preferences")
                loadPreferences()
            } catch (e: Exception) {
                addLog("Error clearing preferences: ${e.message}")
            }
        }
    }
    
    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _logs.value = _logs.value + "[$timestamp] $message"
    }
}

// Debug Screen
@Destination
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadPreferences()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Tools") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Actions
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.loadPreferences() }
                        ) {
                            Text("Refresh")
                        }
                        
                        Button(
                            onClick = { viewModel.clearSyncTimes() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Clear Sync")
                        }
                        
                        Button(
                            onClick = { viewModel.clearAllPreferences() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear All")
                        }
                    }
                }
            }
            
            // DataStore Values
            Card(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "DataStore Values",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (preferences.isEmpty()) {
                        Text(
                            "No preferences stored",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(preferences.entries.toList()) { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        key,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
            
            // Logs
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Logs",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp),
                        reverseLayout = true
                    ) {
                        items(logs.reversed()) { log ->
                            Text(
                                log,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// Verification Instructions
/*
To verify the fixes:

1. **Data Loading Issue:**
   - Open the app and navigate to any list screen
   - Check Debug Screen - should show sync times
   - Navigate between tabs - data should not reload
   - Use "Clear Sync" button and navigate back - data should reload once

2. **Navigation Crash:**
   - Click on any person/starship/planet in the list
   - Should navigate to detail screen without crashing
   - Check that the title shows the character name

3. **Favorite Toggle:**
   - Click favorite button on any list item
   - Navigate away and back - favorite state should persist
   - Click favorite button in detail screen - should update

4. **Test the Debug Screen:**
   - Add this to your bottom navigation or access via:
     navigator.navigate(DebugScreenDestination)
   
5. **Monitor Logs:**
   - Use Android Studio Logcat
   - Filter by "RemoteMediator" to see loading behavior
   - Filter by "Star Wars" for general app logs
*/