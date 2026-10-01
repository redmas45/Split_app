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

## 📲 Install

1. Open the [latest release](https://github.com/redmas45/Split_app/releases/latest) on your phone and download `app-release.apk`.
2. Install it (allow installs from your browser if Android asks) and log in.

Updates install over the existing app. Your groups and balances live in the cloud, so reinstalling never loses data.

> **Coming from a build older than v1.0.0?** Uninstall the old app once first: it was signed with a different key. Then install v1.0.0 and log in as usual.

## 🌟 Features

- **Accounts:** email and password or Google sign-in (Firebase Authentication), with "Forgot password?" by email and plain-language error messages.
- **Groups:** create a group, invite people with its code or the **Share** button, and leave a group any time. When you join, you pick which existing member you are, or join as someone new, so nobody is listed twice and you can't join the same group twice. Creating a group while offline syncs when you're back online.
- **Itemized ledger:** record expenses (who paid, and who it's split between, everyone by default) and transfers (money one person gave another). Newest entries first, each with its date and who added it. Category icons are picked from the description.
- **Exact money math:** every amount is handled in whole paise, never floating point, so shares always add up to the total. People who join later never owe for expenses from before they joined.
- **Settle up:** the Summary tab shows each person's balance and a short list of payments that settles the group (a greedy method: a small number of payments, not always the absolute minimum).
- **Safe by design:** deleting a transaction, removing a member, leaving a group and signing out always ask first. A member who has transactions can't be removed. Two people editing at the same moment never overwrite each other.
- **Real-time sync** through Cloud Firestore, with one live connection per group.
- **Notifications** when *someone else* adds an expense while the app is open. Permission is asked, with an explanation, the first time you create or join a group.
- **Admin panel** for the Super Admin: all users and groups, totals, read-only ledgers with names, ban/unban and delete group, each with a confirmation. Banned users are blocked immediately and can't get back in with the Back button.
- **Light and dark mode** in one consistent brand theme, with labelled icons for screen readers.

## 📸 Screenshots

Screenshots are not included yet.

## 🛠️ Tech stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Navigation:** Navigation 3
- **Backend:** Firebase Authentication and Cloud Firestore
- **Build:** Gradle (Kotlin DSL), GitHub Actions

### Project layout

| Folder (`app/src/main/java/com/example/splitapp/`) | What's in it |
|---|---|
| `domain/` | Pure logic with no Android or Firebase code: money parsing and formatting, balances, settle-up, join and edit decisions, form validation |
| `data/` | Firebase access (`FirebaseService`), notifications, auth error messages |
| `ui/` | Compose screens: login, group list, group (Members / Ledger / Summary), admin, shared confirmation dialog |
| `theme/` | Brand colours, light and dark schemes, typography |

## 🚀 Getting started (developers)

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

### Building and testing from the command line
```bash
./gradlew assembleDebug        # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 161 unit tests (money math, balances, join/edit rules, validation…)
./gradlew lintDebug            # lint
```
Instrumented (on-device) UI tests need an emulator or phone:
```bash
./gradlew connectedDebugAndroidTest   # 22 UI tests (delete confirmation, join dialog, login, admin)
```

### Data compatibility

Phones running older versions read the same Firestore documents, so data changes are **additive only**: never change an existing field's type (`amount` stays a text value such as `"1500.00"`), and new code must still read old documents that lack the newer fields (`splitBetween`, `createdAt`, `createdBy`, member `uid`, `creatorUid`).

## 🔒 Firestore security rules

The app's real protection is its Firestore rules, kept in [`firestore.rules`](firestore.rules) (with [`firebase.json`](firebase.json)) and **deployed**. In short:

- Only a group's members can change it; anyone signed in and not banned can open a group by its code (the code works as an invitation).
- Users can read and edit only their own profile, and can never change their own ban status.
- Banned users can't open or edit groups.
- Only the Super Admin can list all users and groups, ban users and delete groups. The admin is matched by **account uid**, not email. If the admin account is ever re-created, update the uid in `firestore.rules`.

To change the rules: edit `firestore.rules`, test it against every app action in the Firestore emulator, then paste it into **Firebase console → Firestore Database → Rules → Publish**. The console keeps a history of published versions, so you can roll back.

## 📦 Releasing

CI runs on every push to `main` and every pull request (unit tests, lint, debug build) and publishes nothing. A release is built only when you push a version tag:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

The release job signs the APK and publishes it as the latest release, which is what the **Download APK** button links to. It needs four **Repository secrets** (Settings → Secrets and variables → Actions → *Secrets* tab → *Repository secrets*, not Environments or Variables):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the release `.jks` file, base64-encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

The signing key's SHA-1 and SHA-256 fingerprints must be registered in the Firebase console for Google sign-in to work. **Keep the `.jks` file and its passwords backed up outside the repo:** every future update must be signed with the same key, or users would have to uninstall to update. The version code increases automatically with each build.

## 📝 What's new in v1.0.0

- Exact money math; newcomers no longer owe for older expenses; choose who an expense is split between.
- "Are you sure?" before deleting a transaction, removing a member, leaving a group or signing out.
- Two-step join with no duplicate members, leave group, share code, offline-safe group creation.
- Ledger shows newest first with dates and authors; the Summary tab scrolls; clearer add-expense form.
- Scrollable login, password reset, readable errors.
- Admin: confirmations, names instead of ids, exact totals, no broken "Delete User".
- Bans can't be bypassed; security rules deployed on the server.
- One brand theme with working dark mode, real icons, new app icon and the SplitShare name.
- No more connection leaks; no 30-group limit; signed releases.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues).
