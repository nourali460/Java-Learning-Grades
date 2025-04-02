// Secure index.js with bcrypt + JWT + Role-based Route Protection
const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
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

function verifyToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) return res.sendStatus(401);

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return res.sendStatus(403);
        req.user = user;
        next();
    });
}

function requireRole(role) {
    return (req, res, next) => {
        if (!req.user || req.user.role !== role) return res.sendStatus(403);
        next();
    };
}

// === Admin Routes ===
app.post('/admins/add', async (req, res) => {
    const { name, password } = req.body;
    if (!name || !password)
        return res.status(400).json({ message: 'Missing fields' });

    const db = await connectToMongo();
    const hash = await bcrypt.hash(password, 10);
    await db.collection('admins').updateOne(
        { name },
        { $set: { name, password: hash } },
        { upsert: true }
    );
    res.json({ message: 'Admin added/updated' });
});

app.post('/admins/validate', async (req, res) => {
    const { name, password } = req.body;
    const db = await connectToMongo();
    const admin = await db.collection('admins').findOne({ name });
    if (!admin) return res.status(401).json({ success: false });

    const match = await bcrypt.compare(password, admin.password);
    if (!match) return res.status(401).json({ success: false });

    const token = jwt.sign({ name, role: 'admin' }, JWT_SECRET, { expiresIn: '3d' });
    res.json({ success: true, token });
});

app.post('/admins/contains', async (req, res) => {
    const { name } = req.body;
    const db = await connectToMongo();
    const exists = await db.collection('admins').findOne({ name });
    res.json({ exists: !!exists });
});

app.get('/admins', async (_req, res) => {
    const db = await connectToMongo();
    const admins = await db.collection('admins').find({}, { projection: { _id: 0, name: 1 } }).toArray();
    res.json(admins.map(a => a.name));
});

app.delete('/admins/remove', async (req, res) => {
    const { name } = req.body;
    const db = await connectToMongo();
    await db.collection('admins').deleteOne({ name });
    res.json({ message: 'Admin removed' });
});

// === Student Routes ===
app.post('/students/add', verifyToken, requireRole('admin'), async (req, res) => {
    const { id, password } = req.body;
    if (!id || !password) return res.status(400).json({ message: 'Missing fields' });

    const db = await connectToMongo();
    const hash = await bcrypt.hash(password, 10);
    await db.collection('students').updateOne(
        { id },
        { $set: { id, password: hash } },
        { upsert: true }
    );
    res.json({ message: 'Student added/updated' });
});

app.delete('/students/remove', verifyToken, requireRole('admin'), async (req, res) => {
    const { id } = req.body;
    const db = await connectToMongo();
    await db.collection('students').deleteOne({ id });
    res.json({ message: 'Student removed' });
});

// === Student Login (With Admin Validation) ===
app.post('/validateStudent', async (req, res) => {
    const { id, password, admin } = req.body;
    if (!admin) return res.status(400).json({ success: false, message: 'Missing admin name' });

    const db = await connectToMongo();
    const adminExists = await db.collection('admins').findOne({ name: admin });
    if (!adminExists) return res.status(401).json({ success: false, message: 'Invalid admin' });

    const student = await db.collection('students').findOne({ id });
    if (!student) return res.status(401).json({ success: false });

    const match = await bcrypt.compare(password, student.password);
    if (!match) return res.status(401).json({ success: false });

    const token = jwt.sign({ id, role: 'student' }, JWT_SECRET, { expiresIn: '3d' });
    res.json({ success: true, token });
});

// === Grade Submission (Protected for Students Only) ===
app.post('/grades', verifyToken, requireRole('student'), async (req, res) => {
    const { studentId, course, assignment, grade, consoleOutput, timestamp, admin } = req.body;
    if (!studentId || !course || !assignment || !grade || !admin)
        return res.status(400).json({ message: 'Missing required fields' });

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
});

// === Grade Retrieval (Open for Now) ===
app.get('/grades', async (req, res) => {
    const { studentId, admin, course, assignment } = req.query;
    const db = await connectToMongo();
    const query = {};
    if (studentId) query.studentId = studentId.toString();
    if (admin) query.admin = admin.toString();
    if (course) query.course = course.toString();
    if (assignment) query.assignment = assignment.toString();

    const grades = await db.collection('grades').find(query).toArray();
    res.json(grades);
});

// === Server Startup ===
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`✅ Backend running on port ${PORT}`));
