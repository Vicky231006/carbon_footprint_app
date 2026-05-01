require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { MongoClient, ServerApiVersion, ObjectId } = require('mongodb');
const bcrypt = require('bcryptjs');
const { GoogleGenerativeAI } = require("@google/generative-ai");

const app = express();

const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const uri = process.env.MONGODB_URI;

const SERVER_VERSION = "v1.2-robust-db-check";

// Add middleware to log absolutely every request that hits the server
app.use((req, res, next) => {
  console.log(`[${SERVER_VERSION}] [INCOMING] ${req.method} ${req.url}`);
  res.setHeader('X-Server-Version', SERVER_VERSION);
  next();
});


const client = new MongoClient(uri, {
  serverApi: {
    version: ServerApiVersion.v1,
    strict: true,
    deprecationErrors: true,
  }
});

let db;

async function connectToMongo() {
  console.log("Attempting to connect to MongoDB Atlas...");
  try {
    await client.connect();
    db = client.db("global_carbon_footprint_project");
    console.log("✅ Successfully connected to MongoDB Atlas!");
    console.log("   Using database: global_carbon_footprint_project");
  } catch (e) {
    console.error("❌ CRITICAL: Failed to connect to MongoDB!");
    console.error(e);
    // Exit if we can't connect, as the app is useless without DB
    process.exit(1);
  }
}

// connectToMongo is now only called once inside the app.listen callback to ensure it's ready.
// connectToMongo();



// AUTH ENDPOINTS
app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, name, state } = req.body;
    console.log("-> Received POST /api/auth/register request!");
    console.log("   Payload:", { email, name, state, passwordLength: password?.length });
    
    if (!db) {
      console.error("   Error: MongoDB database not initialized yet!");
      return res.status(500).json({ success: false, error: "Database not connected. Please try again in a moment." });
    }

    if (!email || !password) {
      console.log("   Validation failed: Missing email or password");
      return res.status(400).json({ success: false, error: "Missing credentials" });
    }

    const collection = db.collection('users');
    console.log("   Checking for existing user...");
    const existingUser = await collection.findOne({ email });
    if (existingUser) {
      console.log("   User already exists:", email);
      return res.status(400).json({ success: false, error: "User already exists" });
    }

    console.log("   Hashing password...");
    const hashedPassword = await bcrypt.hash(password, 10);
    
    console.log("   Inserting new user...");
    const result = await collection.insertOne({ 
      email, 
      password: hashedPassword,
      name: name || "",
      state: state || "",
      createdAt: new Date(),
      onboardingComplete: false, // Track if the user finished onboarding
      baseline_co2_daily: 0
    });


    console.log("   Successfully inserted user:", name, "with ID:", result.insertedId);
    res.status(200).json({ success: true, userId: result.insertedId });
  } catch (e) {
    console.error("   CRITICAL Registration Error:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});


app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    console.log("-> Received POST /api/auth/login request!");
    
    const collection = db.collection('users');
    const user = await collection.findOne({ email });

    if (!user) return res.status(404).json({ success: false, error: "User not found" });

    const isValid = await bcrypt.compare(password, user.password);
    if (!isValid) return res.status(401).json({ success: false, error: "Invalid credentials" });

    // Ensure onboardingComplete is included even for legacy users
    const userToReturn = {
      ...user,
      onboardingComplete: user.onboardingComplete || false
    };

    // If the user is an institution, fetch their profile
    let institution = null;
    if (user.userType === 'INSTITUTION') {
      institution = await db.collection('institutions').findOne({ userId: user._id.toString() });
    }

    res.status(200).json({ success: true, user: userToReturn, userId: user._id, institution: institution });

  } catch (e) {
    console.error("   Login Error:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});


// PROFILE ENDPOINTS
app.post('/api/users/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const userProfile = req.body;
    
    const collection = db.collection('users');
    await collection.updateOne(
      { _id: new ObjectId(userId) },
      { $set: { ...userProfile, timestamp: new Date() } }
    );
    console.log(`   Successfully updated user profile for ${userId}:`, userProfile.name, "State:", userProfile.state);

    res.status(200).json({ success: true });
  } catch (e) {
    console.error("   Error updating user:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/institutions/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const instProfile = req.body;
    const collection = db.collection('institutions');
    
    // Also update the user document to mark as institution and completed onboarding
    await db.collection('users').updateOne(
      { _id: new ObjectId(userId) },
      { $set: { userType: 'INSTITUTION', onboardingComplete: true, name: instProfile.name } }
    );

    const result = await collection.updateOne(
      { userId: userId },
      { $set: { ...instProfile, userId, timestamp: new Date() } },
      { upsert: true }
    );
    res.status(200).json({ success: true, result });

  } catch (e) {
    console.error(e);
    res.status(500).json({ success: false, error: e.message });
  }
});

// LEADERBOARD ENDPOINT
app.get('/api/leaderboard', async (req, res) => {
  try {
    const { state, userType } = req.query;
    console.log(`[LEADERBOARD] Request received for state: ${state}, type: ${userType}`);
    
    if (!db) {
       console.log("[LEADERBOARD] DB not initialized!");
       return res.status(500).json({ success: false, error: "DB not initialized" });
    }

    // Determine which collection to use
    const collectionName = userType === 'INSTITUTION' ? 'institution_scores' : 'scores';
    const scoresCollection = db.collection(collectionName);
    
    // Aggregation to get the LATEST score for each user in the selected state
    const pipeline = [
      {
        $match: state 
          ? { state: { $regex: new RegExp(`^${state}$`, "i") } } 
          : {}
      },
      { $sort: { timestamp: -1 } }, // Sort by newest first
      {
        $group: {
          _id: "$userId",
          name: { $first: "$name" },
          state: { $first: "$state" },
          score: { $first: "$score" },
          timestamp: { $first: "$timestamp" }
        }
      },
      { $sort: { score: -1 } }, // Sort by highest score first
      { $limit: 50 }
    ];

    const leaderboard = await scoresCollection.aggregate(pipeline).toArray();

    console.log(`[LEADERBOARD] Found ${leaderboard.length} users in ${collectionName}`);

    const mappedLeaderboard = leaderboard.map(user => ({
      name: user.name || "Anonymous",
      state: user.state || "",
      baseline_co2_daily: user.score || 0
    }));

    console.log(`[LEADERBOARD] Sending results from ${collectionName}:`, JSON.stringify(mappedLeaderboard));

    res.status(200).json({ success: true, leaderboard: mappedLeaderboard });


  } catch (e) {
    console.error("[LEADERBOARD] ERROR:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});


// HISTORY & LOGS ENDPOINTS
app.post('/api/logs/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const logData = req.body;
    const collection = db.collection("daily_logs");

    console.log(`[v1.3-history] Syncing log for user: ${userId}`);

    // Update or Insert log for the specific date
    const dateStr = new Date(logData.date).toISOString().split('T')[0];

    await collection.updateOne(
      { userId, dateStr },
      { $set: { ...logData, userId, dateStr, updatedAt: new Date() } },
      { upsert: true }
    );

    // Save the latest score back to the user document for the leaderboard
    if (logData.score !== undefined) {
      const user = await db.collection('users').findOne({ _id: new ObjectId(userId) });
      const state = user ? user.state : "Unknown";
      const name = user ? user.name : "Anonymous";

      await db.collection('users').updateOne(
        { _id: new ObjectId(userId) },
        { $set: { current_score: logData.score, latest_total_kg: logData.totalKg } }
      );

      // Log to 'scores' collection with 6-hour bucket
      const now = new Date();
      const hour = now.getHours();
      const bucketHour = Math.floor(hour / 6) * 6;
      const bucketTimestamp = new Date(now);
      bucketTimestamp.setHours(bucketHour, 0, 0, 0);

      // Determine which collection to use
      const collectionName = logData.userType === 'INSTITUTION' ? 'institution_scores' : 'scores';

      await db.collection(collectionName).updateOne(
        { userId, bucketTimestamp: bucketTimestamp.toISOString() },
        { 
          $set: { 
            userId, 
            name,
            state,
            score: logData.score, 
            timestamp: now,
            bucketTimestamp: bucketTimestamp.toISOString()
          } 
        },
        { upsert: true }
      );
      console.log(`   [SCORES] Logged score ${logData.score} for ${name} in ${collectionName} (Bucket: ${bucketHour}:00)`);

    }


    res.status(200).json({ success: true });
  } catch (e) {
    console.error("   Sync Log Error:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.get('/api/logs/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const collection = db.collection("daily_logs");

    const logs = await collection.find({ userId })
      .sort({ date: -1 })
      .limit(30)
      .toArray();

    res.status(200).json({ success: true, logs });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

// AI ASSISTANT ENDPOINT
app.post('/api/ai/chat', async (req, res) => {
  try {
    const { message, context } = req.body;
    const apiKey = process.env.GEMINI_API_KEY;
    
    if (!apiKey) {
      return res.status(500).json({ success: false, error: "AI API Key missing on server" });
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const modelsToTry = ["gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.5-pro", "gemini-2.0-pro"];
    let text = "";
    let duration = 0;
    let lastError = null;

    for (const modelName of modelsToTry) {
      try {
        console.log(`   [AI] Attempting with model: ${modelName}...`);
        const model = genAI.getGenerativeModel({ 
          model: modelName,
          systemInstruction: context,
        });

        const startTime = Date.now();
        const result = await model.generateContent(message);
        const response = await result.response;
        text = response.text();
        duration = Date.now() - startTime;
        
        console.log(`   [AI] Success with ${modelName} in ${duration}ms`);
        lastError = null;
        break; // Success!
      } catch (e) {
        lastError = e;
        console.warn(`   [AI] Model ${modelName} failed: ${e.message.split('\n')[0]}`);
        
        // If it's a quota error, wait 300ms before trying the next model
        if (e.message.includes("429")) {
            await new Promise(r => setTimeout(r, 300));
        }
      }
    }


    if (lastError) throw lastError;

    res.status(200).json({ success: true, reply: text, durationMs: duration });

  } catch (e) {
    console.error("\n❌ [AI CRITICAL ERROR]");
    console.error("   Message:", e.message);
    
    let clientError = e.message;
    if (e.message.includes("429")) clientError = "AI Quota Exceeded (All models)";
    if (e.message.includes("503")) clientError = "AI Service Busy (High Demand)";
    if (e.message.includes("SAFETY")) clientError = "AI Response blocked by Safety Filters";

    res.status(500).json({ success: false, error: clientError });
  }
});




// LEGACY COMPATIBILITY ENDPOINTS (For older app versions)
app.post('/daily-log', async (req, res) => {
  console.log(`[LEGACY] Received daily-log from device: ${req.headers.deviceid}`);
  res.status(200).json({ success: true, message: "Legacy log received and acknowledged" });
});

app.post('/food-log', async (req, res) => {
  console.log(`[LEGACY] Received food-log from device: ${req.headers.deviceid}`);
  res.status(200).json({ success: true });
});

app.post('/transport-segment', async (req, res) => {
  console.log(`[LEGACY] Received transport-segment from device: ${req.headers.deviceid}`);
  res.status(200).json({ success: true });
});

app.listen(port, '0.0.0.0', () => {

  console.log(`\n=========================================`);
  console.log(`  CARBON SERVER ${SERVER_VERSION} RUNNING`);
  console.log(`  PORT: ${port}`);
  console.log(`  URL: http://192.168.1.106:${port}`);
  console.log(`  Listening on all interfaces (0.0.0.0)`);
  console.log(`=========================================\n`);
  connectToMongo();
});

