package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InstitutionDashboardContent(
    energyKg: Double,
    transportKg: Double,
    foodKg: Double,
    wasteKg: Double,
    eventKg: Double,
    studentStaffCount: Int
) {
    Column {
        Text(
            text = "Campus Resource Breakdown",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        DashboardCategoryRow("Campus Energy", energyKg, Icons.Default.Bolt)
        DashboardCategoryRow("Commuting & Fleet", transportKg, Icons.Default.DirectionsBus)
        DashboardCategoryRow("Canteen & Food", foodKg, Icons.Default.Restaurant)
        DashboardCategoryRow("Paper & Waste", wasteKg, Icons.Default.DeleteSweep)
        if (eventKg > 0) {
            DashboardCategoryRow("Annual Events (Daily)", eventKg, Icons.Default.Celebration)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Institution Stats",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Members:", style = MaterialTheme.typography.bodyMedium)
                    Text("$studentStaffCount", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                val total = energyKg + transportKg + foodKg + wasteKg + eventKg
                val perMember = if (studentStaffCount > 0) total / studentStaffCount else 0.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Per Member Daily:", style = MaterialTheme.typography.bodyMedium)
                    Text("%.2f kg".format(perMember), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}
