require('dotenv').config();
const { MongoClient } = require('mongodb');

async function seed() {
  const uri = process.env.MONGODB_URI;
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("global_carbon_footprint_project");
    const users = db.collection('users');

    const institutions = [
      { name: "IIT Bombay", state: "Maharashtra", userType: "INSTITUTION", email: "iitb@example.com" },
      { name: "Pune University", state: "Maharashtra", userType: "INSTITUTION", email: "puneuni@example.com" },
      { name: "VJTI Mumbai", state: "Maharashtra", userType: "INSTITUTION", email: "vjti@example.com" }
    ];

    for (const inst of institutions) {
      await users.updateOne(
        { email: inst.email },
        { $set: { ...inst, onboardingComplete: true, baseline_co2_daily: 200, createdAt: new Date() } },
        { upsert: true }
      );
      console.log(`Seeded institution: ${inst.name}`);
    }

  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

seed();
