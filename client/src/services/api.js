import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    withCredentials: true, 
});

api.interceptors.request.use((config) => {
    let anonId = localStorage.getItem('anon_id');
    if (!anonId) {
        anonId = 'anon_' + Math.random().toString(36).substring(2, 15);
        localStorage.setItem('anon_id', anonId);
    }
    config.headers['X-Anonymous-Session'] = anonId;
    return config;
});

export default api;