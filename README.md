# BINGO - Android Multiplayer Game

A custom 2-player real-time multiplayer Bingo game built with modern Android practices. This is not traditional Bingo; it uses a 5x5 grid with numbers 1-25 and custom line completion rules.

## Features

- **Real-time 1v1 Multiplayer**: Powered by Firebase Firestore for seamless state synchronization.
- **Secure Board Privacy**: Secret board arrangements are protected using Firestore Security Rules and remain hidden from opponents until the game ends.
- **Warm Coral & Amber Theme**: A high-contrast, professional Material 3 visual identity with full Light and Dark mode support.
- **Preset Boards**: Save and manage up to 6 custom board layouts with miniature previews and duplicate detection.
- **Accessible Gameplay**: Call history is encoded with both color and text styles (Bold/Underline vs Italic) for better readability.
- **Haptic Feedback**: Tactile vibrations during board setup and gameplay.
- **Interactive Guide**: A 7-step "How to Play" slideshow to onboard new players.

## Architecture

The project follows Clean Architecture principles with a focus on modularity and maintainability:
- **UI**: Jetpack Compose with Material 3.
- **State Management**: ViewModel with StateFlow.
- **Networking**: Firebase Firestore and Anonymous Authentication.
- **Game Engine**: A dedicated Kotlin-based engine for line detection and BINGO progression.

## Setup & Requirements

### Firebase Configuration
1. Create a Firebase project at the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Anonymous Authentication** in the Authentication section.
3. Enable **Cloud Firestore** in production mode.
4. Add an Android app with the package name `com.ishaan.bingo`.
5. Download `google-services.json` and place it in the `app/` directory.
6. Apply the security rules found in `firestore.rules` to your Firestore project.

### Development Environment
- Latest Android Studio.
- Minimum SDK: 26.
- Compile SDK: 37.

## How to Play
1. **Lobby**: Create or Join a game using a 5-character code.
2. **Setup**: Place numbers 1-25 in your secret grid.
3. **Gameplay**: Call numbers by tapping your board. Both players cross out called numbers.
4. **BINGO**: Complete a row, column, or diagonal to earn a letter.
5. **Victory**: The first player to earn all five letters (B-I-N-G-O) wins.

---
Created for educational and recreational purposes.
