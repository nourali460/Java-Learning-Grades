const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');

const app = express();
app.use(cors());
app.use(express.json());

// === MongoDB Setup ===
const uri = process.env.MONGO_URI;
const client = new MongoClient(uri);
let db;
async function connectToMongo() {
  if (!db) {
    await client.connect();
    db = client.db('java_learning');
    console.log('🧠 MongoDB connected');
  }
  return db;
}

// === /grades GET + POST ===
app.get('/grades', async (req, res) => {
  const { studentId, admin, course, assignment } = req.query;
  try {
    const db = await connectToMongo();
    const query = {};
    if (studentId) query.studentId = studentId.toString();
    if (admin) query.admin = admin.toString();
    if (course) query.course = course.toString();
    if (assignment) query.assignment = assignment.toString();

    const grades = await db.collection('grades').find(query).toArray();
    res.json(grades);
  } catch (err) {
    console.error('Failed to fetch grades:', err);
    res.status(500).json({ message: 'Error fetching grades' });
  }
});

app.post('/grades', async (req, res) => {
  const { studentId, course, assignment, grade, consoleOutput, timestamp, admin } = req.body;
  if (!studentId || !course || !assignment || !grade || !admin) {
    return res.status(400).json({ message: 'Missing required fields' });
  }
  try {
    const db = await connectToMongo();
    const gradeEntry = {
      studentId: studentId.toString(),
      course: course.toString(),
      assignment: assignment.toString(),
      grade: grade.toString(),
      consoleOutput: consoleOutput?.toString() || '',
      timestamp: timestamp || new Date().toISOString(),
      admin: admin.toString()
    };
    const result = await db.collection('grades').updateOne(
      { studentId: gradeEntry.studentId, course: gradeEntry.course, assignment: gradeEntry.assignment },
      { $set: gradeEntry },
      { upsert: true }
    );
    res.json({ message: 'Grade submitted successfully', upserted: result.upsertedCount > 0 });
  } catch (err) {
    console.error('Grade upsert error:', err);
    res.status(500).json({ message: 'Failed to submit grade' });
  }
});

// === Admin Routes ===

// Add or update admin
app.post('/admins/add', async (req, res) => {
  const { name, password } = req.body;
  if (!name || !password)
    return res.status(400).json({ message: 'Missing fields' });

  try {
    const db = await connectToMongo();
    await db.collection('admins').updateOne(
      { name },
      { $set: { name, password } },
      { upsert: true }
    );
    res.json({ message: 'Admin added/updated' });
  } catch (err) {
    console.error('Error adding admin:', err);
    res.status(500).json({ message: 'Error adding admin' });
  }
});

// Validate admin credentials
app.post('/admins/validate', async (req, res) => {
  const { name, password } = req.body;
  if (!name || !password)
    return res.status(400).json({ success: false });

  try {
    const db = await connectToMongo();
    const admin = await db.collection('admins').findOne({ name, password });
    res.json({ success: !!admin });
  } catch (err) {
    console.error('Error validating admin:', err);
    res.status(500).json({ success: false });
  }
});

// Check if admin exists by name only
app.post('/admins/contains', async (req, res) => {
  const { name } = req.body;
  if (!name) return res.status(400).json({ exists: false, message: 'Missing name' });

  try {
    const db = await connectToMongo();
    const exists = await db.collection('admins').findOne({ name });
    res.json({ exists: !!exists });
  } catch (err) {
    console.error('Error checking admin existence:', err);
    res.status(500).json({ exists: false, message: 'Error checking admin' });
  }
});

// List all admin/professor names
app.get('/admins', async (_req, res) => {
  try {
    const db = await connectToMongo();
    const admins = await db.collection('admins')
      .find({}, { projection: { _id: 0, name: 1 } })
      .toArray();
    const names = admins.map(admin => admin.name);
    res.json(names);
  } catch (err) {
    console.error('Error fetching admin list:', err);
    res.status(500).json({ error: 'Error retrieving professors' });
  }
});

app.delete('/admins/remove', async (req, res) => {
  const { name } = req.body;
  if (!name) return res.status(400).json({ message: 'Missing name' });
  try {
    const db = await connectToMongo();
    await db.collection('admins').deleteOne({ name });
    res.json({ message: 'Admin removed' });
  } catch (err) {
    console.error('Error removing admin:', err);
    res.status(500).json({ message: 'Error removing admin' });
  }
});

// === Student Routes ===
app.post('/students/add', async (req, res) => {
  const { id, password } = req.body;
  if (!id || !password)
    return res.status(400).json({ message: 'Missing student ID or password' });

  try {
    const db = await connectToMongo();
    await db.collection('students').updateOne(
      { id },
      { $set: { id, password } },
      { upsert: true }
    );
    res.json({ message: 'Student added/updated' });
  } catch (err) {
    console.error('Error adding student:', err);
    res.status(500).json({ message: 'Error adding student' });
  }
});

app.delete('/students/remove', async (req, res) => {
  const { id } = req.body;
  if (!id) return res.status(400).json({ message: 'Missing student ID' });
  try {
    const db = await connectToMongo();
    await db.collection('students').deleteOne({ id });
    res.json({ message: 'Student removed' });
  } catch (err) {
    console.error('Error removing student:', err);
    res.status(500).json({ message: 'Error removing student' });
  }
});

app.post('/validateStudent', async (req, res) => {
  const { id, password } = req.body;
  if (!id || !password) return res.status(400).json({ success: false });
  try {
    const db = await connectToMongo();
    const student = await db.collection('students').findOne({ id, password });
    res.json({ success: !!student });
  } catch (err) {
    console.error('Error validating student:', err);
    res.status(500).json({ success: false });
  }
});

// === Start Server ===
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`✅ Backend running on port ${PORT}`));
