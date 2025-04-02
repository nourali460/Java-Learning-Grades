// index.js - Bootstraps Super Admin on Startup (Render Ready), Role Stored in DB, Rate Limiting Enabled
const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
const SUPER_ADMIN = (process.env.SUPER_ADMIN || 'ali').trim();
const uri = process.env.MONGO_URI;
const client = new MongoClient(uri);
let db;

const tokenBasedLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100,
    keyGenerator: (req) => {
        const auth = req.headers['authorization'];
        if (auth?.startsWith('Bearer ')) {
            try {
                const token = auth.split(' ')[1];
                const decoded = jwt.verify(token, JWT_SECRET);
                return decoded.name || decoded.id;
            } catch {
                return req.ip;
            }
        }
        return req.ip;
    },
    message: '⏱️ Too many requests. Please try again later.'
});
app.use(tokenBasedLimiter);

async function connectToMongo() {
    if (!db) {
        await client.connect();
        db = client.db('java_learning');
        console.log('🧠 MongoDB connected');
    }
    return db;
}

async function bootstrapSuperAdmin() {
    const db = await connectToMongo();
    const admins = db.collection('admins');
    const password = process.env.DEFAULT_ADMIN_PASSWORD;

    if (!SUPER_ADMIN || !password) {
        console.warn('⚠️ SUPER_ADMIN or DEFAULT_ADMIN_PASSWORD not set. Skipping super admin creation.');
        return;
    }

    const exists = await admins.findOne({ name: SUPER_ADMIN });
    if (exists) {
        console.log(`✅ Super admin '${SUPER_ADMIN}' already exists. Skipping.`);
        return;
    }

    const hash = await bcrypt.hash(password, 10);
    await admins.insertOne({ name: SUPER_ADMIN, password: hash, role: 'super' });
    console.log(`🚀 Super admin '${SUPER_ADMIN}' created successfully.`);
}

function verifyToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader?.split(' ')[1];
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

app.get('/roles', (_req, res) => {
    res.json({
        table: [
            { action: '🔐 Log in (/admins/validate)', superAdmin: true, admin: true, student: false },
            { action: '🔐 Log in (/validateStudent)', superAdmin: false, admin: false, student: true },
            { action: '➕ Add Admin (/admins/add)', superAdmin: true, admin: false, student: false },
            { action: '➖ Remove Admin (/admins/remove)', superAdmin: true, admin: false, student: false },
            { action: '📋 View All Admins (/admins)', superAdmin: true, admin: true, student: true },
            { action: '✅ Check if Admin Exists (/admins/contains)', superAdmin: true, admin: true, student: true },
            { action: '➕ Add Student (/students/add)', superAdmin: true, admin: true, student: false },
            { action: '➖ Remove Student (/students/remove)', superAdmin: true, admin: true, student: false },
            { action: '📤 Submit Grade (/grades POST)', superAdmin: false, admin: false, student: true },
            { action: '📥 View Grades (/grades GET)', superAdmin: true, admin: true, student: true }
        ]
    });
});

app.get('/whoami', verifyToken, (req, res) => {
    res.json({ user: req.user.name || req.user.id, role: req.user.role });
});

app.post('/admins/add', verifyToken, requireRole('admin'), async (req, res) => {
    const { name, password } = req.body;
    if (!name || !password) return res.status(400).json({ message: 'Missing fields' });
    if (name.trim() === SUPER_ADMIN) return res.status(403).json({ message: 'Super admin cannot be created via API' });
    if (String(req.user.name).trim() !== SUPER_ADMIN) return res.status(403).json({ message: 'Only super admin can add admins' });

    const db = await connectToMongo();
    const hash = await bcrypt.hash(password, 10);
    await db.collection('admins').updateOne(
        { name },
        { $set: { name, password: hash, role: 'admin' } },
        { upsert: true }
    );
    res.json({ message: 'Admin added/updated' });
});

app.post('/admins/validate', async (req, res) => {
    const { name, password } = req.body;
    if (!name || !password) return res.status(400).json({ success: false, message: 'Missing fields' });

    const db = await connectToMongo();
    const admin = await db.collection('admins').findOne({ name });
    if (!admin) return res.status(401).json({ success: false, message: 'User not found' });

    const match = await bcrypt.compare(password, admin.password);
    if (!match) return res.status(401).json({ success: false, message: 'Incorrect password' });

    const role = admin.role || (name.trim() === SUPER_ADMIN ? 'super' : 'admin');
    const token = jwt.sign({ name, role }, JWT_SECRET, { expiresIn: '3d' });
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
    const admins = await db.collection('admins').find({}, { projection: { _id: 0, name: 1, role: 1 } }).toArray();
    res.json(admins);
});

app.delete('/admins/remove', verifyToken, requireRole('admin'), async (req, res) => {
    const { name } = req.body;
    if (name.trim() === SUPER_ADMIN) return res.status(403).json({ message: 'Cannot remove super admin' });
    if (String(req.user.name).trim() !== SUPER_ADMIN) return res.status(403).json({ message: 'Only super admin can remove admins' });

    const db = await connectToMongo();
    await db.collection('admins').deleteOne({ name });
    res.json({ message: 'Admin removed' });
});

// 👇 Safely bootstrap and start the server
bootstrapSuperAdmin().then(() => {
    const PORT = process.env.PORT || 3000;
    console.log("✅ Server listening on port", PORT);
    app.listen(PORT, () => console.log(`🟢 Backend running on http://localhost:${PORT}`));
});