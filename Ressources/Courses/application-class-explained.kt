// 🏗️ StarWarsApplication - The Foundation of Your App

@HiltAndroidApp  // This tiny annotation does HUGE work!
class StarWarsApplication : Application()  // Extends Android's Application class

// 🤔 WHAT IS THE APPLICATION CLASS?
// - First thing created when your app starts
// - Lives for the entire app lifetime
// - Only one instance ever exists
// - Created before ANY activity, service, or receiver

// 🎯 WHAT @HiltAndroidApp DOES:
// This annotation triggers Hilt's code generation:

// 1. Generates: Hilt_StarWarsApplication class
// 2. Your class actually becomes:
//    class StarWarsApplication : Hilt_StarWarsApplication()

// 3. The generated class does this:
abstract class Hilt_StarWarsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Hilt's dependency injection system
        // Create the SingletonComponent
        // Set up all @Module classes
        // Prepare to inject into @AndroidEntryPoint classes
    }
}

// 🔄 THE COMPLETE HILT INITIALIZATION FLOW:

// STEP 1: App launches
// Android creates StarWarsApplication instance

// STEP 2: @HiltAndroidApp triggers
// Hilt initializes its dependency graph:
/*
    SingletonComponent (lives forever)
    ├── NetworkModule
    │   ├── provideOkHttpClient()
    │   └── provideApolloClient()
    ├── DatabaseModule
    │   ├── provideDatabase()
    │   └── provideDAOs()
    └── RepositoryModule
        └── provideRepositories()
*/

// STEP 3: MainActivity launches with @AndroidEntryPoint
// Hilt creates ActivityComponent (child of SingletonComponent)

// STEP 4: ViewModel with @HiltViewModel requested
// Hilt injects dependencies automatically

// 💡 WHY THIS ARCHITECTURE?
// Without Application class + Hilt:
class MyActivity : ComponentActivity() {
    override fun onCreate(...) {
        // Create everything manually
        val okHttpClient = OkHttpClient.Builder()...
        val apolloClient = ApolloClient.Builder()...
        val database = Room.databaseBuilder()...
        val dao = database.characterDao()
        val repository = CharacterRepository(apolloClient, dao)
        val viewModel = CharacterViewModel(repository)
        // 😱 Do this in EVERY activity!
    }
}

// With Application class + Hilt:
@HiltAndroidApp
class StarWarsApplication : Application()
// That's it! Everything else is automatic!

// 📚 YOU CAN ALSO USE APPLICATION CLASS FOR:
@HiltAndroidApp
class StarWarsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app-wide services
        initializeAnalytics()
        initializeCrashReporting()
        initializeTimber() // Logging
        
        // Set up global configuration
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }
    
    // Global app state if needed
    companion object {
        lateinit var instance: StarWarsApplication
            private set
    }
    
    init {
        instance = this
    }
}