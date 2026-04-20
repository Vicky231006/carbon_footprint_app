package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Carbon History", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Transport History", style = MaterialTheme.typography.titleMedium) }
            items(state.transportHistory) { segment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${segment.transportMode}: ${String.format(Locale.getDefault(), "%.2f", segment.estimatedKm)} km")
                        Text("CO2: ${String.format(Locale.getDefault(), "%.2f", segment.co2Kg)} kg", color = MaterialTheme.colorScheme.primary)
                        Text(dateFormat.format(Date(segment.date)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Text("Food Logs", style = MaterialTheme.typography.titleMedium) }
            items(state.foodHistory) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${log.mealType}: ${log.description}")
                        Text("CO2: ${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg", color = MaterialTheme.colorScheme.primary)
                        Text(dateFormat.format(Date(log.date)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Text("Energy Logs", style = MaterialTheme.typography.titleMedium) }
            items(state.energyHistory) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${log.energyType}: ${log.value}")
                        Text("CO2: ${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg", color = MaterialTheme.colorScheme.primary)
                        Text(dateFormat.format(Date(log.date)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
