import { useState, useEffect } from 'react';
import api from '../services/api';

export default function Admin() {
    const [users, setUsers] = useState([]);
    const [urls, setUrls] = useState([]);

    useEffect(() => {
        // Fetch both users and URLs when the page loads
        api.get('/admin/users').then(res => setUsers(res.data));
        api.get('/admin/urls').then(res => setUrls(res.data));
    }, []);

    return (
        <div className="admin">
            <h2>Admin Dashboard</h2>
            
            <h3 style={{marginTop: '3rem'}}>All Platform URLs</h3>
            <table className="data-table">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Short URL</th>
                        <th>Original URL</th>
                        <th>Created By</th>
                        <th>Clicks</th>
                    </tr>
                </thead>
                <tbody>
                    {urls.map((u, i) => (
                        <tr key={i}>
                            <td>{i + 1}</td>
                            <td><a href={u.shortUrl} target="_blank" rel="noreferrer">{u.shortUrl.split('/').pop()}</a></td>
                            <td className="truncate" title={u.originalUrl}>{u.originalUrl}</td>
                            <td>{u.userName ? `${u.userName} (${u.userEmail})` : 'Anonymous'}</td>
                            <td>{u.clickCount}</td>
                        </tr>
                    ))}
                </tbody>
            </table>

            <h3 style={{marginTop: '3rem'}}>All Platform URLs</h3>
            <table className="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Short Key</th>
                        <th>Original URL</th>
                        <th>User ID</th>
                        <th>Clicks</th>
                    </tr>
                </thead>
                <tbody>
                    {urls.map(u => (
                        <tr key={u.id}>
                            <td>{u.id}</td>
                            <td>{u.shortKey}</td>
                            <td className="truncate" title={u.originalUrl}>{u.originalUrl}</td>
                            <td>{u.user ? u.user.id : 'Anonymous'}</td>
                            <td>{u.clickCount}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}