# eGK Library

A library for reading German electronic health cards (elektronische Gesundheitskarte / eGK) via NFC on mobile devices.

## Overview

This workspace contains a Kotlin/Android library for NFC communication with German health cards, along with a Flutter demo application that demonstrates its usage.

## Project Structure

```
├── Android/                     # Native Android Library
│   └── egk-library/            # Kotlin library for eGK NFC communication
├── egk_demo_app/               # Flutter Demo Application
│   └── lib/                    # Flutter app demonstrating eGK reading
├── egk_demo_app_native/        # Flutter Plugin
│   ├── android/                # Android platform implementation
│   ├── ios/                    # iOS platform implementation
│   └── lib/                    # Dart API for the plugin
└── Schema_VSD.xsd              # VSD (Versichertenstammdaten) XML Schema
```

## Components

### Android Library (`Android/egk-library`)

Native Kotlin library that handles:
- NFC communication with eGK cards
- PACE protocol for secure card access using CAN (Card Access Number)
- Parsing of VSD (Versichertenstammdaten) data
- Cryptographic operations using Bouncy Castle
- Port of https://github.com/gematik/ref-OpenHealthCardKit (fully done by AI (Claude Opus 4.5), currently untested and unchecked)

**Requirements:**
- Min SDK: 24 (Android 7.0)
- Target SDK: 34

### Flutter Plugin (`egk_demo_app_native`)

A Flutter plugin that bridges the native Android/iOS implementations to Dart:
- NFC availability and status checks
- Reading eGK data with CAN authentication
- Session state management
- Data models for personal and insurance information

### Flutter Demo App (`egk_demo_app`)

A demo application showcasing the library's capabilities:
- NFC status display
- CAN input for card authentication
- Reading and displaying health card data

## Data Models

The library reads and parses the following data from eGK cards:

- **PersonalData**: Name, birth date, gender, address
- **InsuranceData**: Insurance provider, coverage details
- **StreetAddress / PostOfficeAddress**: Address information

## Getting Started

### Prerequisites

- Flutter SDK 3.10.7 or higher
- Android device with NFC capability
- An eGK card with known CAN (6-digit Card Access Number)

### Running the Demo App

```bash
cd egk_demo_app
flutter pub get
flutter run
```

## License

See individual project directories for license information.
