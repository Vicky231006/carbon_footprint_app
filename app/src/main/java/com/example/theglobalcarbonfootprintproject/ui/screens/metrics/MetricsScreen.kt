package com.example.theglobalcarbonfootprintproject.ui.screens.metrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculation Metrics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "How we estimate your footprint",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Our calculations are based on standard emission factors tailored for the Indian context, using data from various environmental research organizations.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            MetricSection(
                title = "Transport",
                icon = Icons.Default.DirectionsCar,
                description = "Emissions are calculated per kilometer based on the fuel type and vehicle efficiency.",
                points = listOf(
                    "Petrol Car: ~210g CO₂/km",
                    "Diesel Car: ~174g CO₂/km",
                    "Electric Car: ~53g CO₂/km (Grid avg)",
                    "Bus: ~89g CO₂/km",
                    "Metro: ~41g CO₂/km",
                    "Walking/Cycling: 0g CO₂/km"
                )
            )

            MetricSection(
                title = "Energy & Electricity",
                icon = Icons.Default.Bolt,
                description = "Calculated based on kWh consumption. If you provide a bill amount, we estimate kWh using an average rate of ₹8/unit.",
                points = listOf(
                    "Grid Factor: ~0.82kg CO₂/kWh (India avg)",
                    "Solar Offset: Deducted directly from usage",
                    "AC usage: Estimated based on duration and tonnage"
                )
            )

            MetricSection(
                title = "Diet & Food",
                icon = Icons.Default.Restaurant,
                description = "Each meal's impact depends on the supply chain and production intensity.",
                points = listOf(
                    "Vegan: ~0.5kg CO₂/meal",
                    "Vegetarian: ~0.7kg CO₂/meal",
                    "Mixed Diet: ~1.2kg CO₂/meal",
                    "Meat Heavy: ~2.5kg CO₂/meal"
                )
            )

            MetricSection(
                title = "Digital Impact",
                icon = Icons.Default.Smartphone,
                description = "Includes data center energy for streaming, device charging, and manufacturing amortized over time.",
                points = listOf(
                    "Screen Time: ~36g CO₂/hour",
                    "Streaming: 20% increase over browsing",
                    "Device Standby: ~50g CO₂/device/day"
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Note: These are estimates intended to guide lifestyle changes. Actual values vary based on specific vehicle models and energy providers.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSection(
    title: String,
    icon: ImageVector,
    description: String,
    points: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            points.forEach { point ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("•", modifier = Modifier.padding(horizontal = 4.dp))
                    Text(point, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
