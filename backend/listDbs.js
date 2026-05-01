require('dotenv').config();
const { MongoClient } = require('mongodb');

async function listDbs() {
  const uri = process.env.MONGODB_URI;
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const admin = client.db().admin();
    const dbs = await admin.listDatabases();
    console.log("Databases:", dbs.databases.map(d => d.name));
    
    for (const dInfo of dbs.databases) {
       const db = client.db(dInfo.name);
       const collections = await db.listCollections().toArray();
       console.log(`DB: ${dInfo.name}, Collections:`, collections.map(c => c.name));
    }
  } catch (e) {
    console.error(e);
  } finally {
    await client.close();
  }
}

listDbs();
