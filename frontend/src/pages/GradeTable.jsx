import { useEffect, useState } from 'react';

export default function GradeTable({ token }) {
  const [grades, setGrades] = useState([]);

  useEffect(() => {
    fetch("/api/grades", {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => setGrades(data));
  }, [token]);

  return (
    <div className="p-6">
      <h2 className="text-xl font-semibold mb-4">Student Grades</h2>
      <table className="min-w-full table-auto border rounded shadow">
        <thead className="bg-gray-100 sticky top-0">
          <tr>
            <th className="px-4 py-2 text-left">Student ID</th>
            <th className="px-4 py-2">Course</th>
            <th className="px-4 py-2">Assignment</th>
            <th className="px-4 py-2">Grade</th>
            <th className="px-4 py-2">Timestamp</th>
          </tr>
        </thead>
        <tbody>
          {grades.map((g, i) => (
            <tr key={i} className="even:bg-gray-50 hover:bg-gray-100 transition">
              <td className="px-4 py-2">{g.studentId}</td>
              <td className="px-4 py-2">{g.course}</td>
              <td className="px-4 py-2">{g.assignment}</td>
              <td className="px-4 py-2">{g.grade}</td>
              <td className="px-4 py-2">{g.timestamp}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}