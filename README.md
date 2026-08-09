# EasyURL 

Hey there! Welcome to **EasyURL** — a clean, fast, and simple URL shortener project. 

**Live Demo:** [https://easyurl.tech](https://easyurl.tech)

I built this to make sharing long, clunky links a breeze. It's got both a frontend (`client`) and a backend (`server`), and it's fully containerized with Docker to make running it super easy.

## Sneak Peek

Here's what the app looks like:

<p align="center">
  <img src="images/Screenshot%202026-08-09%20221318.png" alt="App Screenshot 1" width="45%" />
  <img src="images/Screenshot%202026-08-09%20221741.png" alt="App Screenshot 2" width="45%" />
  <img src="images/Screenshot%202026-08-09%20221757.png" alt="App Screenshot 3" width="45%" />
  <img src="images/Screenshot%202026-08-09%20221957.png" alt="App Screenshot 4" width="45%" />
</p>

## How It Works

1. **Shorten a URL**: You paste a long, cumbersome URL into the React frontend.
2. **Backend Processing**: The React client securely sends this URL to our Spring Boot backend API.
3. **Storage & Caching**: The backend generates a unique short alias. It saves the mapping in a PostgreSQL database (hosted on Neon) and caches it in Redis (via Upstash) for lightning-fast retrieval.
4. **Instant Redirection**: When someone visits the short link, the backend intercepts the request, checks the Redis cache first (for maximum speed), retrieves the original long URL, and redirects the user instantly!

## What's Inside?

- **`client/`**: The frontend UI where all the magic happens for users.
- **`server/`**: The heavy lifter backend API handling the link shrinking.
- **Docker Ready**: You'll find a `docker-compose.yml` and `Dockerfile` to get things spun up without the headache of manual setups.

