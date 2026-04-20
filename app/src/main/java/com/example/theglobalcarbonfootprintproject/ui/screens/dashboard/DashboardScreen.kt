package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToLog: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val todayLog by viewModel.todayLog.collectAsState()
    val carbonScore by viewModel.carbonScore.collectAsState()
    val transportCo2Today by viewModel.transportCo2Today.collectAsState()
    val foodCo2Today by viewModel.foodCo2Today.collectAsState()
    val energyCo2Today by viewModel.energyCo2Today.collectAsState()
    val totalCo2Today by viewModel.totalCo2Today.collectAsState()
    val stepsToday by viewModel.stepsToday.collectAsState()
    val unverifiedSegments by viewModel.unverifiedSegments.collectAsState()
    
    val animatedCarbon = remember { Animatable(0f) }

    LaunchedEffect(totalCo2Today) {
        animatedCarbon.animateTo(
            targetValue = totalCo2Today.toFloat(),
            animationSpec = tween(durationMillis = 1500)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Carbon Project") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLog,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Activity")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TODAY'S FOOTPRINT",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", animatedCarbon.value)} kg",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score: $carbonScore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (carbonScore >= 70) Color(0xFF2E7D32) else if (carbonScore >= 40) Color(0xFFF57F17) else Color(0xFFC62828)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // India Peer Comparison
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val indiaAvg = 4.1 // kg per day approx
                            val diff = totalCo2Today - indiaAvg
                            val color = if (diff <= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            val text = if (diff <= 0) 
                                "${String.format(Locale.getDefault(), "%.1f", -diff)} kg lower than India avg" 
                                else "${String.format(Locale.getDefault(), "%.1f", diff)} kg higher than India avg"
                            
                            Icon(
                                if (diff <= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Live Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                todayLog?.let { log ->
                    if (unverifiedSegments.isNotEmpty()) {
                        DisambiguationCard(unverifiedSegments.first()) { id, mode ->
                            viewModel.verifySegment(id, mode)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    DashboardCategoryRow("Transport", log.transportKg + transportCo2Today, Icons.Default.DirectionsCar)
                    DashboardCategoryRow("Steps", stepsToday.toDouble(), Icons.Default.DirectionsWalk, isSteps = true)
                    DashboardCategoryRow("Energy", log.electricityKg + energyCo2Today, Icons.Default.Bolt)
                    DashboardCategoryRow("Food", log.foodKg + foodCo2Today, Icons.Default.Restaurant)
                    DashboardCategoryRow("Digital", log.digitalKg, Icons.Default.Smartphone)
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCategoryRow(
    label: String, 
    value: Double, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSteps: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = if (isSteps) value.toInt().toString() else "${String.format(Locale.getDefault(), "%.1f", value)} kg",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
fun DisambiguationCard(
    segment: com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment,
    onVerify: (Int, com.example.theglobalcarbonfootprintproject.calculator.TransportMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm your vehicle mode", fontWeight = FontWeight.Bold)
            }
            Text(
                "We detected a ${segment.durationMinutes.toInt()} min trip. Was it:",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Car" to com.example.theglobalcarbonfootprintproject.calculator.TransportMode.CAR,
                    "Bus" to com.example.theglobalcarbonfootprintproject.calculator.TransportMode.BUS,
                    "Metro" to com.example.theglobalcarbonfootprintproject.calculator.TransportMode.METRO
                ).forEach { (label, mode) ->
                    OutlinedButton(onClick = { onVerify(segment.id, mode) }) {
                        Text(label)
                    }
                }
            }
        }
    }
}
