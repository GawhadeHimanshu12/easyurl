import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. Configure the load test stages for a stress test
export const options = {
  stages: [
    { duration: '30s', target: 5000 },
    { duration: '30s', target: 20000 },
    { duration: '30s', target: 50000 },
    { duration: '30s', target: 50000 },
  ],
  thresholds: {
    // These thresholds will mark the test as failed if we start breaking
    http_req_duration: ['p(95)<1000'], // Expect 95% of requests to complete under 1s
    http_req_failed: ['rate<0.05'], // Max 5% error rate before we consider it struggling
  },
};

// Use host.docker.internal if running k6 in docker to access the host's localhost
// Otherwise, just use localhost if running k6 directly
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export default function () {
  // --- TEST 1: URL Creation (Testing how many links it can support) ---
  const originalUrl = `https://example.com/test-${Math.random().toString(36).substring(7)}`;
  
  const createPayload = JSON.stringify({
    originalUrl: originalUrl,
  });

  const createParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const createRes = http.post(`${BASE_URL}/api/v1/urls/shorten`, createPayload, createParams);
  
  const createSuccess = check(createRes, {
    'create status is 201': (r) => r.status === 201 || r.status === 200,
  });

  // --- TEST 2: Link Clicking (Testing concurrent redirects) ---
  if (createSuccess) {
    try {
      const responseBody = JSON.parse(createRes.body);
      const shortUrl = responseBody.shortUrl; 
      
      if (shortUrl) {
        // We use redirect: 'manual' if we don't want k6 to actually follow the redirect to the target site
        // Rewrite localhost to host.docker.internal so k6 inside docker can reach the host machine
        const redirectUrl = shortUrl.replace('localhost', 'host.docker.internal');
        
        const redirectRes = http.get(redirectUrl, { redirects: 0 });
        
        check(redirectRes, {
          'redirect status is 302': (r) => r.status === 302 || r.status === 301,
        });
      }
    } catch (e) {
      // In case parsing fails or response format is unexpected
      console.error("Failed to parse creation response:", e);
    }
  }

  // Add a small sleep to simulate real user think time
  sleep(1);
}
