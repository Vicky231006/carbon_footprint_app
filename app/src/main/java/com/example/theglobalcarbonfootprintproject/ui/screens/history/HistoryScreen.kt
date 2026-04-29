package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Filter logic
    val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val isSameDay = { time: Long ->
        val calLog = Calendar.getInstance().apply { timeInMillis = time }
        calSelected.get(Calendar.YEAR) == calLog.get(Calendar.YEAR) &&
        calSelected.get(Calendar.DAY_OF_YEAR) == calLog.get(Calendar.DAY_OF_YEAR)
    }

    val filteredTransport = state.transportHistory.filter { isSameDay(it.date) }
    val filteredFood = state.foodHistory.filter { isSameDay(it.date) }
    val filteredEnergy = state.energyHistory.filter { isSameDay(it.date) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Your Carbon Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        // CALENDAR SCROLL
        Text("Select Day", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(state.dailyAggregates.reversed()) { day ->
                val isSelected = isSameDay(day.date)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { selectedDate = day.date }
                        .padding(8.dp)
                ) {
                    Text(monthFormat.format(Date(day.date)), style = MaterialTheme.typography.labelSmall)
                    Text(
                        dayFormat.format(Date(day.date)), 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val color = if (day.totalKg < 10.0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Text("Logs for Selected Day", style = MaterialTheme.typography.titleMedium) }
            
            if (filteredTransport.isEmpty() && filteredFood.isEmpty() && filteredEnergy.isEmpty()) {
                item {
                    Text("No logs found for this day.", 
                         style = MaterialTheme.typography.bodyMedium, 
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(filteredTransport) { segment ->
                HistoryItem(
                    title = "Transport: ${segment.transportMode}",
                    value = "${String.format(Locale.getDefault(), "%.2f", segment.co2Kg)} kg",
                    date = dateFormat.format(Date(segment.date)),
                    subtitle = "${String.format(Locale.getDefault(), "%.1f", segment.estimatedKm)} km traveled"
                )
            }

            items(filteredFood) { log ->
                HistoryItem(
                    title = "Food: ${log.mealType}",
                    value = "${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg",
                    date = dateFormat.format(Date(log.date)),
                    subtitle = log.description
                )
            }

            items(filteredEnergy) { log ->
                HistoryItem(
                    title = "Energy: ${log.energyType}",
                    value = "${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg",
                    date = dateFormat.format(Date(log.date)),
                    subtitle = log.value.toString()
                )
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
