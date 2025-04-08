import { useState } from 'react';

export default function ProfessorLogin({ setToken }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = async () => {
    const res = await fetch('/api/admins/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    if (!res.ok) return alert("Login failed");
    const { token } = await res.json();
    localStorage.setItem('jwt', token);
    setToken(token);
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <h1 className="text-2xl font-bold mb-4">Professor Login</h1>
      <input className="border p-2 m-1" type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} />
      <input className="border p-2 m-1" type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} />
      <button className="bg-blue-500 text-white px-4 py-2 rounded mt-2" onClick={handleLogin}>Login</button>
    </div>
  );
}