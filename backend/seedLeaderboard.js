require('dotenv').config();
const { MongoClient } = require('mongodb');

async function seed() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error("No MONGODB_URI found in environment");
    process.exit(1);
  }

  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("global_carbon_footprint_project");
    const users = db.collection('users');

    const dummies = [
      { name: "Aditya Kulkarni", state: "Maharashtra", current_score: 85, userType: "INDIVIDUAL", onboardingComplete: true },
      { name: "Snehal Patil", state: "Maharashtra", current_score: 72, userType: "INDIVIDUAL", onboardingComplete: true },
      { name: "Rahul Deshmukh", state: "Maharashtra", current_score: 65, userType: "INDIVIDUAL", onboardingComplete: true },
      { name: "Anjali Joshi", state: "Maharashtra", current_score: 91, userType: "INDIVIDUAL", onboardingComplete: true }
    ];

    for (const d of dummies) {
      await users.updateOne(
        { name: d.name },
        { $set: d },
        { upsert: true }
      );
    }
    console.log("Successfully seeded dummy individuals!");
  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

seed();
