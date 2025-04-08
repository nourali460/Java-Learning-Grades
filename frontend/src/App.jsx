import { useState } from 'react';
import ProfessorLogin from './pages/ProfessorLogin';
import GradeTable from './pages/GradeTable';
import './style.css';

function App() {
  const [token, setToken] = useState(localStorage.getItem('jwt'));
  return token ? (
    <GradeTable token={token} />
  ) : (
    <ProfessorLogin setToken={setToken} />
  );
}

export default App;