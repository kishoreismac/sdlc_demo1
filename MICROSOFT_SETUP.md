# Microsoft Calendar Setup

This site now supports real Microsoft sign-in and reads the signed-in user's daily schedule from Microsoft Graph.

## 1. Register an app in Microsoft Entra

Create a Single-page application (SPA) app registration in Microsoft Entra ID.

Use these redirect URIs in the SPA platform settings:

- `http://127.0.0.1:8000/`
- `http://localhost:8000/`

Copy these values from the app overview:

- Application (client) ID
- Directory (tenant) ID, if you want to lock sign-in to a specific tenant

## 2. Grant delegated API permissions

Add these Microsoft Graph delegated permissions:

- `User.Read`
- `Calendars.Read`

If your tenant requires admin consent, grant consent before testing.

## 3. Update the site config

Edit [auth-config.js](/Users/ECOGNITY/.gemini/antigravity/scratch/ec-ai-website/auth-config.js) and replace:

- `YOUR_MICROSOFT_APP_CLIENT_ID`
- `tenantId` if you do not want to use `common`

## 4. Run the site

Serve the website locally and open:

- `http://127.0.0.1:8000/`

Sign in from the dashboard calendar panel. Once authenticated, the selected day's meetings are loaded from Microsoft Graph.
