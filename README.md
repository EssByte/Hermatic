# Hermatic

Hermatic is a secure, private Android client for the **Hermes Agent** API. It focuses on providing a top-notch secure environment for interacting with the Hermes LLM agent.

## Features

- **OpenAI Compatible**: Seamlessly integrates with the Hermes Agent API server.
- **Top-Notch Security**: 
    - **Biometric Authentication**: App access is protected by fingerprint or face unlock.
    - **Encrypted Local Storage**: Chat history is stored in an encrypted Room database using **SQLCipher**.
    - **API Key Protection**: Keys are stored using **Android Security Crypto** (`EncryptedSharedPreferences`) with hardware-backed AES-256 encryption.
    - **Privacy Screen**: App content is automatically obscured in the multitasking/recents menu (FLAG_SECURE).
    - **Automatic Authentication**: Bearer Token injection via OkHttp Interceptors.
- **Streaming Responses**: Real-time word-by-word bot responses for a responsive experience.
- **Modern UI**: Built entirely with **Jetpack Compose** for a smooth, reactive user experience.
- **Clean Architecture**: Follows MVVM (Model-View-ViewModel) and Repository patterns.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) with [SQLCipher](https://www.zetetic.net/sqlcipher/)
- **Security**: [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) & [Security-Crypto](https://developer.android.com/jetpack/androidx/releases/security)
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android API Level 24+
- A Hermes Agent API Key (defined via `API_SERVER_KEY` on your Hermes server)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/EssByte/Hermatic.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on an emulator or physical device.

### Configuration
On first launch, the app will require **Biometric Authentication**. After unlocking, you will be prompted for your **Hermes API Key**. All sensitive data is stored securely in the device's hardware-backed Keystore.

## Project Structure

- `data/api`: Retrofit interfaces, SSE streaming logic, and OkHttp configuration.
- `data/db`: Encrypted Room database and DAO definitions.
- `data/model`: Data classes for API models and DB entities.
- `data/repository`: Business logic and history management.
- `security`: Biometric helpers and encryption managers.
- `ui`: Jetpack Compose screens and themes.

## Security Roadmap

- [x] **Biometric Authentication**
- [x] **Privacy Screen**
- [x] **Local Encryption** (SQLCipher)
- [ ] **Certificate Pinning**: Prevent MITM attacks by pinning the server certificate.
- [ ] **Self-Destruct Messages**: Option to automatically clear history after a set period.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Note: This app is purely a client for the [Hermes Agent](https://hermes-agent.nousresearch.com/).*
