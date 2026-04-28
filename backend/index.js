const express = require('express');
const cors = require('cors');
const { MongoClient, ServerApiVersion } = require('mongodb');

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

const uri = "mongodb+srv://bickyboi09_db_user:Fd2XtYecyYKrzj0c@cluster0.3cm3wfw.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

// Add middleware to log absolutely every request that hits the server
app.use((req, res, next) => {
  console.log(`[INCOMING] ${req.method} ${req.url}`);
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

async function run() {
  try {
    await client.connect();
    db = client.db("global_carbon_footprint_project");
    console.log("Pinged your deployment. You successfully connected to MongoDB!");
  } catch (e) {
    console.error(e);
  }
}
run().catch(console.dir);

// Endpoints
app.post('/api/users', async (req, res) => {
  console.log("-> Received POST /api/users request!");
  try {
    const userProfile = req.body;
    if (!userProfile) return res.status(400).send("No body");
    
    // Insert into users collection
    const collection = db.collection('users');
    const result = await collection.insertOne({ ...userProfile, timestamp: new Date() });
    console.log("   Successfully inserted user:", userProfile.name);
    res.status(200).json({ success: true, result });
  } catch (e) {
    console.error("   Error inserting user:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.post('/api/institutions', async (req, res) => {
  console.log("-> Received POST /api/institutions request!");
  try {
    const instProfile = req.body;
    if (!instProfile) return res.status(400).send("No body");
    
    // Insert into institutions collection
    const collection = db.collection('institutions');
    const result = await collection.insertOne({ ...instProfile, timestamp: new Date() });
    console.log("   Successfully inserted institution:", instProfile.name);
    res.status(200).json({ success: true, result });
  } catch (e) {
    console.error("   Error inserting institution:", e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.listen(port, () => {
  console.log(`Backend API listening on port ${port}`);
});
