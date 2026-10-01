<h1 align="center">SplitShare 💸</h1>

<p align="center">
  A modern Android app for splitting group expenses. Keep an itemized ledger of shared expenses and direct transfers, and see at a glance who owes whom, with a short list of payments that settles everyone up.
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white">
</p>

<p align="center">
  <a href="https://github.com/redmas45/Split_app/releases/latest">
    <img alt="Download APK" src="https://img.shields.io/badge/Download_APK-FF4081?style=for-the-badge&logo=android&logoColor=white">
  </a>
</p>

---

## 🌟 Features

- **Accounts:** sign in with email and password, or with Google, through Firebase Authentication. Password reset by email is built in.
- **Real-time sync:** expenses, members and transfers sync instantly through Cloud Firestore. Every edit is applied as one safe change, so two people adding expenses at the same moment never overwrite each other.
- **Create and join groups:** share a group code (or use the Share button) to invite people. When you join, say which existing member you are, or join as a new member, so nobody is listed twice and newcomers never owe for expenses from before they joined.
- **Itemized ledger:** record expenses and choose who paid and who the expense is split between. Every row shows who added it and when.
- **Transfers:** log money one person gave another directly.
- **Exact money math:** all amounts are handled in whole paise, never floating point, so shares always add up to the total. When an amount doesn't divide evenly, the extra paise are spread fairly.
- **Settle up:** the Summary tab shows each person's balance and a short list of payments that settles the group. It is a greedy method that gives a small number of payments, not always the absolute minimum.
- **Safe to use:** deleting a transaction or removing a member always asks first, and a member with transactions can't be removed.
- **Light and dark mode** in one consistent brand theme.

## 📸 Screenshots

Screenshots are not included yet.

## 🛠️ Tech stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Navigation:** Navigation 3
- **Backend:** Firebase Authentication and Cloud Firestore
- **Build:** Gradle (Kotlin DSL)

## 🚀 Getting started

### Prerequisites
- Android Studio (current stable release recommended)
- JDK 17

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/redmas45/Split_app.git
   ```
2. Open the project in **Android Studio** and let Gradle sync.
3. Run the app on an emulator or a physical device.

### Building from the command line
```bash
./gradlew assembleDebug        # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # lint
```
Instrumented (on-device) tests need an emulator or phone:
```bash
./gradlew connectedDebugAndroidTest
```

## 🔒 Firestore security rules

The app's real protection is its Firestore rules, kept in [`firestore.rules`](firestore.rules) (with [`firebase.json`](firebase.json)) so they can be reviewed. **Don't deploy them until every user has installed an app version that reads groups one at a time** (older versions run a query the rules deny). Test them in the Firebase console's Rules Playground first. Note the admin check requires the admin account's email to be verified.

## 📦 Releasing

CI runs on every push and pull request (unit tests, lint, debug build). A release is built and published only when you push a version tag:

```bash
git tag v1.2.0 && git push origin v1.2.0
```

The release job signs the APK with a key you keep out of the repo. Add these **GitHub Secrets** first: `KEYSTORE_BASE64` (your `.jks` file, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Create the key once with `keytool -genkeypair -v -keystore splitshare-release.jks -alias splitshare -keyalg RSA -keysize 2048 -validity 10000`, and register its SHA-1 and SHA-256 fingerprints in the Firebase console (needed for Google sign-in).

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues).
