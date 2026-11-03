# ClubNexus Android

Native Kotlin + Jetpack Compose application for ClubNexus. It calls the existing Express API and uses the existing database; this project does not create or change a backend or database.

## Open in Android Studio

1. Install a recent Android Studio with Android SDK Platform 37 and JDK 17 support.
2. Choose **Open** and select this `clubnexus-android` directory.
3. Allow Gradle sync to download dependencies.
4. Select the `developmentDebug` build variant and run the `app` configuration on an emulator or connected device.

The project uses AGP 9.1.1, Gradle 9.3.1, compileSdk 37, targetSdk 36, and Compose BOM 2026.09.00. The development default `http://10.0.2.2:4000/api/` points to port 4000 on the host from the standard Android emulator.

## Connect a physical phone to a development API

The phone cannot use `localhost` or the emulator-only `10.0.2.2` address. Build with the computer's reachable LAN address, with the phone and computer on the same network and the backend/firewall allowing access:

```powershell
./gradlew.bat -PclubnexusDevApiUrl=http://192.168.1.4:4000/api/ :app:assembleDevelopmentDebug
```

Replace the example address with the computer's current LAN IP. This is plain HTTP for local development only; production must use HTTPS.

## Production API URL

Set the existing API's HTTPS base URL before a production build, either as an environment variable:

```powershell
$env:CLUBNEXUS_PROD_API_URL = "https://your-api-host.example.com/api/"
./gradlew.bat :app:assembleProductionRelease
```

or as a Gradle project property:

```powershell
./gradlew.bat -PclubnexusProdApiUrl=https://your-api-host.example.com/api/ :app:assembleProductionRelease
```

Production release tasks reject a missing or non-HTTPS URL. Do not ship a sample URL.

## Project foundation

- Compose theme uses ClubNexus's ink/navy surfaces, slate text, and sky-blue accent.
- Native navigation provides Dashboard, Members, Attendance, Inventory, Projects, Events, and Email Studio destinations.
- Development and production API URLs are independently configurable.
- Retrofit/OkHttp attaches the stored JWT as a bearer token and clears the session on 401.
- JWTs are encrypted with AES-GCM using a non-exportable AES key held by Android Keystore.
- Feature screens use the existing Express API. Installable debug builds are signed by Android's debug key; production release output needs a real API URL and signing configuration for publishing.
