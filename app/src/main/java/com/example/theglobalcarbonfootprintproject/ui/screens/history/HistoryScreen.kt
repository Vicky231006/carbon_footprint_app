package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Your Carbon History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        // HEATMAP / BAR CHART REPRESENTATION
        Text("Daily Activity", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(state.dailyAggregates.reversed()) { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val height = (day.totalKg * 10).coerceIn(20.0, 100.0).dp
                    val color = if (day.totalKg < 9.58) Color(0xFF4CAF50) else Color(0xFFF44336)
                    
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(height)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dayFormat.format(Date(day.date)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Text("Recent Logs", style = MaterialTheme.typography.titleMedium) }
            
            if (state.transportHistory.isEmpty() && state.foodHistory.isEmpty() && state.energyHistory.isEmpty()) {
                item {
                    Text("No logs found. Start tracking today!", 
                         style = MaterialTheme.typography.bodyMedium, 
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(state.transportHistory.take(10)) { segment ->
                HistoryItem(
                    title = "Transport: ${segment.transportMode}",
                    value = "${String.format(Locale.getDefault(), "%.2f", segment.co2Kg)} kg",
                    date = dateFormat.format(Date(segment.date)),
                    subtitle = "${String.format(Locale.getDefault(), "%.1f", segment.estimatedKm)} km traveled"
                )
            }

            items(state.foodHistory.take(10)) { log ->
                HistoryItem(
                    title = "Food: ${log.mealType}",
                    value = "${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg",
                    date = dateFormat.format(Date(log.date)),
                    subtitle = log.description
                )
            }

            items(state.energyHistory.take(10)) { log ->
                HistoryItem(
                    title = "Energy: ${log.energyType}",
                    value = "${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg",
                    date = dateFormat.format(Date(log.date)),
                    subtitle = log.value.toString()
                )
            }

            items(state.dailyAggregates.filter { it.wasteKg > 0 || it.eventKg > 0 }.take(10)) { log ->
                if (log.wasteKg > 0) {
                    HistoryItem(
                        title = "Resource: Waste/Paper",
                        value = "${String.format(Locale.getDefault(), "%.2f", log.wasteKg)} kg",
                        date = dateFormat.format(Date(log.date)),
                        subtitle = "Institutional Daily Waste"
                    )
                }
                if (log.eventKg > 0) {
                    HistoryItem(
                        title = "Resource: Annual Events",
                        value = "${String.format(Locale.getDefault(), "%.2f", log.eventKg)} kg",
                        date = dateFormat.format(Date(log.date)),
                        subtitle = "Events Amortized Daily"
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(title: String, value: String, date: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
