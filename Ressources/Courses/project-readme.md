# Star Wars Reference App 🌟

A modern Android application built with Jetpack Compose that serves as a comprehensive reference for the Star Wars universe. Browse characters, starships, and planets from the iconic saga with a beautiful, responsive UI.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

## 📱 Features

### Core Functionality
- **Three Main Lists**: Browse People, Starships, and Planets
- **Detailed Information**: Tap any item to view comprehensive details
- **Favorites System**: Mark your favorite characters, ships, and planets
- **Offline Support**: Data cached locally for offline viewing
- **Smart Pagination**: Efficient loading of large datasets
- **Pull to Refresh**: Stay up-to-date with the latest data
- **Search & Sort**: Find items quickly with multiple sorting options

### UI/UX
- **Material 3 Design**: Modern, consistent UI following latest design guidelines
- **Dark/Light Theme**: Automatic theme switching based on system settings
- **Smooth Animations**: Delightful transitions and loading states
- **Responsive Layout**: Optimized for phones and tablets
- **Landscape Support**: Adaptive UI with navigation rail in landscape

## 🏗️ Architecture

This app follows **Clean Architecture** principles with clear separation of concerns:

```
app/
├── _core/                          # Core functionality
│   ├── boilerplate/               # Base classes (BaseViewModel, MVI)
│   ├── data/                      # Data layer
│   │   ├── local/                 # Database (Room)
│   │   ├── remote/                # Network (Apollo GraphQL)
│   │   ├── mapper/                # Entity ↔ Domain mappers
│   │   └── service/               # Services (Apollo, DataStore)
│   ├── domain/                    # Domain layer
│   │   ├── model/                 # Domain models
│   │   ├── error/                 # Error handling
│   │   └── repository/            # Repository interfaces
│   └── presentation/              # Presentation layer
│       ├── components/            # Reusable UI components
│       ├── ui/                    # Theme, colors, typography
│       └── utils/                 # UI utilities
│
└── screens/                       # Feature modules
    ├── people/
    │   ├── list/                  # List screen
    │   │   ├── data/             # Repository implementation
    │   │   ├── domain/           # Use cases & interfaces
    │   │   └── presentation/     # UI & ViewModel
    │   └── detail/               # Detail screen
    ├── starship/                 # Same structure
    └── planet/                   # Same structure
```

### Architecture Components

- **MVI Pattern**: Unidirectional data flow with Intents, States, and Events
- **Repository Pattern**: Abstraction between data sources and domain
- **Dependency Injection**: Hilt for compile-time safe DI
- **Coroutines & Flow**: Reactive programming with Kotlin coroutines
- **Paging 3**: Efficient data loading and display

## 🛠️ Tech Stack

### Core
- **Language**: [Kotlin](https://kotlinlang.org/) 2.0.21
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) 2025.06.01
- **Min SDK**: 30 (Android 11)
- **Target SDK**: 35 (Android 15)

### Libraries
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) 2.52
- **Navigation**: [Compose Destinations](https://github.com/raamcosta/compose-destinations) 1.9.54
- **Database**: [Room](https://developer.android.com/training/data-storage/room) 2.7.2
- **Network**: [Apollo GraphQL](https://www.apollographql.com/docs/kotlin/) 3.8.2
- **Async**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.8.0
- **Pagination**: [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) 3.3.6
- **Local Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.7

### API
- **GraphQL Endpoint**: [SWAPI GraphQL](https://swapi-graphql.eskerda.vercel.app/)

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android SDK with API 35

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/star-wars-app.git
   cd star-wars-app
   ```

2. Open in Android Studio:
   ```bash
   studio .
   ```

3. Sync project with Gradle files

4. Run the app:
   - Select a device/emulator
   - Click "Run" or press `Shift+F10`

## 📋 Required Tasks Implementation

### ✅ Part 1: List Views
- Three main lists (People, Starships, Planets) with bottom navigation
- Each item displays name and film count
- Smooth scrolling with item animations

### ✅ Part 2: Pagination
- Cursor-based pagination using GraphQL
- RemoteMediator for network + database sync
- Efficient loading with Paging 3

### ✅ Part 3: Detail View
- Comprehensive details for each item type
- At least 6 additional attributes per item
- Beautiful card-based layout

### ✅ Part 4: Favorites
- Toggle favorites on list items and detail screens
- Filter to show only favorites
- Persistent storage across app sessions

## 🧪 Testing

Run tests with:
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Test Coverage
- **Unit Tests**: ViewModels, Repositories, Use Cases
- **UI Tests**: Composable components, Navigation
- **Integration Tests**: Database operations, API calls

## 🎨 Design Decisions

### State Management
- **StateFlow** for UI state (always has value)
- **SharedFlow** for one-time events
- **Paging 3** for list data
- **DataStore** for preferences

### Error Handling
- Sealed classes for type-safe errors
- Graceful fallbacks for network issues
- User-friendly error messages

### Performance
- Lazy loading with LazyColumn
- Image caching and optimization
- Proper coroutine scope management
- Memory-efficient pagination

## 📝 Code Style

This project follows:
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Clean Architecture principles
- SOLID principles

## 🔧 Configuration

### Build Variants
- **Debug**: Development build with logging
- **Release**: Optimized production build

### ProGuard
Release builds are optimized with R8/ProGuard. Configuration in `proguard-rules.pro`.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [SWAPI](https://swapi.dev/) for the Star Wars data
- [Star Wars](https://www.starwars.com/) by Lucasfilm Ltd.
- Android community for amazing libraries and tools

## 📞 Contact

For questions or feedback, please open an issue on GitHub.

---

<p align="center">Made with ❤️ for Star Wars fans</p>
<p align="center">May the Force be with you! 🌟</p>