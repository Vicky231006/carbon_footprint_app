require('dotenv').config();
const { MongoClient, ObjectId } = require('mongodb');

async function seed() {
  const uri = process.env.MONGODB_URI;
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("global_carbon_footprint_project");
    const scores = db.collection('scores');
    const instScores = db.collection('institution_scores');
    const users = db.collection('users');

    const allUsers = await users.find({}).toArray();

    const now = new Date();
    const hour = now.getHours();
    const bucketHour = Math.floor(hour / 6) * 6;
    const bucketTimestamp = new Date(now);
    bucketTimestamp.setHours(bucketHour, 0, 0, 0);

    for (const user of allUsers) {
      if (!user.name) continue;

      let score;
      if (user.userType === "INSTITUTION") {
        if (user.name === "IIT Bombay") score = 88;
        else if (user.name === "Pune University") score = 75;
        else if (user.name === "VJTI Mumbai") score = 92;
        else score = 70;
      } else {
        if (user.name === "Vicky") score = 100;
        else if (user.name === "Anjali Joshi") score = 91;
        else if (user.name === "Aditya Kulkarni") score = 85;
        else if (user.name === "Snehal Patil") score = 72;
        else if (user.name === "Rahul Deshmukh") score = 65;
        else score = 50;
      }

      const collection = user.userType === 'INSTITUTION' ? instScores : scores;

      await collection.updateOne(
        { userId: user._id.toString(), bucketTimestamp: bucketTimestamp.toISOString() },
        { 
          $set: { 
            userId: user._id.toString(),
            name: user.name,
            state: user.state || "Maharashtra",
            score: score,
            timestamp: now,
            bucketTimestamp: bucketTimestamp.toISOString()
          } 
        },
        { upsert: true }
      );
      console.log(`Seeded score ${score} for ${user.name} (${user.userType})`);
    }

  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

seed();
