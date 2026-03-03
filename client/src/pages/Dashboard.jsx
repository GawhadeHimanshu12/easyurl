import { useState, useEffect } from 'react';
import api from '../services/api';

export default function Dashboard() {
    const [urls, setUrls] = useState([]);

    useEffect(() => {
        fetchUrls();
    }, []);

    const fetchUrls = () => {
        api.get('/urls/my-urls').then(res => setUrls(res.data));
    };

    const handleDelete = async (shortUrl) => {
        if (!window.confirm("Are you sure you want to delete this URL?")) return;
        
        const key = shortUrl.split('/').pop();
        
        try {
            await api.delete(`/urls/${key}`);
            setUrls(urls.filter(u => u.shortUrl !== shortUrl));
        } catch (e) {
            console.error("Failed to delete", e);
            alert("Failed to delete URL");
        }
    };

    return (
        <div className="dashboard">
            <h2>My Dashboard</h2>
            <table className="data-table">
                <thead>
                    <tr>
                        <th>Short URL</th>
                        <th>Original URL</th>
                        <th>Clicks</th>
                        <th>Created</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {urls.map((u, i) => (
                        <tr key={i}>
                            <td><a href={u.shortUrl} target="_blank" rel="noreferrer">{u.shortUrl}</a></td>
                            <td className="truncate" title={u.originalUrl}>{u.originalUrl}</td>
                            <td>{u.clickCount}</td>
                            <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                            <td>
                                <button onClick={() => handleDelete(u.shortUrl)} className="btn-danger">Delete</button>
                            </td>
                        </tr>
                    ))}
                    {urls.length === 0 && <tr><td colSpan="5">No URLs found. Go create some!</td></tr>}
                </tbody>
            </table>
        </div>
    );
}