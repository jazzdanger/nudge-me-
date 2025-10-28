# Google OAuth Setup Guide for Nudge Me App

## Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note down your project ID

## Step 2: Enable Google Calendar API

1. In the Google Cloud Console, go to "APIs & Services" > "Library"
2. Search for "Google Calendar API"
3. Click on it and press "Enable"

## Step 3: Configure OAuth Consent Screen

1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type (unless you have a Google Workspace account)
3. Fill in the required information:
   - App name: "Nudge Me"
   - User support email: your email
   - Developer contact information: your email
4. Add scopes:
   - `https://www.googleapis.com/auth/calendar.readonly`
5. Save and continue through all steps

## Step 4: Create OAuth 2.0 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Android" as application type
4. Fill in:
   - Name: "Nudge Me Android"
   - Package name: `com.example.reminderapp` (from your app's build.gradle)
   - SHA-1 certificate fingerprint: (see below for how to get this)

## Step 5: Get SHA-1 Fingerprint

Run this command in your project directory:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint and paste it in the OAuth client configuration.

## Step 6: Update Your App

1. Copy the generated OAuth client ID
2. Update `app/src/main/res/values/strings.xml`:
```xml
<string name="google_oauth_client_id">YOUR_CLIENT_ID_HERE</string>
```

## Step 7: Test the Integration

1. Build and run your app
2. Navigate to the Calendar tab
3. Tap "Sign in to Google"
4. Select your Google account
5. Grant calendar permissions
6. Your calendar events should now appear!

## Troubleshooting

- If sign-in fails, check that the package name and SHA-1 fingerprint match exactly
- Make sure the Google Calendar API is enabled
- Verify that the OAuth consent screen is properly configured
- Check the Android logs for any error messages

## Security Note

Never commit your OAuth client ID to public repositories. Consider using environment variables or a separate config file for production builds.
