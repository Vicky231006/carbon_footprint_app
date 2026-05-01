require('dotenv').config();
const { MongoClient } = require('mongodb');

async function testLeaderboardQuery() {
  const uri = process.env.MONGODB_URI;
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("global_carbon_footprint_project");
    const state = "maharashtra"; // Simulate the app's request
    
    console.log(`[LEADERBOARD] Fetching for state: ${state}`);
    const query = state 
      ? { state: { $regex: new RegExp(`^${state}$`, "i") }, current_score: { $gt: 0 } } 
      : { current_score: { $gt: 0 } };
      
    console.log("Query object:", query);
    
    const collection = db.collection('users');
    const leaderboard = await collection
      .find(query)
      .project({ name: 1, state: 1, current_score: 1 })
      .sort({ current_score: -1 })
      .limit(50)
      .toArray();

    console.log("Raw Leaderboard results from DB:", leaderboard);

    const mappedLeaderboard = leaderboard.map(user => ({
      name: user.name,
      state: user.state,
      baseline_co2_daily: user.current_score
    }));

    console.log("Mapped response to be sent to app:", mappedLeaderboard);
  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

testLeaderboardQuery();
