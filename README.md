<h1 align="center">Expense Splitter 💸</h1>

<p align="center">
  A sleek, modern Android application built to simplify group expenses. Features an itemized ledger system to track both group expenses and direct peer-to-peer transfers, automatically calculating the minimal transactions needed to settle up.
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

- **Multi-User Authentication:** Fast and secure user accounts utilizing email and password via Firebase Authentication.
- **Real-Time Syncing:** All expenses, group members, and transfers are synced instantly using Google Cloud Firestore, keeping all users up to date dynamically.
- **Create & Join Groups:** Share unique, generated group codes to easily invite friends and family to join your ledger.
- **Itemized Ledger System:** Record specific expenses (e.g. "Movie Tickets") and specify who paid for them.
- **Peer-to-Peer Transfers:** Log direct cash advances or payments from one person to another (e.g. "Person A gave Person B an advance of ₹1500").
- **Smart Settlements:** Automatically calculates the total group expenditure, balances out all expenses and transfers, and utilizes a greedy algorithm to output the absolute minimal number of direct transactions required to square up everyone.
- **Dynamic Members:** Easily add and manage group members dynamically.
- **Premium Material 3 UI:** Features a gorgeous, card-based interface with sliding filter chips, bottom sheet dialogs, and a clean three-tab navigation layout built entirely in Jetpack Compose.

## 📸 Screenshots

*(Add screenshots of your app here)*
<!-- 
<p align="center">
  <img src="link_to_screenshot_1.png" width="30%">
  <img src="link_to_screenshot_2.png" width="30%">
</p> 
-->

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture/Design:** Material Design 3 (M3)
- **Build System:** Gradle (Kotlin DSL)

## 🚀 Getting Started

### Prerequisites
- Android Studio (Jellyfish or newer recommended)
- JDK 17+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/redmas45/Split_app.git
   ```
2. Open the project in **Android Studio**.
3. Sync the project with Gradle files.
4. Run the app on an emulator or a physical Android device.

### Building from Command Line
To build a debug APK directly from your terminal:
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues).

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
