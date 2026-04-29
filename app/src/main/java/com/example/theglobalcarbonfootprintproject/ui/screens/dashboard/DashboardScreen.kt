package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                title = { Text("GreenView") },

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
                            if (isInstitution) {
                                val perCapita = if (studentStaffCount > 0) totalCo2Today / studentStaffCount else 0.0
                                val benchmark = 0.88 // kg per day per person benchmark for Indian institutions
                                val diff = perCapita - benchmark
                                val color = if (diff <= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (diff <= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            null,
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (diff <= 0) "${String.format(Locale.getDefault(), "%.2f", -diff)} kg lower than benchmark"
                                            else "${String.format(Locale.getDefault(), "%.2f", diff)} kg higher than benchmark",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = color
                                        )
                                    }
                                    Text(
                                        "Per Capita: ${String.format(Locale.getDefault(), "%.2f", perCapita)} kg/day (Benchmark: $benchmark kg)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Source: Academic research on Indian campuses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                val indiaAvg = 9.58 // kg per day benchmark for metro city individual
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
                }

                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isInstitution) {
                    val instProfile by viewModel.institutionProfile.collectAsState()
                    InstitutionDashboardContent(
                        profile = instProfile,
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



                    var expandedCategory by remember { mutableStateOf<String?>(null) }

                    val transportKm by viewModel.transportKmToday.collectAsState()
                    val profile by viewModel.individualProfile.collectAsState()

                    // ─── Transport Row ───
                    DashboardTransportRow(transportCo2Today, transportKm,
                        expanded = expandedCategory == "transport",
                        onClick = { expandedCategory = if (expandedCategory == "transport") null else "transport" }
                    )
                    AnimatedVisibility(
                        visible = expandedCategory == "transport",
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        val mode = profile?.travelMode ?: "CAR"
                        val km = profile?.kmPerDay ?: 10.0
                        val factor = when (mode.uppercase()) {
                            "WALK_BIKE" -> 0.0; "METRO" -> 0.041; "BUS" -> 0.089
                            "TWO_WHEELER" -> 0.065; "CAR" -> 0.210; "MIXED" -> 0.12
                            else -> 0.21
                        }
                        BreakdownCard(
                            title = "How we calculated Transport",
                            lines = if (transportKm > 0.1) {
                                listOf(
                                    "Source: Auto-detected travel today",
                                    "Distance tracked: ${String.format(Locale.US, "%.1f", transportKm)} km",
                                    "Total CO₂: ${String.format(Locale.US, "%.1f", transportCo2Today)} kg"
                                )
                            } else {
                                listOf(
                                    "Source: Estimated from your profile",
                                    "Your mode: $mode | Distance: ${String.format(Locale.US, "%.1f", km)} km/day",
                                    "Emission factor: $factor kg CO₂ per km",
                                    "${String.format(Locale.US, "%.1f", km)} km × $factor = ${String.format(Locale.US, "%.2f", transportCo2Today)} kg"
                                )
                            }
                        )
                    }

                    // ─── Steps Row ───
                    val kmWalked = stepsToday * 0.000762
                    val co2SavedGrams = kmWalked * 210
                    DashboardStepRow(stepsToday, kmWalked, co2SavedGrams)

                    // ─── Energy Row ───
                    val monthlyKwh = profile?.monthlyKwhBase ?: 150.0
                    val gridFactor = profile?.gridFactor ?: 0.82
                    val dailyKwh = monthlyKwh / 30.0
                    DashboardEnergyRow(energyCo2Today, dailyKwh,
                        expanded = expandedCategory == "energy",
                        onClick = { expandedCategory = if (expandedCategory == "energy") null else "energy" }
                    )
                    AnimatedVisibility(
                        visible = expandedCategory == "energy",
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        val season = com.example.theglobalcarbonfootprintproject.calculator.SeasonalElectricityCalculator.getSeasonLabel()
                        val loggedEnergy by viewModel.loggedEnergyToday.collectAsState()
                        BreakdownCard(
                            title = "How we calculated Energy",
                            lines = listOf(
                                "Source: Profile estimate + Manual logs",
                                "Monthly usage: ${String.format(Locale.US, "%.0f", monthlyKwh)} kWh",
                                "Daily usage: ${String.format(Locale.US, "%.1f", dailyKwh)} kWh ($season)",
                                "Grid emission factor: $gridFactor kg CO₂/kWh",
                                "Profile Estimate: ${String.format(Locale.US, "%.2f", energyCo2Today - loggedEnergy)} kg CO₂",
                                if (loggedEnergy > 0) "Manual logs today: ${String.format(Locale.US, "%.2f", loggedEnergy)} kg CO₂" else "",
                                "Total Result: ${String.format(Locale.US, "%.2f", energyCo2Today)} kg CO₂"
                            ).filter { it.isNotEmpty() }
                        )


                    }

                    // ─── Food Row ───
                    DashboardCategoryRow("Food", foodCo2Today, Icons.Default.Restaurant,
                        expanded = expandedCategory == "food",
                        onClick = { expandedCategory = if (expandedCategory == "food") null else "food" }
                    )
                    AnimatedVisibility(
                        visible = expandedCategory == "food",
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        val diet = profile?.dietType ?: "MIXED"
                        val meals = profile?.mealsPerDay ?: 3
                        val profileEstimate = when (diet.uppercase()) {
                            "VEGAN" -> 0.50; "VEGETARIAN" -> 0.70; "MIXED" -> 1.20; "MEAT_HEAVY" -> 2.50; else -> 1.20
                        } * meals
                        val isLogged = Math.abs(foodCo2Today - profileEstimate) > 0.01 && foodCo2Today > 0
                        BreakdownCard(
                            title = "How we calculated Food",
                            lines = if (isLogged) {
                                listOf(
                                    "Source: Your logged meals today",
                                    "Total from food logs: ${String.format(Locale.US, "%.2f", foodCo2Today)} kg CO₂"
                                )
                            } else {
                                listOf(
                                    "Source: Estimated from your diet profile",
                                    "Diet type: $diet | Meals/day: $meals",
                                    "Estimated: ${String.format(Locale.US, "%.2f", foodCo2Today)} kg CO₂"
                                )
                            }
                        )
                    }

                    // ─── Digital Row ───
                    DashboardCategoryRow("Digital", digitalCo2Today, Icons.Default.Smartphone,
                        expanded = expandedCategory == "digital",
                        onClick = { expandedCategory = if (expandedCategory == "digital") null else "digital" }
                    )
                    AnimatedVisibility(
                        visible = expandedCategory == "digital",
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        BreakdownCard(
                            title = "How we calculated Digital",
                            lines = listOf(
                                "Source: Actual screen time tracked by your phone",
                                "We measure your total app usage today",
                                "Rate: ~0.036 kg CO₂ per hour of screen time",
                                "Your total today: ${String.format(Locale.US, "%.1f", digitalCo2Today)} kg CO₂"
                            )
                        )
                    }



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
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = "${String.format(Locale.getDefault(), "%.1f", value)} kg",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
fun DashboardTransportRow(co2: Double, km: Double, expanded: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Transport", fontWeight = FontWeight.Bold)
            Text(
                if (km > 0.1) "You traveled ${String.format(Locale.US, "%.1f", km)} km today" else "Estimated from your profile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${String.format(Locale.US, "%.1f", co2)} kg", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
fun DashboardEnergyRow(co2: Double, kwh: Double, expanded: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bolt, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Energy", fontWeight = FontWeight.Bold)
            Text("Daily usage: ${String.format(Locale.US, "%.1f", kwh)} kWh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${String.format(Locale.US, "%.1f", co2)} kg", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
fun BreakdownCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            lines.forEach { line ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
