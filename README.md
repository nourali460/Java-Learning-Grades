## 🌐 API Endpoint Access Table

| Endpoint             | Method | Access               | Description                    |
|----------------------|--------|----------------------|--------------------------------|
| `/admins/validate`   | POST   | Super Admin, Admin   | Log in, returns JWT            |
| `/admins/add`        | POST   | Super Admin          | Add new admin                  |
| `/admins/remove`     | DELETE | Super Admin          | Remove admin                   |
| `/admins`            | GET    | Public               | List admins + roles            |
| `/admins/contains`   | POST   | Public               | Check if admin exists          |
| `/students/add`      | POST   | Admin, Super Admin   | Add/update a student           |
| `/students/remove`   | DELETE | Admin, Super Admin   | Remove student                 |
| `/validateStudent`   | POST   | Public               | Student login, returns JWT     |
| `/grades` (POST)     | POST   | Student              | Submit or update a grade       |
| `/grades` (GET)      | GET    | Public               | View grades (with filters)     |
| `/whoami`            | GET    | Authenticated Users  | Identify current user          |
| `/roles`             | GET    | Public               | View access control table      |

---

### 📨 Request & 📤 Response Samples

<details>
<summary><strong>🔐 <code>/admins/validate</code></strong></summary>

**Request:**
```json
{ "name": "ali", "password": "admin123" }
```

**Response:**
```json
{ "success": true, "token": "jwt..." }
```
</details>

<details>
<summary><strong>➕ <code>/admins/add</code></strong></summary>

**Request:**
```json
{ "name": "nour", "password": "pass123" }
```

**Response:**
```json
{ "message": "Admin added/updated" }
```
</details>

<details>
<summary><strong>➖ <code>/admins/remove</code></strong></summary>

**Request:**
```json
{ "name": "nour" }
```

**Response:**
```json
{ "message": "Admin removed" }
```
</details>

<details>
<summary><strong>👥 <code>/students/add</code></strong></summary>

**Request:**
```json
{
  "id": "student123",
  "password": "studpass",
  "email": "student@example.com",
  "admin": "nour",
  "paid": false,
  "paymentLink": "https://...",
  "active": false,
  "createdAt": "2025-04-04T00:00:00Z",
  "paymentDate": null
}
```

**Response:**
```json
{ "message": "Student added/updated" }
```
</details>

<details>
<summary><strong>📥 <code>/grades</code> (POST)</strong></summary>

**Request:**
```json
{
  "studentId": "student123",
  "course": "CourseA",
  "assignment": "Assignment-1",
  "grade": "5/5",
  "consoleOutput": "Grader output...",
  "timestamp": "2025-04-02T12:00:00Z",
  "admin": "nour"
}
```

**Response:**
```json
{
  "message": "Grade submitted successfully",
  "upserted": true
}
```
</details>

<details>
<summary><strong>📊 <code>/grades</code> (GET)</strong></summary>

**Request (Query Params):**
```
?studentId=student123&admin=nour
```

**Response:**
```json
[
  {
    "studentId": "student123",
    "course": "CourseA",
    "assignment": "Assignment-1",
    "grade": "5/5",
    "consoleOutput": "...",
    "timestamp": "...",
    "admin": "nour"
  }
]
```
</details>

<details>
<summary><strong>❓ <code>/whoami</code></strong></summary>

**Response:**
```json
{ "user": "nour", "role": "admin" }
```
</details>

---

### ✅ Additional Behavior Summary

- 🔐 Protected endpoints use **JWT Bearer Token**
- 🧠 Super admin is **bootstrapped** from `.env`
- 🔁 Grade submissions are **upserted** (update or insert)
- 🧪 `consoleOutput` and `timestamp` are **required** for grades
- ⏳ JWT tokens expire in **3 days**
- 🧭 `/whoami` reads current token’s identity
- 📚 `/roles` shows access table for all roles

