package com.example.theglobalcarbonfootprintproject.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Community Leaderboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == "INDIVIDUAL",
                onClick = { viewModel.setCategory("INDIVIDUAL") },
                label = { Text("Individuals") }
            )
            FilterChip(
                selected = selectedCategory == "INSTITUTION",
                onClick = { viewModel.setCategory("INSTITUTION") },
                label = { Text("Institutions") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.users) { user ->
                LeaderboardItem(user)
            }
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardEntry) {
    val isUser = user.username == "You"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isUser) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                 else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${user.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(50.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${user.points} pts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
