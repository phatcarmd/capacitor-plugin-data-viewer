# capacitor-plugin-data-viewer

A powerful local SQLite database explorer plugin for Capacitor apps. This plugin allows developers and testers to inspect, browse, and verify local database records directly within the app using a modern, performant UI.\
This plugin provides excellent support for performing mobile app automation testing at the database level.

<img src="screenshots/databases.png" alt="All Database Files" width="300">
<img src="screenshots/tables.png" alt="All Tables of Selected Database" width="300">
<img src="screenshots/records.png" alt="All Records of Selected Table" width="300">

## Prerequisites

To use this plugin, your environment and projects must meet the following minimum requirements:

- **Android**
  - Minimum SDK: 23 (Android 6.0 Marshmallow) or higher.
  - Android Gradle Plugin (AGP): 8.7.2
  - Gradle Wrapper: 8.9 or 8.10.2 (Recommended for AGP 8.7.x compatibility).
  - Java Version: JDK 21
- **iOS**
  - Deployment Target: iOS 12.0 or higher.
  - Xcode: 15.0 or newer (to support modern Swift and Capacitor features).

## Install

```bash
npm install https://github.com/phatcarmd/capacitor-plugin-data-viewer
npx cap sync
```

## API

<docgen-index>

- [`explore()`](#explore)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### explore()

```typescript
explore() => Promise<void>
```

---

</docgen-api>

## Usage

```bash
import { DataViewer }  from 'capacitor-plugin-data-viewer'
...
DataViewer.explore();
```

## Author

Phat Vuong (phatvuong.sm@gmail.com)
