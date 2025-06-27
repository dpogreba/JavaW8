# JavaW8 Android App

A coffee shop finder app that uses OpenStreetMap for mapping.

## Maps Implementation

This app uses the osmdroid library for maps and the Overpass API for searching for places. The main benefits include:

- **No API key required**: Works out of the box without any Google Cloud setup
- **Open source**: Uses only open-source mapping technologies
- **Free to use**: No billing or usage limits

## Architecture

The app uses a provider-based architecture that abstracts the mapping implementation:

- `MapProvider` interface defines the contract for map providers
- `OsmdroidProvider` is the current implementation using osmdroid
- `MapFactory` creates the appropriate provider (currently always osmdroid)
- `PlaceInfo` provides a standardized way to represent places across providers

## Adding Google Maps Support

The app is designed to make it easy to add Google Maps support in the future if needed. To do so:

1. Add Google Maps dependencies back to app/build.gradle.kts
2. Update MapFactory to check preferences and return the appropriate provider
3. Re-enable the map provider toggle in settings
4. Update the MapTogglePreference class to store and retrieve the actual preference

## Build and Run

This project uses Gradle. To build and run the app:

1. Open the project in Android Studio
2. Sync Gradle files
3. Run the app on an emulator or device

## Permissions

The app requires the following permissions:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: To show the user's location on the map
- `INTERNET`: To download map tiles and search for places
- `WRITE_EXTERNAL_STORAGE` (for API < 29): For caching map tiles
