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

connectToMongo();


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

    res.status(200).json({ success: true, user: userToReturn, userId: user._id });
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
    console.log("   Successfully updated user profile:", userProfile.name);
    res.status(200).json({ success: true });
  } catch (e) {
    console.error("   Error updating user:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/institutions', async (req, res) => {
  try {
    const instProfile = req.body;
    const collection = db.collection('institutions');
    const result = await collection.insertOne({ ...instProfile, timestamp: new Date() });
    res.status(200).json({ success: true, result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ success: false, error: e.message });
  }
});

// LEADERBOARD ENDPOINT
app.get('/api/leaderboard', async (req, res) => {
  try {
    const { state } = req.query;
    const query = state ? { state: state, baseline_co2_daily: { $gt: 0 } } : { baseline_co2_daily: { $gt: 0 } };
    
    const collection = db.collection('users');
    const leaderboard = await collection
      .find(query)
      .project({ name: 1, state: 1, baseline_co2_daily: 1 })
      .sort({ baseline_co2_daily: 1 }) // Lowest footprint first
      .limit(50)
      .toArray();

    res.status(200).json({ success: true, leaderboard });
  } catch (e) {
    console.error(e);
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
    const model = genAI.getGenerativeModel({ 
      model: "gemini-1.5-flash",
      systemInstruction: context
    });

    const result = await model.generateContent(message);
    const response = await result.response;
    const text = response.text();

    res.status(200).json({ success: true, reply: text });
  } catch (e) {
    console.error("   AI Error:", e);
    res.status(500).json({ success: false, error: e.message });
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

