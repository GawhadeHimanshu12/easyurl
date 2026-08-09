import { useState, useContext } from 'react';
import api from '../services/api';
import { AuthContext } from '../App';

export default function Home() {
    const { user } = useContext(AuthContext);
    const [originalUrl, setOriginalUrl] = useState('');
    const [customAlias, setCustomAlias] = useState('');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setResult(null);

        try {
            const res = await api.post('/urls/shorten', { originalUrl, customAlias });
            setResult(res.data.shortUrl);
        } catch (err) {
            setError(err.response?.data?.message || 'An error occurred');
        }
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(result);
        alert('Copied to clipboard!');
    };

    return (
        <div className="hero-section">
            <h1>Shorten Your Links</h1>
            <form onSubmit={handleSubmit} className="shorten-form">
                <input 
                    type="url" 
                    placeholder="https://your-long-url.com" 
                    value={originalUrl}
                    onChange={(e) => setOriginalUrl(e.target.value)}
                    required 
                />
                <input 
                    type="text" 
                    placeholder="Custom alias (optional)" 
                    value={customAlias}
                    onChange={(e) => setCustomAlias(e.target.value)}
                />
                <button type="submit" className="btn-primary">Shorten</button>
            </form>

            {error && <div className="error-box">{error}</div>}

            {result && (
                <div className="result-box">
                    <p>Your short URL is ready:</p>
                    <a href={result} target="_blank" rel="noreferrer">{result}</a>
                    <button onClick={handleCopy} className="btn-secondary">Copy</button>
                    {!user && (
                        <div className="login-prompt">
                            ⚠️ <button onClick={() => window.location.href = '/oauth2/authorization/google'} className="link-btn">Login with Google</button> to save and track this URL forever!
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}