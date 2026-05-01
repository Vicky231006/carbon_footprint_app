# The Global Carbon Footprint Project 🌿

An enterprise-grade Android application designed for high-precision carbon tracking, analysis, and reduction strategies for individuals and institutions in India.

---

## 🏛 System Architecture

The application follows a robust **Offline-First, Cloud-Synced** architecture designed for high availability and low latency.

### 1. Frontend Layer (Android)
- **UI Engine**: Built entirely with **Jetpack Compose** and **Material 3**, utilizing a reactive state-driven UI.
- **Pattern**: **MVVM (Model-View-ViewModel)** ensures strict separation of concerns between business logic and UI presentation.
- **Dependency Injection**: **Hilt (Dagger)** for scalable dependency management and testing.
- **Persistence**: **Room Database** serves as the primary local storage, using multi-table relational schemas for Transport, Food, Energy, and Digital logs.

### 2. Intelligent AI & NLP Layer
- **Hybrid Parsing Engine**: A dual-tier system that prioritizes local performance.
    - **Tier 1 (Local)**: Regex-based keyword matching for instant intent recognition (O(1) complexity).
    - **Tier 2 (Cloud)**: **Gemini 1.5 Flash** fallback using Few-Shot prompting to parse complex, descriptive natural language into structured JSON payloads.
- **In-App Assistant**: Context-aware conversational AI that injects live user data into system prompts for personalized sustainability advice.

- **API**: RESTful service built with **Node.js** and **Express.js**.
- **Global Storage**: **MongoDB Atlas** for cross-device synchronization and long-term historical analytics.
- **Networking**: **Retrofit 2.0** with **OkHttp** for resilient data transfer.
- **Automatic Sync-on-Login**: A robust restoration logic that pulls all historical logs from MongoDB immediately upon authentication. This ensures 100% data continuity even after local database resets or device migrations.


### 4. Sensor & Passive Tracking Layer
- **Google Activity Recognition Transitions API**: Tracks ENTER/EXIT events for `IN_VEHICLE`, `WALKING`, and `ON_BICYCLE` to estimate travel segments without user intervention.
- **Health Connect**: Unified data interface to read real-time step counts for precise walking-distance CO₂ offset calculations.
- **Android UsageStatsManager**: Real-time monitoring of app usage and screen time for digital carbon footprinting.

---

## 📊 Scientific Calculation Models

The app uses India-specific emission factors sourced from the **Central Electricity Authority (CEA)** and **IPCC AR6** reports.

### 1. Transport CO₂ Formula
$$CO_2 (\text{kg}) = \text{Distance (km)} \times \text{Emission Factor (kg/km)}$$

**Preconsidered Factors:**
| Mode | Factor | Source |
| :--- | :--- | :--- |
| **Petrol Car** | 0.210 | IPCC AR6 (2022) |
| **Diesel Car** | 0.174 | IPCC AR6 (2022) |
| **Electric Car** | 0.053 | CEA + IPCC |
| **Two-Wheeler** | 0.065 | MoRTH India |
| **City Bus** | 0.089 | IPCC AR6 |
| **Metro / Train** | 0.041 | DMRC LCA (2020) |

### 2. Seasonal Electricity Model
$$CO_2 (\text{kg}) = \text{kWh} \times \text{State Grid Factor} \times \text{Seasonal Multiplier}$$

- **State Factor (Maharashtra)**: **0.82 kg CO₂/kWh** (CEA Baseline v17)
- **Seasonal Multipliers**:
    - **Summer (Mar-Jun)**: **1.45x** (Peak AC cooling demand)
    - **Monsoon (Jul-Sep)**: **1.10x** (Moderate usage)
    - **Winter (Oct-Feb)**: **0.75x** (Low cooling demand)

### 3. Food & Diet Model
$$CO_2 (\text{kg}) = \text{Meal Count} \times \text{Diet Intensity Factor}$$

- **Vegan**: 0.45 kg CO₂/meal
- **Vegetarian**: 0.65 kg CO₂/meal
- **Mixed (Low Meat)**: 1.10 kg CO₂/meal
- **High Meat**: 3.20 kg CO₂/meal
- **Food Delivery Penalty**: +0.30 kg CO₂ per delivery (packaging & logistics)

### 4. Digital Carbon Model
$$CO_2 (\text{kg}) = \text{Screen Time (hrs)} \times 0.036 + (\text{HD Streaming hrs} \times 1.2)$$

### 5. Institutional Module Constants
| Category | Value | Unit |
| :--- | :--- | :--- |
| **LPG Cylinder** | 42.8 | kg CO₂ per cylinder |
| **Diesel Generator** | 2.68 | kg CO₂ per litre |
| **Paper Usage** | 2.1 | kg CO₂ per A4 ream |
| **Food Waste** | 2.5 | kg CO₂ per kg waste |

---

## 📈 The Carbon Score Algorithm

The **Carbon Score (0-100)** is a normalized metric designed to gamify sustainability.

$$Score = \text{clamp}(0, 100, 100 - \lfloor(\frac{\text{Total Daily kg}}{4.1}) \times 50\rfloor)$$

- **Baseline**: India's national average is **4.1 kg CO₂/day**.
- **Metro Average**: For urban contexts, an average of **9.6 kg CO₂/day** is considered for comparison.
- **Ranking**: Users are dynamically ranked based on their rolling 7-day average to ensure long-term behavioral consistency.

---

## 🛠 Feature Deep-Dive

- **Generative AI Fallback**: Utilizes the **Gemini 1.5 Flash** model with a specific JSON schema enforcement prompt to handle complex edge cases (e.g., "I hosted a green seminar for 50 people").
- **Institutional Context**: The AI Assistant dynamically adapts its greeting and consultation logic based on the user's role, providing campus-scale metrics (solar capacity, energy baselines) for institutions.


### **2. Passive Activity Recognition & Transport Logic**
The app uses the **Google Activity Recognition Transitions API** to detect `IN_VEHICLE`, `WALKING`, and `ON_BICYCLE` states. 
- **Mode Disambiguation**: Since the API cannot distinguish between a car and a bus, the app uses a non-blocking notification to prompt the user for confirmation, enabling precise CO₂ factor correction.
- **Data Recovery**: The **History Module** features a "Reconstruction Logic" that scans raw event logs across the database to rebuild 7-day trends and pie charts even if the user failed to sync their daily summary, ensuring 100% data integrity.

### **3. Health Connect & Hardware Sensor Fallback**
The app implements a "best-available" step tracking strategy:
- **Primary**: **Google Health Connect**. The app integrates with the unified health data API to pull steps from diverse sources like Google Fit, Fitbit, and Samsung Health.
- **Fallback**: Real-time **Hardware Step Counter Sensor**. If Health Connect is unavailable (legacy devices or missing permissions), the app utilizes the device's physical step-counter sensor directly.
- **Polling Strategy (Battery Optimized)**: To minimize system pressure, the app **does not poll for steps when backgrounded**. Instead, it calculates the "step delta" only when the user opens the app or performs a log action, ensuring high accuracy with zero background battery drain.

### **4. Gamification & Eco-Reward System**
Sustainability is incentivized through a tiered reward system integrated into the global and state leaderboards:
- **Walking Milestones**:
    - 1,000 Steps: **5 Green Points**
    - 5,000 Steps: **10 Green Points**
    - 10,000 Steps: **20 Green Points** (Unlocks the "Eco-Warrior" status)
- **Points are awarded instantly** upon foreground sync, driving real-time competition.

### **5. Institutional Module (Campus-Scale Tracking)**
- **Canteen**: LPG consumption (42.8 kg/cylinder) and food waste metrics.
- **Office**: Paper usage tracking (2.1 kg per A4 ream).
- **Enrolled Community**: A specialized institutional leaderboard that compares all colleges and organizations enrolled in the platform, benchmarking them on per-capita carbon efficiency.
- **Role-Based Routing**: The app automatically routes users to either the Individual or Institutional dashboard based on their verified account type, ensuring a seamless and specialized UX.


---

## 📅 The Carbon Calendar & Heatmap
The **History Screen** features a custom-built calendar heatmap. It visualizes daily footprint intensity over 14 days, allowing users to spot high-emission patterns at a glance. Clicking any day reveals a detailed **Category Split (Pie Chart)** and a granular list of individual logs.

---

## 🏗 Engineering Hardships & Implementation Struggles

### **1. The MongoDB Sync "Nightmare"**
Our greatest challenge was establishing a stable, bi-directional sync between the Android Room DB and MongoDB Atlas. 
- **The Struggle**: Initial attempts at direct MongoDB-to-Mobile connections were insecure and prone to failure. 
- **The Solution**: We built a custom Node.js/Express middleware to serve as a security gate. We implemented a strict **Upsert Strategy** (DeviceID + Date) to resolve sync conflicts, ensuring that even if a user logs in from multiple devices, their data remains consistent.

### **2. Passive Transport Detection**
Getting the **Google Activity Recognition API** to work reliably across different manufacturers (MIUI, OxygenOS, OneUI) was a significant hurdle due to aggressive background battery optimizations. We had to implement a **Mode Disambiguation Notification** system that gently prompts the user to confirm their vehicle type (Bus vs. Car) only when a segment is detected, balancing automation with precision.

### **3. Digital Footprint Granularity**
Android's `UsageStatsManager` provides raw data that includes system background processes. To prevent "over-charging" users for carbon, we developed a logic layer to bifurcate **Active App Usage** (Foreground) from **Total Device Usage**, providing a fair and accurate digital carbon footprint.

---

## 📂 Project Structure

The project is divided into a **Multi-Module Android App** (Frontend) and a **Node.js/Express Backend**.

### **1. Android App Architecture**
*   **`com.example.theglobalcarbonfootprintproject.data`**
    *   **`local/`**: Contains `CarbonDatabase.kt` (Room builder) and `CarbonDao.kt`. Defines entities like `UserProfile`, `CarbonLog`, and `InstitutionProfile`.
    *   **`remote/`**: The cloud interface. `MongoApiService.kt` defines Retrofit endpoints, and `MongoDbManager.kt` handles the logic for syncing local data to MongoDB.
    *   **`repository/`**: `CarbonRepository.kt` acts as the **Single Source of Truth**, coordinating between Room and MongoDB.
*   **`com.example.theglobalcarbonfootprintproject.ui`**
    *   **`screens/`**: Organized by feature (e.g., `onboarding/`, `dashboard/`, `ai/`, `community/`). Each contains its own `Screen.kt` and `ViewModel.kt`.
    *   **`components/`**: Reusable UI elements like `CarbonGauge`, `NavigationRail`, and `StatCard`.
*   **`com.example.theglobalcarbonfootprintproject.utils`**
    *   `CarbonEngine.kt`: The "Brain" for individual calculations.
    *   `InstitutionCarbonEngine.kt`: Specialized logic for campus-scale footprints.
    *   `DigitalFootprintManager.kt`: Interfaces with Android's `UsageStatsManager`.

### **2. Backend Architecture (Node.js)**
*   `index.js`: The central hub. Handles JWT/Auth, MongoDB CRUD operations, and the AI Proxy.
*   `.env`: Secure storage for MongoDB URIs and Gemini API keys.
*   `debugUsers.js` / `seedLeaderboard.js`: Maintenance scripts for database integrity.

---

## 🚀 Full Project Flow & Logic Depth

### **1. User Onboarding & Secure Auth**
*   **Action**: User clicks "Finish Onboarding" in `OnboardingScreen`.
*   **Internal Flow**: 
    1.  `OnboardingViewModel.completeOnboarding()` is triggered. It collects data from the multi-step UI.
    2.  The data is passed to `CarbonRepository.saveUserProfile()`, which persists it locally in **Room SQLite**.
    3.  Immediately, `MongoDbManager.syncUserProfile()` is called via a Coroutine.
    4.  **Backend Logic**: The backend `index.js` receives the profile. If it's a new signup, it uses `bcrypt.hash()` to secure the password before inserting the record into the **MongoDB `users` collection**.
    5.  **UI Feedback**: Animations in `OnboardingScreen` (powered by `Lottie` or `Compose Animate`) trigger once the `onboardingComplete` flag in `SharedPreferences` is set to `true`.

### **2. The Daily Logging Lifecycle**
*   **Action**: User logs a transport trip (e.g., "10km by Petrol Car").
*   **Internal Flow**:
    1.  `LogViewModel.addTransportLog()` calls `CarbonEngine.calculateTransport()` located in `CarbonEngine.kt`.
    2.  The resulting `CarbonLog` object is saved to the local database via `CarbonDao.insertLog()`.
    3.  The **Observer Pattern** in `DashboardViewModel` detects the database change via a `Flow` and updates the `CarbonGauge` in real-time.
    4.  **Background Sync**: Every 6 hours (or upon manual refresh), `CarbonRepository.syncLogsToCloud()` pushes these records to the backend's `/api/sync-logs` endpoint, where they are "upserted" into MongoDB to prevent duplicates.

### **3. Institutional AI Consulting (Gemini Strategy)**
*   **Action**: An institutional admin asks: "How can I reduce campus energy costs?"
*   **Internal Flow**:
    1.  `AiAssistantViewModel` detects `user_type == "INSTITUTION"` from `SharedPreferences`.
    2.  It queries `carbonDao.getInstitutionProfile()` to get campus-specific data (Solar capacity, student count).
    3.  It constructs a **Rich Context String** (Persona) and sends it to the backend via `MongoApiService.chatWithAi()`.
    4.  **Backend Fallback Logic**: 
        - The backend `index.js` tries `gemini-2.5-flash` for speed.
        - If it hits a `503` (busy) or `429` (quota), it automatically falls back to `gemini-2.0-flash`, then `gemini-2.5-pro`, and finally `gemini-2.0-pro`.
    5.  **Output Limiting**: The backend applies a `maxOutputTokens: 500` limit to ensure the response is concise and fits perfectly in the chat bubble.

### **4. Global & Local Leaderboards**
*   **Action**: User opens the "Community" tab.
*   **Internal Flow**:
    1.  `LeaderboardViewModel` is initialized. It fetches the user's current `state` (e.g., Maharashtra) from Room.
    2.  It calls `repository.getLeaderboard(state, category)`.
    3.  **Backend Aggregation**: The backend queries the **`scores`** collection for individuals or **`institution_scores`** for organizations. 
    4.  It returns a sorted JSON list which `LeaderboardScreen.kt` renders using a `LazyColumn` with custom `FilterChips` to toggle between types.

---




