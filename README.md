# Hermatic

Hermatic is a secure, private Android client for the **Hermes Agent** API. It focuses on providing a top-notch secure environment for interacting with the Hermes LLM agent.

## Features

- **OpenAI Compatible**: Seamlessly integrates with the Hermes Agent API server.
- **Top-Notch Security**: 
    - API keys are stored using **Android Security Crypto** (`EncryptedSharedPreferences`) with AES-256 encryption.
    - Automatic **Bearer Token** injection via OkHttp Interceptors.
    - Strict HTTPS communication.
- **Modern UI**: Built entirely with **Jetpack Compose** for a smooth, reactive user experience.
- **Clean Architecture**: Follows MVVM (Model-View-ViewModel) and Repository patterns for better maintainability and testability.
- **Stateless/Stateful Chat**: Supports standard chat completions with conversation history management.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Security**: [AndroidX Security-Crypto](https://developer.android.com/jetpack/androidx/releases/security)
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android API Level 24+
- A Hermes Agent API Key (defined via `API_SERVER_KEY` on your Hermes server)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hermatic.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on an emulator or physical device.

### Configuration
On first launch, the app will prompt you for your **Hermes API Key**. This key is stored securely in the device's hardware-backed Keystore via `EncryptedSharedPreferences`.

## Project Structure

- `data/api`: Retrofit interfaces and OkHttp configuration.
- `data/model`: Data classes for API requests and responses.
- `data/repository`: Business logic for data operations.
- `security`: Security managers and encryption logic.
- `ui`: Jetpack Compose screens and themes.

## Security Roadmap

- [ ] **Biometric Authentication**: Lock the app behind fingerprint/face unlock.
- [ ] **Certificate Pinning**: Prevent MITM attacks by pinning the server certificate.
- [ ] **Privacy Screen**: Obscure app content in the recent apps switcher.
- [ ] **Local Encryption**: Encrypt local message history database.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Note: This app is purely a client for the [Hermes Agent](https://hermes-agent.nousresearch.com/).*
