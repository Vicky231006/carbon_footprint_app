package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val weekdayFormat = SimpleDateFormat("EEE", Locale.getDefault())

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
    val filteredDigital = state.digitalHistory.filter { isSameDay(it.date) }

    // Compute selected-day totals for pie chart using CarbonLog (has ALL categories)
    val dayLogs = state.dailyAggregates.filter { isSameDay(it.date) }
    
    // For "Today", if the daily snapshot is 0 but we have logs, prefer the logs.
    // This handles the case where the app just opened and saved a 0-snapshot before profile loaded.
    val isToday = isSameDay(System.currentTimeMillis())
    
    val dayTransportTotal = if (dayLogs.isNotEmpty()) {
        val fromSnapshot = dayLogs.maxByOrNull { it.date }?.transportKg ?: 0.0
        if (isToday && fromSnapshot == 0.0) filteredTransport.sumOf { it.co2Kg } else fromSnapshot
    } else filteredTransport.sumOf { it.co2Kg }

    val dayFoodTotal = if (dayLogs.isNotEmpty()) {
        val fromSnapshot = dayLogs.maxByOrNull { it.date }?.foodKg ?: 0.0
        if (isToday && fromSnapshot == 0.0) filteredFood.sumOf { it.co2Kg } else fromSnapshot
    } else filteredFood.sumOf { it.co2Kg }

    val dayEnergyTotal = if (dayLogs.isNotEmpty()) {
        val fromSnapshot = dayLogs.maxByOrNull { it.date }?.electricityKg ?: 0.0
        if (isToday && fromSnapshot == 0.0) filteredEnergy.sumOf { it.co2Kg } else fromSnapshot
    } else filteredEnergy.sumOf { it.co2Kg }

    val dayDigitalTotal = if (dayLogs.isNotEmpty()) {
        val fromSnapshot = dayLogs.maxByOrNull { it.date }?.digitalKg ?: 0.0
        if (isToday && fromSnapshot == 0.0) filteredDigital.maxByOrNull { it.date }?.co2Kg ?: 0.0 else fromSnapshot
    } else filteredDigital.maxByOrNull { it.date }?.co2Kg ?: 0.0

    val dayTotal = dayTransportTotal + dayFoodTotal + dayEnergyTotal + dayDigitalTotal


    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Your Carbon Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // ─── Calendar Strip ───
        item {
            Text("Select Day", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            // Generate last 14 days
            val days = (0..13).map {
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -it) }.timeInMillis
            }.reversed()

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(days) { dayMs ->
                    val isSelected = isSameDay(dayMs)
                    val calDayLogs = state.dailyAggregates.filter {
                        val c1 = Calendar.getInstance().apply { timeInMillis = it.date }
                        val c2 = Calendar.getInstance().apply { timeInMillis = dayMs }
                        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
                    }
                    val dayTotalKg = calDayLogs.maxByOrNull { it.date }?.totalKg ?: 0.0


                    Card(
                        modifier = Modifier
                            .width(56.dp)
                            .clickable { selectedDate = dayMs },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                weekdayFormat.format(Date(dayMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                dayFormat.format(Date(dayMs)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                monthFormat.format(Date(dayMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dotColor = if (dayTotalKg <= 0.01) Color.Gray else if (dayTotalKg < 10.0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                        }
                    }
                }
            }
        }

        // ─── Pie Chart ───
        item {
            Text("Category Split", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (dayTotal > 0) {
                val slices = listOf(
                    PieSlice("Transport", dayTransportTotal, Color(0xFF1565C0)),
                    PieSlice("Food", dayFoodTotal, Color(0xFF2E7D32)),
                    PieSlice("Energy", dayEnergyTotal, Color(0xFFFBC02D)),
                    PieSlice("Digital", dayDigitalTotal, Color(0xFF7B1FA2))
                ).filter { it.value > 0 }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = (slice.value / dayTotal * 360f).toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                style = Fill,
                                size = Size(size.minDimension, size.minDimension),
                                topLeft = Offset(
                                    (size.width - size.minDimension) / 2,
                                    (size.height - size.minDimension) / 2
                                )
                            )
                            startAngle += sweep
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        slices.forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(slice.color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${slice.label}: ${String.format(Locale.US, "%.2f", slice.value)} kg", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total: ${String.format(Locale.US, "%.2f", dayTotal)} kg", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("No data for this day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ─── 7-Day Bar Chart ───
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("7-Day Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val weekData = (0..6).map { offset ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                val logsForDay = state.dailyAggregates.filter {
                    val c1 = Calendar.getInstance().apply { timeInMillis = it.date }
                    c1.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                }
                // Use the LATEST log for the day (not sum, to avoid duplicates)
                val latestLog = logsForDay.maxByOrNull { it.date }
                BarData(
                    label = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time),
                    value = latestLog?.totalKg ?: 0.0
                )
            }.reversed()


            val maxVal = (weekData.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val barWidth = size.width / (weekData.size * 2f)
                val chartHeight = size.height - 30f

                weekData.forEachIndexed { index, data ->
                    val barHeight = (data.value / maxVal * chartHeight).toFloat().coerceAtLeast(4f)
                    val x = barWidth * (index * 2 + 0.5f)
                    val barColor = if (data.value < 10.0) Color(0xFF4CAF50) else if (data.value < 15.0) Color(0xFFFFA000) else Color(0xFFE53935)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        data.label,
                        x + barWidth / 2,
                        size.height,
                        android.graphics.Paint().apply {
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = android.graphics.Color.GRAY
                        }
                    )

                    if (data.value > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f kg", data.value),
                            x + barWidth / 2,
                            chartHeight - barHeight - 8f,
                            android.graphics.Paint().apply {
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                                color = android.graphics.Color.DKGRAY
                            }
                        )

                    }
                }
            }
        }

        // ─── Log List ───
        item { Text("Logs for Selected Day", style = MaterialTheme.typography.titleMedium) }

        if (filteredTransport.isEmpty() && filteredFood.isEmpty() && filteredEnergy.isEmpty() && filteredDigital.isEmpty()) {
            item {
                Text("No activity logs found for this day.",
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

        items(filteredDigital) { log ->
            HistoryItem(
                title = "Digital Activity",
                value = "${String.format(Locale.getDefault(), "%.2f", log.co2Kg)} kg",
                date = dateFormat.format(Date(log.date)),
                subtitle = "${log.screenTimeMinutes} minutes screen time"
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

data class PieSlice(val label: String, val value: Double, val color: Color)
data class BarData(val label: String, val value: Double)

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
