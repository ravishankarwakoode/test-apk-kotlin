# test-apk-kotlin

A Kotlin desktop/console application that analyzes an APK file and generates a full APK security and metadata report in HTML.

## Features

- Reads APK file metadata
- Extracts package name, version name, version code
- Reads minSdk and targetSdk
- Lists permissions, activities, services, receivers, and providers
- Lists native libraries under `lib/`
- Lists `META-INF` files
- Calculates SHA-256 hash
- Saves a full HTML report next to the APK

## Project structure

```text
.
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
├── .gitignore
└── src/
    └── main/
        └── kotlin/
            └── com/example/apkreport/Main.kt
```

## Run

From the project root:

```bash
gradle run --args="/path/to/app.apk"
```

Example:

```bash
gradle run --args="/Users/yourname/Desktop/sample.apk"
```

The app will:
- print a full report to the terminal
- create a report file in the same folder as the APK, for example:
  `sample_report.html`

## Build

```bash
gradle build
```

## Notes

This APK reader inspects the APK archive and Android manifest. It does not run the application or emulate device behavior.
