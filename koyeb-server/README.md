# AcadsCatchUp — Java Gmail & Email Checker Server (Koyeb Free Deployment)
**Developer**: F4TAL (Stevenson James G. Gastanes)  
**Runtime**: 100% Pure Java 21 (Zero 3rd-party dependencies)  
**Container**: Eclipse Temurin 21 Alpine JRE (< 100MB RAM)

---

## Features
- **100% Pure Standard Java**: Uses Java's built-in `com.sun.net.httpserver.HttpServer` and `javax.naming.directory`.
- **Interactive Web Interface**: Visit `https://your-app.koyeb.app/` in your browser to test any email with a modern UI.
- **REST API**: `GET /api/check?email=target@gmail.com` returns detailed JSON.
- **Health Check**: `GET /health` for Koyeb container health monitoring.
- **Multi-Level Detection**:
  1. RFC 5322 structure validation
  2. Official Google account constraints (6-30 chars, allowed characters, dot rules)
  3. Live DNS MX host discovery
  4. Direct SMTP handshake (`RCPT TO` existence test)
  5. Multi-provider cloud fallback

---

## Step-by-Step Koyeb Deployment Guide (Free Tier)

### Step 1: Create a GitHub Repository
1. Open your GitHub account.
2. Create a new repository, for example: `acadscatchup-checker`
3. Push the files inside this `koyeb-server` directory to your new repository:
   ```bash
   cd koyeb-server
   git init
   git add .
   git commit -m "Initial Java Gmail Checker microservice"
   git branch -M main
   git remote add origin https://github.com/<your-username>/acadscatchup-checker.git
   git push -u origin main
   ```

### Step 2: Deploy on Koyeb
1. Go to [https://app.koyeb.com/](https://app.koyeb.com/) and sign in (Free).
2. Click **Create Service** and select **GitHub**.
3. Select your repository (`acadscatchup-checker`).
4. Koyeb will automatically detect the `Dockerfile`.
5. Under **Service configuration**:
   - **Instance type**: Select **Free (Eco / Nano)** (512 MB RAM, 0.1 vCPU).
   - **Regions**: Choose the closest region (e.g. Frankfurt, Washington D.C., or Singapore).
   - **Ports**: Koyeb will automatically use port `8000` with HTTP.
6. Click **Deploy**.

Within 1–2 minutes, Koyeb will build your Java container and give you a public HTTPS URL:
`https://<app-name>-<user>.koyeb.app`

---

## Step 3: Link with AcadsCatchUp
Once deployed, copy your Koyeb URL and paste it into `GmailLookupUtil.java`:
```java
public static final String KOYEB_SERVICE_URL = "https://your-service.koyeb.app";
```
AcadsCatchUp will then query your private Java microservice whenever users register or recover accounts!
