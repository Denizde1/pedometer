# Pedometer: Android app + FastAPI + PostgreSQL
I don't want to maintain this project I was made this for fun. So, I will archive that if you want to use that use the forks (If any exists).


## Backend

```bash
cd backend
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt

# create the database
createdb pedometer_db
psql pedometer_db < schema.sql   # optional, SQLAlchemy also auto-creates tables

# set env vars (or put them in a .env file next to main.py)
export DATABASE_URL="postgresql://pedometer_user:secret@localhost:5432/pedometer_db"
export JWT_SECRET_KEY="pick-a-long-random-string"

uvicorn main:app --host 0.0.0.0 --port 8000
```

Endpoints:
- `POST /register` `{username, password}` -> `{access_token}`
- `POST /login` (form-encoded `username`, `password`) -> `{access_token}`
- `POST /steps/sync` (Bearer token) `{entries: [{device_id, day, steps}]}` — upserts
- `GET /steps` (Bearer token) — full history for the logged-in user
- `GET /health`

## Android app

Open the `android/` folder in Android Studio, let Gradle sync, then run on a
device (emulators don't have a real step sensor).

Before building, set the real server URL in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://forum.denizinyeri.org/pedometer-api/\"")
```
(Emulator testing against a local backend uses `http://10.0.2.2:8000/`, which is already the default.)

How it works:
1. `MainActivity` — register/login screen, requests `ACTIVITY_RECOGNITION` + notification permission, then starts the service.
2. `StepCounterService` — a foreground service listening to `Sensor.TYPE_STEP_COUNTER` (hardware step counter, cumulative since boot). It computes "steps today" from a daily baseline, writes to a local Room DB, and pushes unsynced days to the server on every sensor update.
3. `AppDatabase` / `StepDao` — local SQLite (via Room) cache so steps aren't lost when offline; synced rows are marked and re-sent if the server call fails.
4. `ApiClient` — Retrofit client + `TokenStore` (JWT stored in `EncryptedSharedPreferences`).
5. `BootReceiver` — restarts the service after the phone reboots, if already logged in.

## Notes / things to harden before production

- The JWT expires after 30 days; add a refresh flow if you want tighter expiry.
- `/steps/sync` does last-write-wins per device+day; if you need intraday granularity, key by hour instead of day.
- Consider HTTPS only (Cloudflare Tunnel works well here, matching your existing setup) — don't ship `http://` in production `API_BASE_URL`.
- Rate-limit `/register` and `/login` if this is public-facing.
