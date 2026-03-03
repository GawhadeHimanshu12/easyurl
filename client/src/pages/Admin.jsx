import { useState, useEffect } from 'react';
import api from '../services/api';

export default function Admin() {
    const [users, setUsers] = useState([]);

    useEffect(() => {
        api.get('/admin/users').then(res => setUsers(res.data));
    }, []);

    return (
        <div className="admin">
            <h2>Admin Panel: Users</h2>
            <table className="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Joined</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map(u => (
                        <tr key={u.id}>
                            <td>{u.id}</td>
                            <td>{u.name}</td>
                            <td>{u.email}</td>
                            <td><span className={`badge ${u.role}`}>{u.role}</span></td>
                            <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}