# Jetpack Compose Marketplace App

This Android app now includes sign in and sign up screens connected to the FastAPI auth API used by the Marketplace Andes backend.

## Local configuration

The app reads local configuration from `app/secrets.properties`.

Create it from the example file:

```bash
cp app/secrets.properties.example app/secrets.properties
```

Then set your values:

```properties
MAPS_API_KEY=YOUR_MAPS_API_KEY
API_BASE_URL=http://10.0.2.2:8000/
```

## API base URL options

- **Android emulator + backend on the same computer**
  - Use `http://10.0.2.2:8000/`
  - `10.0.2.2` is the Android emulator alias for your host machine localhost

- **Physical Android device + backend on the same Wi‑Fi network**
  - Use `http://<YOUR_COMPUTER_LAN_IP>:8000/`
  - Example: `http://192.168.1.42:8000/`
  - Your phone and computer must be on the same network

- **Remote server**
  - Use the full deployed API URL ending with `/`

## Run the FastAPI backend locally

The backend lives in:

`/home/orpheezt/personal/moviles/backend`

### Option 1: docker compose

From the backend project root:

```bash
for f in *.template; do cp -n "$f" "${f%.template}"; done
docker compose up --build
```

This exposes the API on port `8000`.

### Option 2: local Python environment

The FastAPI entrypoint is:

`src.marketplace_andes_backend.app:app`

Example command from the backend project root:

```bash
uv run fastapi dev src/marketplace_andes_backend/app.py
```

If you prefer `uvicorn`, use the same app module:

```bash
uv run uvicorn src.marketplace_andes_backend.app:app --host 0.0.0.0 --port 8000 --reload
```

## Android auth API contract

The app is integrated with these backend endpoints:

- `POST /auth/signup`
  - JSON body: `name`, `email`, `password`

- `POST /auth/login`
  - Form body: `username`, `password`
  - The app sends the email in the `username` field, matching FastAPI OAuth2 form expectations

- `GET /users/me`
  - Used after login to fetch the authenticated user

## Typical development flows

### Emulator + localhost backend

1. Start the backend on your computer at port `8000`
2. Set `API_BASE_URL=http://10.0.2.2:8000/` in `app/secrets.properties`
3. Sync Gradle
4. Run the Android app in the emulator

### Physical device + LAN IP backend

1. Start the backend with host `0.0.0.0` so it is reachable on your network
2. Find your computer LAN IP address
3. Set `API_BASE_URL=http://<YOUR_COMPUTER_LAN_IP>:8000/` in `app/secrets.properties`
4. Make sure your Android device is on the same Wi‑Fi network
5. Sync Gradle and run the app on the device

## Notes

- The app allows cleartext HTTP traffic for local development so it can connect to local FastAPI instances over `http://`
- If you change `API_BASE_URL`, sync or rebuild the project so `BuildConfig.API_BASE_URL` is regenerated
- A saved session token allows the app to reopen directly on the home screen
