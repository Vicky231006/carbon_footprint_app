require('dotenv').config();
const { MongoClient } = require('mongodb');

async function debug() {
  const uri = process.env.MONGODB_URI;
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("global_carbon_footprint_project");
    const users = await db.collection('users').find({}).toArray();
    
    console.log("ALL USERS IN DB:");
    users.forEach(u => {
      console.log(`- Name: ${u.name}, State: ${u.state}, Type: ${u.userType}, ID: ${u._id}`);
    });
  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

debug();
