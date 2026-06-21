# Hermatic

Hermatic is a high-security, premium AI workstation for the **Hermes Agent** API. It provides a structured, "Noir" aesthetic inspired by the Nous Research portal, focused on privacy, technical control, and agentic workflows.

## Key Features

- **OpenAI Compatible**: Seamless integration with Hermes Agent and other OpenAI-spec servers.
- **Top-Notch Security**: 
    - **Biometric Lock**: Optional fingerprint/face unlock with auto-lock when backgrounded.
    - **Encrypted Local Storage**: Persistent chat history encrypted with AES-256 (SQLCipher).
    - **Hardware-Backed Keystore**: API keys stored in encrypted shards.
    - **Privacy Screen**: Content obscured in multitasking menu (FLAG_SECURE).
    - **Panic Wipe**: One-tap "Wipe All Data" button for technical containment.
- **Advanced Agent Workflow**:
    - **Multi-Session Support**: Manage independent conversation threads with siloed memory.
    - **Live Agent Thoughts**: Pulsing technical indicators for agent processing states.
    - **Vision & Multimedia**: Support for image attachments and visual analysis.
    - **Skills Discovery**: Dedicated screen to explore agent toolsets and technical capabilities.
- **Premium Noir UI/UX**:
    - **Dynamic Theme System**: Monochrome by default with primary/accent color pickers.
    - **Noisy Ambient Background**: Optimized, organic blurred field with a tactile film-grain texture.
    - **Markdown & Code**: Full rich-text rendering with IDE-style formatting.
    - **Responsive Design**: Adaptive layouts for mobile and tablet form factors.

## Tech Stack

- **Language**: Kotlin 2.2.10
- **UI**: Jetpack Compose (Material 3)
- **Networking**: Retrofit & OkHttp (180s technical timeouts)
- **Database**: Room + SQLCipher (KSP)
- **Image Loading**: Coil
- **Markdown**: Multiplatform Markdown Renderer
- **Security**: AndroidX Biometric & Security-Crypto

## Getting Started

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/EssByte/Hermatic.git
   ```
2. Open in Android Studio.
3. Configure your **BASE_URL** and **API_KEY** on the initialization screen.

### Connection Tips
- For local development in an emulator, use `http://10.0.2.2:PORT/`.
- Ensure your node is listening on `0.0.0.0` for external device access.

## Security Roadmap

- [x] Biometric & PIN Lock
- [x] SQLCipher Encryption
- [x] Multi-Session Management
- [x] Vision Support
- [ ] **Certificate Pinning**: Hardening against MITM attacks.
- [ ] **Zero-Log Mode**: Ephemeral sessions with no local trace.

## License

MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed for the Nous Research ecosystem.*
