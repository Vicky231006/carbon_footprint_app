package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group

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
import com.example.theglobalcarbonfootprintproject.calculator.InstitutionCarbonEngine
import java.util.*

@Composable
fun InstitutionHistoryScreen(
    state: HistoryUiState
) {
    val profile = state.institutionProfile ?: return
    
    val monthlyData = remember(profile) { InstitutionCarbonEngine.calculateMonthlyBreakdown(profile) }
    val deptData = remember(profile) { InstitutionCarbonEngine.calculateDepartmentBreakdown(profile) }
    val dailyTotal = remember(profile) { InstitutionCarbonEngine.calculateDailyTotal(profile) }
    
    val totalStudents = profile.studentCount + profile.staffCount
    val perCapitaDaily = if (totalStudents > 0) dailyTotal.totalKg / totalStudents else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                "Institutional Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Strategic insights for ${profile.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Intensity Cards ---
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Per-Capita",
                    value = String.format(Locale.US, "%.2f kg", perCapitaDaily),

                    icon = Icons.Default.Group,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Total",
                    value = String.format(Locale.US, "%.0f kg", dailyTotal.totalKg),

                    icon = Icons.Default.Business,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        // --- Month-on-Month Trend ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Month-on-Month Comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Projected carbon footprint across the year", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val maxVal = (monthlyData.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    val barColorPrimary = MaterialTheme.colorScheme.primary

                    val barColorOutline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    val labelColor = MaterialTheme.colorScheme.onSurface
                    
                    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        val barWidth = size.width / (12 * 1.5f)
                        val chartHeight = size.height - 40f
                        
                        for (i in 1..12) {
                            val value = monthlyData[i] ?: 0.0
                            val barHeight = (value / maxVal * chartHeight).toFloat().coerceAtLeast(4f)
                            val x = (i - 1) * (barWidth * 1.5f) + (barWidth * 0.25f)
                            
                            // Highlight current month
                            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                            val isCurrent = i == currentMonth
                            val color = if (isCurrent) barColorPrimary else barColorOutline
                            
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, chartHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                monthNames[i-1],
                                x + barWidth / 2,
                                size.height - 10f,
                                android.graphics.Paint().apply {
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    this.color = android.graphics.Color.GRAY
                                }
                            )
                        }
                    }
                }
            }
        }


        // --- Departmental Breakdown ---
        item {
            Text("Departmental Attribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val slices = deptData.toList().mapIndexed { index, (name, value) ->
                        val color = listOf(
                            Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFFBC02D), 
                            Color(0xFFE91E63), Color(0xFF7B1FA2), Color(0xFF795548)
                        )[index % 6]
                        PieSlice(name, value, color)
                    }
                    
                    val totalDept = slices.sumOf { it.value }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            var startAngle = -90f
                            slices.forEach { slice ->
                                val sweep = (slice.value / totalDept * 360f).toFloat()
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    size = Size(size.minDimension, size.minDimension)
                                )
                                startAngle += sweep
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            slices.forEach { slice ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(slice.label, style = MaterialTheme.typography.labelMedium)
                                        Text(String.format(Locale.US, "%.1f%%", (slice.value / totalDept) * 100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Strategic Forecast ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Efficiency Goal", style = MaterialTheme.typography.labelLarge)
                        Text("Targeting 5% reduction next quarter via optimization of building AC schedules.", style = MaterialTheme.typography.bodySmall)
                    }
                }

            }
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
