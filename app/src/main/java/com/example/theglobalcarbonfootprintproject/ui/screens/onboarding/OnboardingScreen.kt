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

import androidx.compose.material.icons.automirrored.filled.FactCheck

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
import androidx.compose.ui.text.style.TextAlign
import com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine
import com.example.theglobalcarbonfootprintproject.calculator.LocationConstants
import com.google.gson.Gson
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
                    InstitutionInfoStep(
                        progress = progress,
                        name = onboardingData.institutionName,
                        type = onboardingData.institutionType,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { name, type ->
                            viewModel.updateData { it.copy(institutionName = name, institutionType = type) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            3 -> {
                LocationStep(
                    progress = progress,
                    state = onboardingData.state,
                    onBack = { viewModel.prevStep() },
                    onStateSelected = { state, factor ->
                        viewModel.updateData { it.copy(state = state, gridFactor = factor) }
                    },
                    onNext = { viewModel.nextStep() }
                )
            }
            4 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
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
                } else {
                    InstitutionCapacityStep(
                        progress = progress,
                        studentCount = onboardingData.studentCount,
                        staffCount = onboardingData.staffCount,
                        buildingFloors = onboardingData.buildingFloors,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { student, staff, floors ->
                            viewModel.updateData { it.copy(studentCount = student, staffCount = staff, buildingFloors = floors) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            5 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    HomeEnergyStep(
                        progress = progress,
                        monthlyKwh = onboardingData.monthlyKwhBase,
                        acSeasonality = onboardingData.acSeasonality,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { kwh, seasonality ->
                            viewModel.updateData { it.copy(monthlyKwhBase = kwh, acSeasonality = seasonality) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionClassroomLabsStep(
                        progress = progress,
                        classroomCount = onboardingData.classroomCount,
                        classroomAC = onboardingData.classroomAC,
                        labCount = onboardingData.labCount,
                        pcsPerLab = onboardingData.pcsPerLab,
                        labHours = onboardingData.labHoursPerDay,
                        hasServerRoom = onboardingData.hasServerRoom,
                        serverRoomSize = onboardingData.serverRoomSize,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { classCount, classAC, labCount, pcs, labHours, hasServer, serverSize ->
                            viewModel.updateData { it.copy(
                                classroomCount = classCount,
                                classroomAC = classAC,
                                labCount = labCount,
                                pcsPerLab = pcs,
                                labHoursPerDay = labHours,
                                hasServerRoom = hasServer,
                                serverRoomSize = serverSize
                            ) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            6 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    DietStep(
                        progress = progress,
                        dietType = onboardingData.dietType,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { diet ->
                            viewModel.updateData { it.copy(dietType = diet) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionEnergyStep(
                        progress = progress,
                        monthlyEnergyKwh = onboardingData.monthlyEnergyKwh,
                        solarCapacityKw = onboardingData.solarCapacityKw,
                        generatorDieselLitresMonth = onboardingData.generatorDieselLitresMonth,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { kwh, solar, diesel ->
                            viewModel.updateData { it.copy(
                                monthlyEnergyKwh = kwh,
                                solarCapacityKw = solar,
                                generatorDieselLitresMonth = diesel
                            ) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            7 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    DigitalStep(
                        progress = progress,
                        screenTime = onboardingData.screenTimeCategory,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { st ->
                            viewModel.updateData { it.copy(screenTimeCategory = st) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionTransportStep(
                        progress = progress,
                        studentCommuteSplit = onboardingData.studentCommuteSplit,
                        avgCommuteKm = onboardingData.avgCommuteKm,
                        institutionBusCount = onboardingData.institutionBusCount,
                        busFuelType = onboardingData.busFuelType,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { commuteSplit, avgKm, busCount, fuelType ->
                            viewModel.updateData { it.copy(
                                studentCommuteSplit = commuteSplit,
                                avgCommuteKm = avgKm,
                                institutionBusCount = busCount,
                                busFuelType = fuelType
                            ) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            8 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    PermissionsStep(
                        onBack = { viewModel.prevStep() },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionCanteenWasteStep(
                        progress = progress,
                        hasCanteen = onboardingData.hasCanteen,
                        canteenFuel = onboardingData.canteenFuel,
                        lpgCylindersMonth = onboardingData.lpgCylindersMonth,
                        dailyMealsServed = onboardingData.dailyMealsServed,
                        paperReamsMonth = onboardingData.paperReamsMonth,

                        onBack = { viewModel.prevStep() },
                        onDataChanged = { hasCanteen, fuel, lpg, meals, paper ->
                            viewModel.updateData { it.copy(
                                hasCanteen = hasCanteen,
                                canteenFuel = fuel,
                                lpgCylindersMonth = lpg,
                                dailyMealsServed = meals,
                                paperReamsMonth = paper

                            ) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            9 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    ReviewStep(
                        progress = progress,
                        data = onboardingData,
                        onBack = { viewModel.prevStep() },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionEventsStep(
                        progress = progress,
                        annualEvents = onboardingData.annualEvents,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { events ->
                            viewModel.updateData { it.copy(annualEvents = events) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            10 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    SummaryStep(
                        progress = progress,
                        viewModel = viewModel,
                        onBack = { viewModel.prevStep() },
                        onNext = { viewModel.nextStep() }
                    )
                } else {
                    InstitutionDepartmentsStep(
                        progress = progress,
                        departments = onboardingData.departments,
                        onBack = { viewModel.prevStep() },
                        onDataChanged = { depts ->
                            viewModel.updateData { it.copy(departments = depts) }
                        },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            11 -> {
                if (onboardingData.userType == UserType.INDIVIDUAL) {
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE) }
                    FinalizingStep(
                        progress = progress,
                        onComplete = {
                            viewModel.completeOnboarding(prefs) {
                                onOnboardingComplete()
                            }
                        }
                    )
                } else {
                    SummaryStep(
                        progress = progress,
                        viewModel = viewModel,
                        onBack = { viewModel.prevStep() },
                        onNext = { viewModel.nextStep() }
                    )
                }
            }
            12 -> {
                val context = LocalContext.current
                val prefs = remember { context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE) }
                FinalizingStep(
                    progress = progress,
                    onComplete = {
                        viewModel.completeOnboarding(prefs) {
                            onOnboardingComplete()
                        }
                    }
                )
            }
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onOnboardingComplete) {
                    Text("Finish")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewStep(
    progress: Float,
    data: OnboardingData,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingStepContainer(
        title = "Review your details",
        subtitle = "Please confirm your information before we create your profile.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = onNext,
        continueText = "Confirm and Continue",
        illustration = {
            Icon(Icons.AutoMirrored.Filled.FactCheck, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }

    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("User Type: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text(data.userType.name, modifier = Modifier.padding(bottom = 8.dp))

            if (data.userType == UserType.INDIVIDUAL) {
                Text("Name: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.name.ifEmpty { "N/A" }, modifier = Modifier.padding(bottom = 8.dp))

                Text("Age: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.age?.toString() ?: "N/A", modifier = Modifier.padding(bottom = 8.dp))

                Text("Location (State): ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.state.ifEmpty { "N/A" }, modifier = Modifier.padding(bottom = 8.dp))

                Text("Primary Transport: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.primaryMode.name} (${data.kmPerDay.toInt()} km/day)", modifier = Modifier.padding(bottom = 8.dp))

                Text("Home Energy: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("Avg. Monthly kWh: ${data.monthlyKwhBase.toInt()} (${data.acSeasonality.name} AC)", modifier = Modifier.padding(bottom = 8.dp))

                Text("Diet Type: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.dietType.name, modifier = Modifier.padding(bottom = 8.dp))

                Text("Screen Time: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.screenTimeCategory.name, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                Text("Institution Name: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.institutionName.ifEmpty { "N/A" }, modifier = Modifier.padding(bottom = 8.dp))

                Text("Institution Type: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.institutionType?.name ?: "N/A", modifier = Modifier.padding(bottom = 8.dp))

                Text("Student/Staff Count: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.studentCount} students, ${data.staffCount} staff", modifier = Modifier.padding(bottom = 8.dp))

                Text("Building Floors: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text(data.buildingFloors.toString(), modifier = Modifier.padding(bottom = 8.dp))

                Text("Classrooms: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.classroomCount} classrooms, AC: ${data.classroomAC.name}", modifier = Modifier.padding(bottom = 8.dp))

                Text("Labs: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.labCount} labs, ${data.pcsPerLab} PCs/lab, ${data.labHoursPerDay} hrs/day", modifier = Modifier.padding(bottom = 8.dp))

                Text("Server Room: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("Has Server: ${if (data.hasServerRoom) "Yes" else "No"}, Size: ${data.serverRoomSize?.name ?: "N/A"}", modifier = Modifier.padding(bottom = 8.dp))

                Text("Monthly Energy: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.monthlyEnergyKwh.toInt()} kWh, Solar: ${data.solarCapacityKw.toInt()} kW, Diesel: ${data.generatorDieselLitresMonth.toInt()} L/month", modifier = Modifier.padding(bottom = 8.dp))

                Text("Commute: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("Avg. ${data.avgCommuteKm.toInt()} km, Bus: ${data.institutionBusCount} (${data.busFuelType.name})", modifier = Modifier.padding(bottom = 8.dp))

                Text("Canteen: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("Has Canteen: ${if (data.hasCanteen) "Yes" else "No"}, Fuel: ${data.canteenFuel.name}, LPG: ${data.lpgCylindersMonth} cyl/month, Meals: ${data.dailyMealsServed}", modifier = Modifier.padding(bottom = 8.dp))

                Text("Waste: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("${data.paperReamsMonth} reams paper/month", modifier = Modifier.padding(bottom = 8.dp))


                if (data.annualEvents.isNotEmpty()) {
                    Text("Events: ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    data.annualEvents.forEach { event ->
                        Text("${event.name}: ${event.attendance} people, ${event.durationDays} days", modifier = Modifier.padding(bottom = 4.dp))
                    }
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
                    onStateSelected("India (Average)", 0.82)
                }
            } else {
                onError("Location not found. Using national average.")
                onStateSelected("India (Average)", 0.82)
            }
        } catch (e: Exception) {
            onError("Error: ${e.localizedMessage}. Using national average.")
            onStateSelected("India (Average)", 0.82)
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
    monthlyKwh: Double,
    acSeasonality: com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality,
    onBack: () -> Unit,
    onDataChanged: (Double, com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality) -> Unit,
    onNext: () -> Unit
) {
    var localKwh by remember { mutableStateOf(monthlyKwh.toString()) }
    var localSeasonality by remember { mutableStateOf(acSeasonality) }

    OnboardingStepContainer(
        title = "Tell us about your home energy",
        subtitle = "Electricity is a major carbon source. Estimates are fine.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(localKwh.toDoubleOrNull() ?: 150.0, localSeasonality)
            onNext()
        },
        illustration = {
            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))
        }
    ) {
        OutlinedTextField(
            value = localKwh,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) localKwh = it },
            label = { Text("Avg. monthly electricity (kWh)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("AC Usage Pattern", style = MaterialTheme.typography.labelLarge)
        
        val options = listOf(
            com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality.NONE to "No AC",
            com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality.SEASONAL to "Seasonal (Summer)",
            com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality.YEAR_ROUND to "Year Round"
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (option, label) ->
                FilterChip(
                    selected = localSeasonality == option,
                    onClick = { localSeasonality = option },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
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
    val totalBaseline = if (data.userType == UserType.INDIVIDUAL) {
        val breakdown = CarbonEngine.calculateBreakdown(data)
        breakdown.values.sum()
    } else {
        val instProfile = com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile(
            name = data.institutionName,
            type = data.institutionType?.name ?: InstitutionType.OTHER.name,
            city = data.city,
            state = data.state,
            gridFactor = data.gridFactor,
            studentCount = data.studentCount,
            staffCount = data.staffCount,
            buildingFloors = data.buildingFloors,
            classroomCount = data.classroomCount,
            classroomAC = data.classroomAC.name,
            labCount = data.labCount,
            pcsPerLab = data.pcsPerLab,
            labHoursPerDay = data.labHoursPerDay,
            hasServerRoom = data.hasServerRoom,
            serverRoomSize = data.serverRoomSize?.name,
            monthlyEnergyKwh = data.monthlyEnergyKwh,
            solarCapacityKw = data.solarCapacityKw,
            generatorDieselLitresMonth = data.generatorDieselLitresMonth,
            studentCommuteSplitJson = Gson().toJson(data.studentCommuteSplit.mapKeys { it.key.name }),
            avgCommuteKm = data.avgCommuteKm,
            institutionBusCount = data.institutionBusCount,
            busFuelType = data.busFuelType.name,
            hasCanteen = data.hasCanteen,
            canteenFuel = data.canteenFuel.name,
            lpgCylindersMonth = data.lpgCylindersMonth,
            dailyMealsServed = data.dailyMealsServed,
            paperReamsMonth = data.paperReamsMonth,

            annualEventsJson = Gson().toJson(data.annualEvents)
        )
        com.example.theglobalcarbonfootprintproject.calculator.InstitutionCarbonEngine.calculateDailyTotal(instProfile).totalKg
    }


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
                Text(
                    text = if (data.userType == UserType.INDIVIDUAL) "Your estimated daily footprint" else "Estimated daily footprint for ${data.institutionName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "~${String.format(Locale.getDefault(), "%.1f", animatedValue.value)} kg CO₂/day",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (data.userType == UserType.INDIVIDUAL) {
                    Text("India avg: ~9.58 kg/day", style = MaterialTheme.typography.bodySmall)
                } else {
                    val totalPersons = (data.studentCount + data.staffCount).toDouble()
                    val perPersonKg = if (totalPersons > 0) totalBaseline / totalPersons else 0.0
                    val score = (4100 / (perPersonKg * 10)).toInt().coerceIn(0,100) // This is a placeholder calculation from the prompt
                    Text("Per-person: ${String.format(Locale.getDefault(), "%.1f", perPersonKg)} kg · ${data.studentCount + data.staffCount} students + staff", style = MaterialTheme.typography.bodySmall)
                    Text("Score: $score", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val breakdownMap = if (data.userType == UserType.INDIVIDUAL) {
            CarbonEngine.calculateBreakdown(data)
        } else {
            val instProfile = com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile(
                name = data.institutionName,
                type = data.institutionType?.name ?: InstitutionType.OTHER.name,
                city = data.city,
                state = data.state,
                gridFactor = data.gridFactor,
                studentCount = data.studentCount,
                staffCount = data.staffCount,
                buildingFloors = data.buildingFloors,
                classroomCount = data.classroomCount,
                classroomAC = data.classroomAC.name,
                labCount = data.labCount,
                pcsPerLab = data.pcsPerLab,
                labHoursPerDay = data.labHoursPerDay,
                hasServerRoom = data.hasServerRoom,
                serverRoomSize = data.serverRoomSize?.name,
                monthlyEnergyKwh = data.monthlyEnergyKwh,
                solarCapacityKw = data.solarCapacityKw,
                generatorDieselLitresMonth = data.generatorDieselLitresMonth,
                studentCommuteSplitJson = Gson().toJson(data.studentCommuteSplit.mapKeys { it.key.name }),
                avgCommuteKm = data.avgCommuteKm,
                institutionBusCount = data.institutionBusCount,
                busFuelType = data.busFuelType.name,
                hasCanteen = data.hasCanteen,
                canteenFuel = data.canteenFuel.name,
                lpgCylindersMonth = data.lpgCylindersMonth,
                dailyMealsServed = data.dailyMealsServed,
                paperReamsMonth = data.paperReamsMonth,

                annualEventsJson = Gson().toJson(data.annualEvents)
            )
            val instResult = com.example.theglobalcarbonfootprintproject.calculator.InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            mapOf(
                "Energy" to instResult.energyKg,
                "Commuting" to instResult.transportKg,
                "Canteen + Food" to instResult.foodKg,
                "Waste" to instResult.wasteKg,
                "Events" to instResult.eventKg
            )
        }


        breakdownMap.forEach { (category, value) ->
            val icon = when (category) {
                "Transport", "Commuting" -> Icons.Default.DirectionsCar
                "Energy" -> Icons.Default.Bolt
                "Food", "Canteen + Food" -> Icons.Default.Restaurant
                "Digital" -> Icons.Default.Smartphone
                "Waste" -> Icons.Default.DeleteSweep
                "Events" -> Icons.Default.Celebration
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
                    String.format(Locale.US, "%.2f kg", value),
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { (value / totalBaseline).toFloat() },

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionInfoStep(
    progress: Float,
    name: String,
    type: InstitutionType?,
    onBack: () -> Unit,
    onDataChanged: (String, InstitutionType) -> Unit,
    onNext: () -> Unit
) {
    var localName by remember { mutableStateOf(name) }
    var localType by remember { mutableStateOf(type ?: InstitutionType.ENGINEERING_COLLEGE) }

    OnboardingStepContainer(
        title = "Tell us about your institution",
        subtitle = "We'll tailor the experience for your organization.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = { onDataChanged(localName, localType); onNext() },
        continueEnabled = localName.isNotBlank(),
        illustration = { Icon(Icons.Default.Business, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        OutlinedTextField(
            value = localName,
            onValueChange = { localName = it },
            label = { Text("Institution Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Institution Type", style = MaterialTheme.typography.labelLarge)
        
        val types = listOf(
            InstitutionType.ENGINEERING_COLLEGE to "Engineering College",
            InstitutionType.ARTS_COLLEGE to "Arts College",
            InstitutionType.SCHOOL to "School",
            InstitutionType.UNIVERSITY to "University",
            InstitutionType.OFFICE_CORPORATE to "Office/Corporate",
            InstitutionType.OTHER to "Other"
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { (t, label) ->
                FilterChip(
                    selected = localType == t,
                    onClick = { localType = t },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(range.first)) }) {
                Icon(Icons.Default.RemoveCircleOutline, null, tint = Color(0xFF2E7D32))
            }
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onValueChange((value + 1).coerceAtMost(range.last)) }) {
                Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun InstitutionCapacityStep(
    progress: Float,
    studentCount: Int,
    staffCount: Int,
    buildingFloors: Int,
    onBack: () -> Unit,
    onDataChanged: (Int, Int, Int) -> Unit,
    onNext: () -> Unit
) {
    var localStudentCount by remember { mutableIntStateOf(studentCount) }
    var localStaffCount by remember { mutableIntStateOf(staffCount) }
    var localBuildingFloors by remember { mutableIntStateOf(buildingFloors) }

    OnboardingStepContainer(
        title = "How big is your campus?",
        subtitle = "Help us scale the carbon footprint of your institution.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = { onDataChanged(localStudentCount, localStaffCount, localBuildingFloors); onNext() },
        illustration = { Icon(Icons.Default.Domain, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        Text("Approximate number of students", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = localStudentCount.toFloat(),
            onValueChange = { localStudentCount = it.toInt() },
            valueRange = 500f..10000f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("500", style = MaterialTheme.typography.bodySmall)
            Text("10k+", style = MaterialTheme.typography.bodySmall)
        }
        Text("Selected: ${localStudentCount} students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(24.dp))

        Text("Number of teaching staff", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = localStaffCount.toFloat(),
            onValueChange = { localStaffCount = it.toInt() },
            valueRange = 10f..2000f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("10", style = MaterialTheme.typography.bodySmall)
            Text("2000", style = MaterialTheme.typography.bodySmall)
        }
        Text("Selected: ${localStaffCount} staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(24.dp))

        Stepper(value = localBuildingFloors, onValueChange = { localBuildingFloors = it }, range = 1..30, label = "Number of floors in main building")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionClassroomLabsStep(
    progress: Float,
    classroomCount: Int,
    classroomAC: ClassroomAC,
    labCount: Int,
    pcsPerLab: Int,
    labHours: Double,
    hasServerRoom: Boolean,
    serverRoomSize: ServerSize?,
    onBack: () -> Unit,
    onDataChanged: (Int, ClassroomAC, Int, Int, Double, Boolean, ServerSize?) -> Unit,
    onNext: () -> Unit
) {
    var localClassroomCount by remember { mutableIntStateOf(classroomCount) }
    var localClassroomAC by remember { mutableStateOf(classroomAC) }
    var localLabCount by remember { mutableIntStateOf(labCount) }
    var localPcsPerLab by remember { mutableIntStateOf(pcsPerLab) }
    var localLabHours by remember { mutableStateOf(labHours.toFloat()) }
    var localHasServerRoom by remember { mutableStateOf(hasServerRoom) }
    var localServerRoomSize by remember { mutableStateOf(serverRoomSize) }

    OnboardingStepContainer(
        title = "Tell us about your classrooms and labs",
        subtitle = "",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(
                localClassroomCount,
                localClassroomAC,
                localLabCount,
                localPcsPerLab,
                localLabHours.toDouble(),
                localHasServerRoom,
                localServerRoomSize
            )
            onNext()
        },
        illustration = { Icon(Icons.Default.School, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        Stepper(value = localClassroomCount, onValueChange = { localClassroomCount = it }, range = 1..200, label = "Number of regular classrooms")

        Spacer(modifier = Modifier.height(16.dp))
        Text("AC in classrooms?", style = MaterialTheme.typography.labelLarge)
        val acOptions = listOf(ClassroomAC.NONE, ClassroomAC.PARTIAL, ClassroomAC.FULL)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            acOptions.forEach { ac ->
                FilterChip(
                    selected = localClassroomAC == ac,
                    onClick = { localClassroomAC = ac },
                    label = { Text(ac.name, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Stepper(value = localLabCount, onValueChange = { localLabCount = it }, range = 0..100, label = "Number of computer labs")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Average PCs per lab: ${localPcsPerLab}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = localPcsPerLab.toFloat(),
            onValueChange = { localPcsPerLab = it.toInt() },
            valueRange = 10f..40f,
            steps = 30,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Lab operating hours per day (on working days): ${localLabHours.toInt()}h", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = localLabHours,
            onValueChange = { localLabHours = it },
            valueRange = 4f..12f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = localHasServerRoom, onCheckedChange = { localHasServerRoom = it })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Do you have a server room?", style = MaterialTheme.typography.labelLarge)
        }

        if (localHasServerRoom) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Estimated Size", style = MaterialTheme.typography.labelLarge)
            val sizes = listOf(
                ServerSize.SMALL to "Small (<10 servers)",
                ServerSize.MEDIUM to "Medium",
                ServerSize.LARGE to "Large (datacenter)"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sizes.forEach { (s, label) ->
                    FilterChip(
                        selected = localServerRoomSize == s,
                        onClick = { localServerRoomSize = s },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionEnergyStep(
    progress: Float,
    monthlyEnergyKwh: Double,
    solarCapacityKw: Double,
    generatorDieselLitresMonth: Double,
    onBack: () -> Unit,
    onDataChanged: (Double, Double, Double) -> Unit,
    onNext: () -> Unit
) {
    var localMonthlyEnergyKwh by remember { mutableStateOf(monthlyEnergyKwh.toString()) }
    var localSolarCapacityKw by remember { mutableStateOf(solarCapacityKw.toString()) }
    var localGeneratorDieselLitresMonth by remember { mutableStateOf(generatorDieselLitresMonth.toString()) }
    var useKwhInput by remember { mutableStateOf(true) } // Default to kWh input

    OnboardingStepContainer(
        title = "Campus energy usage",
        subtitle = "Electricity is a major carbon source. Estimates are fine.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            val kwh = localMonthlyEnergyKwh.toDoubleOrNull() ?: 0.0
            val solar = localSolarCapacityKw.toDoubleOrNull() ?: 0.0
            val diesel = localGeneratorDieselLitresMonth.toDoubleOrNull() ?: 0.0
            onDataChanged(kwh, solar, diesel)
            onNext()
        },
        illustration = { Icon(Icons.Default.Power, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        TabRow(selectedTabIndex = if (useKwhInput) 0 else 1) {
            Tab(selected = useKwhInput, onClick = { useKwhInput = true }) {
                Text("kWh units", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = !useKwhInput, onClick = { useKwhInput = false }) {
                Text("₹ Bill", modifier = Modifier.padding(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (useKwhInput) {
            OutlinedTextField(
                value = localMonthlyEnergyKwh,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) localMonthlyEnergyKwh = it },
                label = { Text("Monthly electricity (kWh)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        } else {
            var billValue by remember { mutableStateOf("") }
            OutlinedTextField(
                value = billValue,
                onValueChange = {
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                        billValue = it
                        localMonthlyEnergyKwh = ((it.toDoubleOrNull() ?: 0.0) / 8.0).toString() // Convert bill to kWh
                    }
                },
                label = { Text("Monthly electricity bill (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
        Text(
            "This is usually on your electricity meter statement",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        var hasSolar by remember { mutableStateOf(solarCapacityKw > 0) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = hasSolar, onCheckedChange = { hasSolar = it })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Do you have solar panels on campus?", style = MaterialTheme.typography.labelLarge)
        }
        if (hasSolar) {
            OutlinedTextField(
                value = localSolarCapacityKw,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) localSolarCapacityKw = it },
                label = { Text("Approximate capacity (kW)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        var hasGenerator by remember { mutableStateOf(generatorDieselLitresMonth > 0) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = hasGenerator, onCheckedChange = { hasGenerator = it })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Do you have a diesel generator?", style = MaterialTheme.typography.labelLarge)
        }
        if (hasGenerator) {
            OutlinedTextField(
                value = localGeneratorDieselLitresMonth,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) localGeneratorDieselLitresMonth = it },
                label = { Text("Approx monthly diesel consumed (litres)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionTransportStep(
    progress: Float,
    studentCommuteSplit: Map<TransportMode, Int>,
    avgCommuteKm: Double,
    institutionBusCount: Int,
    busFuelType: FuelType,
    onBack: () -> Unit,
    onDataChanged: (Map<TransportMode, Int>, Double, Int, FuelType) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val localCommuteSplit = remember { mutableStateMapOf<TransportMode, Int>().apply { putAll(studentCommuteSplit) } }

    var localAvgCommuteKm by remember { mutableStateOf(avgCommuteKm.toFloat()) }
    var localInstitutionBusCount by remember { mutableIntStateOf(institutionBusCount) }
    var localBusFuelType by remember { mutableStateOf(busFuelType) }
    var hasInstitutionBuses by remember { mutableStateOf(institutionBusCount > 0) }

    OnboardingStepContainer(
        title = "How do students and staff commute?",
        subtitle = "",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(
                localCommuteSplit,
                localAvgCommuteKm.toDouble(),
                if (hasInstitutionBuses) localInstitutionBusCount else 0,
                localBusFuelType
            )
            onNext()
        },
        illustration = { Icon(Icons.Default.DirectionsBus, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        Text("What % of students use each transport mode?", style = MaterialTheme.typography.labelLarge)
        Text("Approximate student commute split", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val commuteModes = listOf(
            TransportMode.BUS to "College bus",
            TransportMode.PUBLIC_TRANSPORT to "Public bus / auto",
            TransportMode.TWO_WHEELER to "Two-wheeler",
            TransportMode.CAR to "Car",
            TransportMode.WALK_BIKE to "Walk / cycle"
        )

        commuteModes.forEach { (mode, label) ->
            val currentPct = localCommuteSplit[mode] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f))
                Text(currentPct.toString() + "%", modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Slider(
                    value = currentPct.toFloat(),
                    onValueChange = { localCommuteSplit[mode] = it.toInt() },

                    valueRange = 0f..100f,
                    steps = 100,
                    modifier = Modifier.weight(2f)
                )
            }
        }
        val totalPercentage = localCommuteSplit.values.sum()
        Text(
            "Total: $totalPercentage%",
            color = if (totalPercentage == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.End)
        )
        Text(
            "Rough estimates are fine — doesn't need to add up perfectly",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        Spacer(modifier = Modifier.height(16.dp))
        Text("Average one-way commute distance (km): ${localAvgCommuteKm.toInt()} km", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = localAvgCommuteKm,
            onValueChange = { localAvgCommuteKm = it },
            valueRange = 2f..50f,
            steps = 48,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = hasInstitutionBuses, onCheckedChange = { hasInstitutionBuses = it })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Does your institution run buses?", style = MaterialTheme.typography.labelLarge)
        }

        if (hasInstitutionBuses) {
            Spacer(modifier = Modifier.height(16.dp))
            Stepper(value = localInstitutionBusCount, onValueChange = { localInstitutionBusCount = it }, range = 1..50, label = "Number of buses")

            Spacer(modifier = Modifier.height(16.dp))
            Text("Fuel Type", style = MaterialTheme.typography.labelLarge)
            val fuels = listOf(FuelType.DIESEL, FuelType.CNG, FuelType.ELECTRIC, FuelType.PETROL)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                fuels.forEach { f ->
                    FilterChip(
                        selected = localBusFuelType == f,
                        onClick = { localBusFuelType = f },
                        label = { 
                            Text(
                                text = f.name.lowercase().replaceFirstChar { it.uppercase() }, 
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            ) 
                        },
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionCanteenWasteStep(
    progress: Float,
    hasCanteen: Boolean,
    canteenFuel: CanteenFuel,
    lpgCylindersMonth: Int,
    dailyMealsServed: Int,
    paperReamsMonth: Int,
    onBack: () -> Unit,
    onDataChanged: (Boolean, CanteenFuel, Int, Int, Int) -> Unit,
    onNext: () -> Unit

) {
    var localHasCanteen by remember { mutableStateOf(hasCanteen) }
    var localCanteenFuel by remember { mutableStateOf(canteenFuel) }
    var localLpgCylindersMonth by remember { mutableIntStateOf(lpgCylindersMonth) }
    var localDailyMealsServed by remember { mutableIntStateOf(dailyMealsServed) }
    var localPaperReamsMonth by remember { mutableIntStateOf(paperReamsMonth) }


    OnboardingStepContainer(
        title = "Food and waste",
        subtitle = "",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = {
            onDataChanged(
                localHasCanteen,
                localCanteenFuel,
                if (localCanteenFuel == CanteenFuel.LPG || localCanteenFuel == CanteenFuel.MIXED) localLpgCylindersMonth else 0,
                if (localHasCanteen) localDailyMealsServed else 0,
                localPaperReamsMonth

            )
            onNext()
        },
        illustration = { Icon(Icons.Default.Fastfood, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = localHasCanteen, onCheckedChange = { localHasCanteen = it })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Does your institution have a canteen?", style = MaterialTheme.typography.labelLarge)
        }

        if (localHasCanteen) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cooking fuel type", style = MaterialTheme.typography.labelLarge)
            val fuels = listOf(CanteenFuel.LPG, CanteenFuel.PNG, CanteenFuel.ELECTRIC, CanteenFuel.MIXED)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                fuels.forEach { f ->
                    FilterChip(
                        selected = localCanteenFuel == f,
                        onClick = { localCanteenFuel = f },
                        label = { 
                            Text(
                                text = f.name.lowercase().replaceFirstChar { it.uppercase() }, 
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            ) 
                        },
                    )
                }
            }


            if (localCanteenFuel == CanteenFuel.LPG || localCanteenFuel == CanteenFuel.MIXED) {
                Spacer(modifier = Modifier.height(16.dp))
                Stepper(value = localLpgCylindersMonth, onValueChange = { localLpgCylindersMonth = it }, range = 0..200, label = "Approximate LPG cylinders used per month")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Approximate daily meals served: ${localDailyMealsServed}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = localDailyMealsServed.toFloat(),
                onValueChange = { localDailyMealsServed = it.toInt() },
                valueRange = 100f..5000f,
                steps = 49,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Stepper(value = localPaperReamsMonth, onValueChange = { localPaperReamsMonth = it }, range = 0..2000, label = "Paper waste (approximate reams of paper per month)")

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionEventsStep(
    progress: Float,
    annualEvents: List<AnnualEvent>,
    onBack: () -> Unit,
    onDataChanged: (List<AnnualEvent>) -> Unit,
    onNext: () -> Unit
) {
    var localAnnualEvents by remember { mutableStateOf(annualEvents.toMutableList()) }

    OnboardingStepContainer(
        title = "Do you host large events?",
        subtitle = "Events like fests generate significant carbon from travel, power, and food.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = { onDataChanged(localAnnualEvents); onNext() },
        showSkip = true,
        onSkip = onNext,
        illustration = { Icon(Icons.Default.Celebration, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        var hostEvents by remember { mutableStateOf(localAnnualEvents.isNotEmpty()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = hostEvents, onCheckedChange = { hostEvents = it; if (!it) localAnnualEvents = mutableListOf() })
            Spacer(modifier = Modifier.width(16.dp))
            Text("We host annual fests", style = MaterialTheme.typography.labelLarge)
        }

        if (hostEvents) {
            Spacer(modifier = Modifier.height(16.dp))
            localAnnualEvents.forEachIndexed { index, event ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = event.name,
                            onValueChange = { newName ->
                                localAnnualEvents = localAnnualEvents.toMutableList().apply { this[index] = event.copy(name = newName) }
                            },
                            label = { Text("Event name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Expected attendance: ${event.attendance}", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = event.attendance.toFloat(),
                            onValueChange = { newAttendance ->
                                localAnnualEvents = localAnnualEvents.toMutableList().apply { this[index] = event.copy(attendance = newAttendance.toInt()) }
                            },
                            valueRange = 100f..10000f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Stepper(
                            value = event.durationDays,
                            onValueChange = { newDuration ->
                                localAnnualEvents = localAnnualEvents.toMutableList().apply { this[index] = event.copy(durationDays = newDuration) }
                            },
                            range = 1..5,
                            label = "Duration (days)"
                        )
                        
                        // Month Selector
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Event Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            months.forEachIndexed { mIndex, mName ->
                                FilterChip(
                                    selected = event.month == mIndex + 1,
                                    onClick = { 
                                        localAnnualEvents = localAnnualEvents.toMutableList().apply { this[index] = event.copy(month = mIndex + 1) } 
                                    },
                                    label = { Text(mName, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { localAnnualEvents = localAnnualEvents.toMutableList().apply { removeAt(index) } }) {
                            Text("Remove Event")
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                localAnnualEvents = localAnnualEvents.toMutableList().apply { add(AnnualEvent("", 100, 1)) }
            }, enabled = localAnnualEvents.size < 3) {
                Text("Add Another Event (Max 3)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionDepartmentsStep(
    progress: Float,
    departments: List<DepartmentBreakdown>,
    onBack: () -> Unit,
    onDataChanged: (List<DepartmentBreakdown>) -> Unit,
    onNext: () -> Unit
) {
    val localDepts = remember { mutableStateListOf<DepartmentBreakdown>().apply { addAll(departments) } }


    OnboardingStepContainer(
        title = "Departmental Breakdown",
        subtitle = "Break down your institution by departments to see which areas have the most impact.",
        progress = progress,
        showBack = true,
        onBack = onBack,
        onContinue = { onDataChanged(localDepts); onNext() },
        showSkip = true,
        onSkip = onNext,
        illustration = { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32)) }
    ) {
        if (localDepts.isEmpty()) {
            Text("Add your major departments (e.g., Computer Science, Mechanical, Admin, Library)", style = MaterialTheme.typography.bodyMedium)
        }

        localDepts.forEachIndexed { index, dept ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = dept.name,
                        onValueChange = { localDepts[index] = dept.copy(name = it) },

                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(String.format(Locale.US, "Energy Intensity Weight: %.1f", dept.energyWeight), style = MaterialTheme.typography.labelLarge)

                    Text("1.0 is standard (Classrooms), higher (2.0+) for Labs/Workshops", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = dept.energyWeight.toFloat(),
                        onValueChange = { localDepts[index] = dept.copy(energyWeight = it.toDouble()) },

                        valueRange = 0.5f..5.0f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(onClick = { localDepts.removeAt(index) }, modifier = Modifier.align(Alignment.End)) {

                        Text("Remove")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { localDepts.add(DepartmentBreakdown("New Dept", 100, 1.0)) },

            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("+ Add Department")
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizingStep(
    progress: Float,
    onComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    OnboardingStepContainer(
        title = "Setting up your profile",
        subtitle = "We're calculating your baseline and preparing your dashboard.",
        progress = progress,
        onContinue = {},
        continueEnabled = false,
        illustration = { 
            CircularProgressIndicator(
                color = Color(0xFF2E7D32),
                modifier = Modifier.size(60.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Finalizing...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
