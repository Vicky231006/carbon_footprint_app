package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val onboardingData by viewModel.data.collectAsState()
    val progress by remember { derivedStateOf { viewModel.progressFraction } }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "onboarding_transition"
    ) { step ->
        when (step) {
            0 -> WelcomeStep(
                progress = progress,
                onNext = { viewModel.nextStep() }
            )
            1 -> UserTypeStep(
                progress = progress,
                selectedType = onboardingData.userType,
                onTypeSelected = { viewModel.updateData { data -> data.copy(userType = it) } },
                onNext = { viewModel.nextStep() }
            )
            2 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    BasicInfoStep(
                        progress = progress,
                        name = onboardingData.name,
                        age = onboardingData.age,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { name, age ->
                            viewModel.updateData { it.copy(name = name, age = age) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    // Institution branch Placeholder
                    InstitutionInfoStep(
                        progress = progress,
                        onBack = { viewModel.prevStep() },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            3 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    LocationStep(
                        progress = progress,
                        state = onboardingData.state,
                        onBack = { viewModel.prevStep() },
                        onStateSelected = { state, factor ->
                            viewModel.updateData { it.copy(state = state, gridFactor = factor) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    Text("Institution Steps Placeholder")
                }
            }
            4 -> {
                TransportStep(
                    progress = progress,
                    primaryMode = onboardingData.primaryMode,
                    kmPerDay = onboardingData.kmPerDay,
                    onBack = { viewModel.prevStep() },
                    onDataChanged = { mode, km ->
                        viewModel.updateData { it.copy(primaryMode = mode, kmPerDay = km) }
                    },
                    onNext = { viewModel.nextStep() }
                )
            }
            5 -> {
                HomeEnergyStep(
                    progress = progress,
                    acUsage = onboardingData.acUsage,
                    onBack = { viewModel.prevStep() },
                    onDataChanged = { ac ->
                        viewModel.updateData { it.copy(acUsage = ac) }
                    },
                    onNext = { viewModel.nextStep() }
                )
            }
            6 -> {
                DietStep(
                    progress = progress,
                    dietType = onboardingData.dietType,
                    onBack = { viewModel.prevStep() },
                    onDataChanged = { diet ->
                        viewModel.updateData { it.copy(dietType = diet) }
                    },
                    onNext = { viewModel.nextStep() }
                )
            }
            7 -> {
                DigitalStep(
                    progress = progress,
                    screenTime = onboardingData.screenTimeCategory,
                    onBack = { viewModel.prevStep() },
                    onDataChanged = { st ->
                        viewModel.updateData { it.copy(screenTimeCategory = st) }
                    },
                    onNext = { viewModel.nextStep() }
                )
            }
            8 -> {
                SummaryStep(
                    progress = progress,
                    viewModel = viewModel,
                    onBack = { viewModel.prevStep() },
                    onNext = { viewModel.nextStep() }
                )
            }
            9 -> {
                val context = LocalContext.current
                val prefs = remember { context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE) }
                PermissionsStep(
                    onBack = { viewModel.prevStep() },
                    onNext = {
                        viewModel.saveToPreferences(prefs)
                        onOnboardingComplete()
                    }
                )
            }
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onOnboardingComplete) {
                    Text("Finish (Integration in Progress)")
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(progress: Float, onNext: () -> Unit) {
    OnboardingStepContainer(
        title = "Let's understand\nyour lifestyle",
        subtitle = "This takes about 60 seconds. We'll use your answers to build your personal carbon baseline.",
        progress = progress,
        onContinue = onNext,
        continueText = "Let's go →",
        illustration = {
            Text("🌱", style = MaterialTheme.typography.displayLarge)
        }
    ) {
        // No extra content
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTypeStep(
    progress: Float,
    selectedType: UserType,
    onTypeSelected: (UserType) -> Unit,
    onNext: () -> Unit
) {
    var localSelected by remember { mutableStateOf(selectedType) }

    OnboardingStepContainer(
        title = "Who's tracking carbon?",
        subtitle = "Pick what fits you best.",
        progress = progress,
        onContinue = {
            onTypeSelected(localSelected)
            onNext()
        },
        illustration = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp))
                Icon(Icons.Default.Business, null, modifier = Modifier.size(48.dp))
            }
        }
    ) {
        OnboardingOptionCard(
            title = "For me personally",
            subtitle = "Track your daily lifestyle footprint",
            icon = Icons.Default.Person,
            selected = localSelected == UserType.INDIVIDUAL,
            onClick = { localSelected = UserType.INDIVIDUAL }
        )
        OnboardingOptionCard(
            title = "For my institution",
            subtitle = "Track your campus, office, or college",
            icon = Icons.Default.Business,
            selected = localSelected == UserType.INSTITUTION,
            onClick = { localSelected = UserType.INSTITUTION }
        )
    }
}

@Composable
fun BasicInfoStep(
    progress: Float,
    name: String,
    age: Int?,
    onBack: () -> Unit,
    onDataChanged: (String, Int?) -> Unit,
    onNext: () -> Unit
) {
    var localName by remember { mutableStateOf(name) }
    var localAge by remember { mutableStateOf(age?.toString() ?: "") }

    OnboardingStepContainer(
        title = "What should we call you?",
        subtitle = "Just a first name is fine.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(localName, localAge.toIntOrNull())
            onNext()
        },
        continueEnabled = localName.isNotBlank(),
        continueText = if (localName.isNotBlank()) "Hello, $localName →" else "Continue",
        illustration = {
            Icon(Icons.Default.WavingHand, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        OutlinedTextField(
            value = localName,
            onValueChange = { localName = it },
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = localAge,
            onValueChange = { if (it.all { char -> char.isDigit() }) localAge = it },
            label = { Text("Age (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(
            "We use this to compare with your age group's average.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationStep(
 progress: Float,
    state: String,
    onBack: () -> Unit,
    onStateSelected: (String, Double) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var detectedState by remember { mutableStateOf(state) }
    var isDetecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            isDetecting = true
        } else {
            error = "Permission denied. Using national average."
            onStateSelected("India (Average)", 0.82)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun detectLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        onStateDetected: (String, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                onError("Permissions not granted")
                onStateDetected("India (Average)", 0.82)
                return
            }

            val location = withContext(Dispatchers.IO) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                } catch (e: Exception) {
                    null
                }
            }
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Using a safer synchronous call for simplicity within withContext(Dispatchers.IO)
                            // Even on Tiramisu, geocoder.getFromLocation(lat, long, max) still exists but is deprecated.
                            // However, the callback version is preferred.
                            // To keep it simple and avoid callback hell in suspend, we'll stick to the sync one
                            // which is safe inside Dispatchers.IO.
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        } else {
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                val stateName = addresses?.firstOrNull()?.adminArea
                if (stateName != null) {
                    val factor = LocationConstants.INDIA_GRID_FACTORS.getOrElse(stateName) { 0.82 }
                    onStateDetected(stateName, factor)
                } else {
                    onError("Could not determine state. Using national average.")
                    onStateDetected("India (Average)", 0.82)
                }
            } else {
                onError("Location not found. Using national average.")
                onStateDetected("India (Average)", 0.82)
            }
        } catch (e: Exception) {
            onError("Error: ${e.localizedMessage}. Using national average.")
            onStateDetected("India (Average)", 0.82)
        }
    }

    LaunchedEffect(isDetecting) {
        if (isDetecting) {
            detectLocation(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onStateDetected = { s, f ->
                    detectedState = s
                    onStateSelected(s, f)
                },
                onError = { error = it }
            )
            isDetecting = false
        }
    }

    OnboardingStepContainer(
        title = "Where are you located?",
        subtitle = "We use this to find your state's electricity grid factor — it affects your emissions calculation.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = onNext,
        continueEnabled = !isDetecting,
        illustration = {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = null
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (detectedState.isEmpty()) "Detect my location" else "Detected: $detectedState",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (error != null) {
                        Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (detectedState.isNotEmpty()) {
                        val factor = LocationConstants.INDIA_GRID_FACTORS.getOrElse(detectedState) { 0.82 }
                        Text("Grid Factor: $factor kg CO₂/kWh", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (detectedState.isEmpty() && !isDetecting) {
            Text(
                "Tapping auto-detect will request location permissions.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportStep(
    progress: Float,
    primaryMode: TransportMode,
    kmPerDay: Double,
    onBack: () -> Unit,
    onDataChanged: (TransportMode, Double) -> Unit,
    onNext: () -> Unit
) {
    var localMode by remember { mutableStateOf(primaryMode) }
    var localKm by remember { mutableStateOf(kmPerDay.toFloat()) }

    OnboardingStepContainer(
        title = "How do you usually get around?",
        subtitle = "Pick your main mode — you can log specifics later.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(localMode, localKm.toDouble())
            onNext()
        },
        illustration = {
            Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        val modes = listOf(
            TransportMode.WALK_BIKE to "Walk/Bike",
            TransportMode.METRO to "Metro",
            TransportMode.BUS to "Bus",
            TransportMode.TWO_WHEELER to "2-Wheeler",
            TransportMode.CAR to "Car",
            TransportMode.MIXED to "Mixed"
        )

        val rows = ceil(modes.size / 2f).toInt()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height((rows * 58).dp)
        ) {
            items(modes) { (mode, label) ->
                FilterChip(
                    selected = localMode == mode,
                    onClick = { localMode = mode },
                    label = { Text(label) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Roughly how far do you travel daily? ~${localKm.toInt()} km/day")
        Slider(
            value = localKm,
            onValueChange = { localKm = it },
            valueRange = 0f..80f,
            steps = 80
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEnergyStep(
    progress: Float,
    acUsage: ACUsage,
    onBack: () -> Unit,
    onDataChanged: (ACUsage) -> Unit,
    onNext: () -> Unit
) {
    var localAc by remember { mutableStateOf(acUsage) }

    OnboardingStepContainer(
        title = "Tell us about your home energy",
        subtitle = "Electricity and cooking are usually the biggest sources of household carbon.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(localAc)
            onNext()
        },
        illustration = {
            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        Text("Do you use air conditioning?", style = MaterialTheme.typography.labelLarge)
        val options = listOf(
            ACUsage.NONE to "No AC",
            ACUsage.OCCASIONAL to "Sometimes",
            ACUsage.DAILY to "Daily"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (usage, label) ->
                FilterChip(
                    selected = localAc == usage,
                    onClick = { localAc = usage },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
        }
    }
}

@Composable
fun DietStep(
    progress: Float,
    dietType: DietType,
    onBack: () -> Unit,
    onDataChanged: (DietType) -> Unit,
    onNext: () -> Unit
) {
    var localDiet by remember { mutableStateOf(dietType) }

    OnboardingStepContainer(
        title = "What does your plate usually look like?",
        subtitle = "Food is one of the biggest carbon contributors.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(localDiet)
            onNext()
        },
        illustration = {
            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        val diets = listOf(
            DietType.VEGAN to ("Vegan" to "No animal products"),
            DietType.VEGETARIAN to ("Vegetarian" to "Dairy and eggs fine"),
            DietType.MIXED to ("Mixed" to "Meat occasionally"),
            DietType.MEAT_HEAVY to ("Meat-heavy" to "Regular meat/fish")
        )

        diets.forEach { (type, info) ->
            OnboardingOptionCard(
                title = info.first,
                subtitle = info.second,
                icon = Icons.Default.Restaurant,
                selected = localDiet == type,
                onClick = { localDiet = type }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalStep(
    progress: Float,
    screenTime: ScreenTime,
    onBack: () -> Unit,
    onDataChanged: (ScreenTime) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var detectedMinutes by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAverageScreenTime(): Long? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7 * 24 * 60 * 60 * 1000) // 7 days ago
        
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        if (stats.isNullOrEmpty()) return null

        val totalTime = stats.values.sumOf { it.totalTimeInForeground }
        return totalTime / (7 * 60 * 1000) // Average minutes per day over 7 days
    }

    LaunchedEffect(isChecking) {
        if (isChecking) {
            if (hasUsageStatsPermission()) {
                val avg = getAverageScreenTime()
                if (avg != null) {
                    detectedMinutes = avg
                    val category = when {
                        avg < 120 -> ScreenTime.LIGHT
                        avg < 300 -> ScreenTime.MODERATE
                        else -> ScreenTime.HEAVY
                    }
                    onDataChanged(category)
                } else {
                    error = "Could not retrieve stats. Using default."
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            isChecking = false
        }
    }

    OnboardingStepContainer(
        title = "How much do you use your devices?",
        subtitle = "We'll read your actual screen time from your phone — no guessing needed.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = onNext,
        illustration = {
            Icon(Icons.Default.Smartphone, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { isChecking = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (detectedMinutes != null) Icons.Default.CheckCircle else Icons.Default.Timer,
                    null,
                    tint = if (detectedMinutes != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (detectedMinutes == null) "Auto-detect screen time" else "Detected: ${detectedMinutes!! / 60}h ${detectedMinutes!! % 60}m avg.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (detectedMinutes == null) "Requires usage access permission" else "Category: ${screenTime.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Or select manually:", style = MaterialTheme.typography.labelLarge)
        val options = listOf(
            ScreenTime.LIGHT to "Light (< 2h)",
            ScreenTime.MODERATE to "Moderate (2-5h)",
            ScreenTime.HEAVY to "Heavy (5h+)"
        )
        options.forEach { (cat, label) ->
            OnboardingOptionCard(
                title = label,
                subtitle = "",
                icon = Icons.Default.PhoneAndroid,
                selected = screenTime == cat,
                onClick = { onDataChanged(cat) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryStep(
    progress: Float,
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val breakdown = remember(data) { CarbonEngine.calculateBreakdown(data) }
    val totalBaseline = remember(breakdown) { breakdown.values.sum() }

    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(totalBaseline) {
        animatedValue.animateTo(
            targetValue = totalBaseline.toFloat(),
            animationSpec = tween(durationMillis = 2000)
        )
    }

    OnboardingStepContainer(
        title = "Here's your carbon baseline",
        subtitle = "Based on your lifestyle, we estimate:",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = onNext,
        continueText = "Looks good — let's go",
        illustration = {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your estimated daily footprint", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "~${String.format(Locale.getDefault(), "%.1f", animatedValue.value)} kg CO₂/day",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("India avg: ~4.1 kg/day", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        breakdown.forEach { (category, value) ->
            val icon = when (category) {
                "Transport" -> Icons.Default.DirectionsCar
                "Energy" -> Icons.Default.Bolt
                "Food" -> Icons.Default.Restaurant
                "Digital" -> Icons.Default.Smartphone
                else -> Icons.Default.Category
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(16.dp))
                Text(category, modifier = Modifier.weight(1f))
                Text(
                    String.format(Locale.getDefault(), "%.2f kg", value),
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = (value / totalBaseline).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF2E7D32),
                trackColor = Color(0xFFC8E6C9)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current

    val permissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION to ("Location" to "Track your carbon footprint automatically based on your movement.")
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS to ("Notifications" to "Get daily carbon alerts and milestone reminders."))
            }
        }
    }

    var currentPermIndex by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (currentPermIndex < permissions.size - 1) {
            currentPermIndex++
        } else {
            onNext()
        }
    }

    OnboardingStepContainer(
        title = "One more thing",
        subtitle = "These help us track your footprint automatically.",
        progress = 1f,
        showBack = true,
        onBack = onBack,
        onContinue = {
            if (currentPermIndex < permissions.size) {
                val perm = permissions[currentPermIndex].first
                if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                    if (currentPermIndex < permissions.size - 1) {
                        currentPermIndex++
                    } else {
                        onNext()
                    }
                } else {
                    launcher.launch(perm)
                }
            } else {
                onNext()
            }
        },
        continueText = if (currentPermIndex < permissions.size) "Grant ${permissions[currentPermIndex].second.first} →" else "Finish →",
        illustration = {
            Icon(Icons.Default.Security, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        permissions.forEachIndexed { index, (perm, info) ->
            val isGranted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            val isCurrent = index == currentPermIndex

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isGranted) Icons.Default.CheckCircle else Icons.Default.Circle,
                        null,
                        tint = if (isGranted) Color(0xFF2E7D32) else if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(info.first, fontWeight = FontWeight.Bold)
                        Text(info.second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun InstitutionInfoStep(progress: Float, onBack: () -> Unit, onNext: () -> Unit) {
    OnboardingStepContainer(
        title = "Tell us about your institution",
        subtitle = "We'll tailor the experience for your organization.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = onNext,
        illustration = { Icon(Icons.Default.Business, null, modifier = Modifier.size(80.dp)) }
    ) {
        // Placeholder for institution fields
        Text("Institution fields coming soon...")
    }
}
