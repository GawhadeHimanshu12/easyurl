import { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../App';
import api from '../services/api';

export default function Navbar() {
    const { user, setUser } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    const handleLogout = async () => {
        await api.post('/auth/logout');
        setUser(null);
        navigate('/');
    };

    return (
        <nav className="navbar">
            <div className="nav-brand">
                <Link to="/">🔗 URL Shortener</Link>
            </div>
            <div className="nav-links">
                {user ? (
                    <>
                        <span className="user-greeting">Hi, {user.name}</span>
                        <Link to="/dashboard">Dashboard</Link>
                        {user.role === 'ADMIN' && <Link to="/admin">Admin</Link>}
                        <button onClick={handleLogout} className="btn-secondary">Logout</button>
                    </>
                ) : (
                    <button onClick={handleLogin} className="btn-primary">Login with Google</button>
                )}
            </div>
        </nav>
    );
}