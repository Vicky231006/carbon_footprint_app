const { MongoClient } = require('mongodb');

// Trying SRV string format to bypass port 27017 blocking
const uri = "mongodb+srv://bickyboi09_db_user:Fd2XtYecyYKrzj0c@cluster0.3cm3wfw.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

const client = new MongoClient(uri, {
  serverSelectionTimeoutMS: 5000 // 5 seconds timeout
});

async function run() {
  try {
    console.log("Connecting to MongoDB Atlas...");
    await client.connect();
    console.log("Connected successfully to server");
    
    // We will test with 'global_carbon_footprint_project' which exists in the screenshot
    const db = client.db("global_carbon_footprint_project");
    const collection = db.collection('test_connection');
    
    console.log("Attempting to insert a document...");
    const result = await collection.insertOne({ test: "Connection works!", timestamp: new Date() });
    console.log(`Document inserted with _id: ${result.insertedId}`);
    
    console.log("Attempting to find the document...");
    const doc = await collection.findOne({ _id: result.insertedId });
    console.log("Found:", doc);
    
  } catch (err) {
    console.error("Connection or operation failed!");
    console.error(err);
  } finally {
    await client.close();
    console.log("Connection closed.");
  }
}

run().catch(console.dir);
