package com.example.theglobalcarbonfootprintproject.ui.screens.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Restaurant

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyType
import com.example.theglobalcarbonfootprintproject.data.local.entities.MealType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogActivityScreen(
    onBack: () -> Unit,
    viewModel: LogActivityViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(Modifier.padding(16.dp)) { Text("Food") }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(Modifier.padding(16.dp)) { Text("Energy") }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // --- AI Quick Log Section ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Quick Log", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Text("Just type what you did (e.g., 'burger' or 'metro')", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var nlpInput by remember { mutableStateOf("") }
                        val nlpLoading by viewModel.nlpLoading.collectAsState()
                        val nlpResult by viewModel.nlpResult.collectAsState()
                        
                        OutlinedTextField(
                            value = nlpInput,
                            onValueChange = { nlpInput = it },
                            placeholder = { Text("Log with AI...") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (nlpLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2E7D32), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { 
                                        viewModel.processNlpInput(nlpInput)
                                        nlpInput = ""
                                    }) {
                                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send, contentDescription = "Log", tint = Color(0xFF2E7D32))
                                    }
                                }
                            },
                            singleLine = true
                        )
                        
                        nlpResult?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it.startsWith("Success")) Color(0xFF2E7D32) else Color.Red,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            LaunchedEffect(it) {
                                kotlinx.coroutines.delay(3000)
                                viewModel.clearNlpResult()
                            }
                        }
                    }
                }

                if (selectedTab == 0) {

                    FoodLogSection(onLog = { meal, desc -> 
                        viewModel.logFood(meal, desc)
                        onBack()
                    })
                } else {
                    EnergyLogSection(onLog = { type, value -> 
                        viewModel.logEnergy(type, value)
                        onBack()
                    })
                }
            }
        }
    }
}

@Composable
fun FoodLogSection(onLog: (MealType, String) -> Unit) {
    var selectedMeal by remember { mutableStateOf(MealType.VEGETARIAN) }
    var description by remember { mutableStateOf("") }

    Text("What did you eat?", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))

    MealType.values().forEach { type ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = selectedMeal == type, onClick = { selectedMeal = type })
            Text(type.name.replace("_", " "))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Short description (optional)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { onLog(selectedMeal, description) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        Text("Log Meal")
    }
}

@Composable
fun EnergyLogSection(onLog: (EnergyType, Double) -> Unit) {
    var selectedType by remember { mutableStateOf(EnergyType.ELECTRICITY) }
    var valueStr by remember { mutableStateOf("") }

    Text("Energy Usage", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))

    EnergyType.values().forEach { type ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = selectedType == type, onClick = { selectedType = type })
            Text(type.name.replace("_", " "))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = valueStr,
        onValueChange = { valueStr = it },
        label = { Text(if (selectedType == EnergyType.ELECTRICITY) "kWh" else "Units/kg") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
    )

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { 
            val value = valueStr.toDoubleOrNull() ?: 0.0
            onLog(selectedType, value) 
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        Text("Log Energy")
    }
}
