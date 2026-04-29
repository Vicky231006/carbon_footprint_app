package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import java.util.Locale




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToLog: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val carbonScore by viewModel.carbonScore.collectAsState()
    val transportCo2Today by viewModel.transportCo2Today.collectAsState()
    val foodCo2Today by viewModel.foodCo2Today.collectAsState()
    val energyCo2Today by viewModel.energyCo2Today.collectAsState()
    val digitalCo2Today by viewModel.digitalCo2Today.collectAsState()
    val totalCo2Today by viewModel.totalCo2Today.collectAsState()
    val stepsToday by viewModel.stepsToday.collectAsState()
    val unverifiedSegments by viewModel.unverifiedSegments.collectAsState()
    val isInstitution by viewModel.isInstitution.collectAsState()
    val wasteCo2Today by viewModel.wasteCo2Today.collectAsState()
    val eventCo2Today by viewModel.eventCo2Today.collectAsState()
    val studentStaffCount by viewModel.studentStaffCount.collectAsState()
    
    val animatedCarbon = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current


    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
    }

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
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isInstitution) {
                FloatingActionButton(
                    onClick = onNavigateToLog,
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Activity")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
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
                            val indiaAvg = 9.58 // kg per day benchmark
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
                
                if (isInstitution) {
                    InstitutionDashboardContent(
                        energyKg = energyCo2Today,
                        transportKg = transportCo2Today,
                        foodKg = foodCo2Today,
                        wasteKg = wasteCo2Today,
                        eventKg = eventCo2Today,
                        studentStaffCount = studentStaffCount
                    )
                } else {
                    Text(
                        text = "Live Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (unverifiedSegments.isNotEmpty()) {
                        DisambiguationCard(unverifiedSegments.first()) { id, mode ->
                            viewModel.verifySegment(id, mode)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val healthGranted by viewModel.healthPermissionsGranted.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (viewModel.isHealthConnectAvailable) {
                        if (!healthGranted) {
                            HealthConnectSyncCard {
                                (context as? com.example.theglobalcarbonfootprintproject.MainActivity)?.let { activity ->
                                    activity.healthConnectPermissionLauncher.launch(
                                        setOf(androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.StepsRecord::class))
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val hcSteps by viewModel.healthConnectSteps.collectAsState()
                                    Text(
                                        text = if (hcSteps != null) "Syncing with Health Connect ($hcSteps steps)" else "Syncing with Health Connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }



                    val transportKm by viewModel.transportKmToday.collectAsState()
                    DashboardTransportRow(transportCo2Today, transportKm)

                    val kmWalked = stepsToday * 0.000762
                    val co2SavedGrams = kmWalked * 210
                    DashboardStepRow(stepsToday, kmWalked, co2SavedGrams)

                    val profile by viewModel.individualProfile.collectAsState()
                    val dailyKwh = (profile?.monthlyKwhBase ?: 150.0) / 30.0
                    DashboardEnergyRow(energyCo2Today, dailyKwh)

                    DashboardCategoryRow("Food", foodCo2Today, Icons.Default.Restaurant)
                    DashboardCategoryRow("Digital", digitalCo2Today, Icons.Default.Smartphone)

                }
                
                Spacer(modifier = Modifier.height(80.dp)) // Extra space to prevent FAB overlap
            }
        }
    }
}

@Composable
fun DashboardStepRow(steps: Int, km: Double, savedGrams: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("You walked %.1f km today".format(km), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("Saved ~%.0f g CO₂ vs driving".format(savedGrams), 
                 style = MaterialTheme.typography.bodySmall, 
                 color = Color(0xFF2E7D32))
        }
        Text(
            text = "%,d steps".format(steps),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
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
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null)

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

@Composable
fun HealthConnectSyncCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.HealthAndSafety, null, tint = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sync with Health Connect", fontWeight = FontWeight.Bold)
                Text("Get accurate steps from your phone", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                Text("Enable", color = Color.White)
            }
        }
    }
}

@Composable
fun DashboardTransportRow(co2: Double, km: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.DirectionsCar,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Transport", fontWeight = FontWeight.Bold)
            if (km > 0.1) {
                Text(
                    "You traveled ${String.format(Locale.US, "%.1f", km)} km today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Estimated from your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "${String.format(Locale.US, "%.1f", co2)} kg",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
fun DashboardEnergyRow(co2: Double, kwh: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Bolt,
            null,
            tint = Color(0xFFFBC02D),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Energy", fontWeight = FontWeight.Bold)
            Text(
                "Daily usage: ${String.format(Locale.US, "%.1f", kwh)} kWh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${String.format(Locale.US, "%.1f", co2)} kg",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}
