// 🎬 THE COMPLETE APP STARTUP FLOW

// 1️⃣ USER TAPS YOUR APP ICON
// Android reads AndroidManifest.xml

// 2️⃣ ANDROID CREATES APPLICATION
// From manifest: android:name=".StarWarsApplication"
@HiltAndroidApp
class StarWarsApplication : Application() {
    // Hilt initializes:
    // - Creates SingletonComponent
    // - Scans for @Module classes
    // - Builds dependency graph
    // - Ready to inject!
}

// 3️⃣ ANDROID LAUNCHES MAIN ACTIVITY
// From manifest: intent-filter with MAIN/LAUNCHER
@AndroidEntryPoint  // Tells Hilt: "I need dependencies!"
class MainActivity : ComponentActivity() {
    // Hilt creates ActivityComponent (child of SingletonComponent)
    // Ready to inject into this activity or its ViewModels
}

// 4️⃣ COMPOSE UI RENDERS
setContent {
    StarWarsTheme {
        StarWarsNavigation()  // Bottom nav + screens
    }
}

// 5️⃣ USER NAVIGATES TO CHARACTERS SCREEN
@Composable
fun CharactersListScreen(
    // Compose requests ViewModel
    viewModel: CharactersListViewModel = hiltViewModel()
    // hiltViewModel() asks Hilt for the ViewModel
)

// 6️⃣ HILT CREATES VIEWMODEL WITH DEPENDENCIES
@HiltViewModel  // Tells Hilt how to create this
class CharactersListViewModel @Inject constructor(
    private val repository: CharacterRepository  // Hilt provides this!
) : ViewModel()

// 7️⃣ HILT CREATES REPOSITORY
class CharacterRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,  // From NetworkModule
    private val characterDao: CharacterDao   // From DatabaseModule
) : CharacterRepository

// 8️⃣ THE DEPENDENCY CHAIN
/*
    hiltViewModel() requests CharactersListViewModel
           ↓
    Hilt needs CharacterRepository
           ↓
    Hilt needs ApolloClient + CharacterDao
           ↓
    NetworkModule provides ApolloClient
    DatabaseModule provides CharacterDao
           ↓
    Everything injected automatically!
*/

// 🎯 THE COMPLETE CONNECTION:

// AndroidManifest.xml says:
// "Use StarWarsApplication as the app class"
// "Launch MainActivity when app starts"

// StarWarsApplication says:
// "Initialize Hilt for the whole app"

// MainActivity says:
// "I'm a Hilt-aware activity, set up Compose UI"

// Compose Navigation says:
// "Show these screens with bottom navigation"

// ViewModels say:
// "I need these dependencies to work"

// Hilt says:
// "I know how to create everything, here you go!"

// 🚨 COMMON ISSUES AND FIXES:

// Issue 1: "Did you forget to add @HiltAndroidApp?"
// Fix: Make sure StarWarsApplication has @HiltAndroidApp

// Issue 2: "lateinit property has not been initialized"
// Fix: Add @AndroidEntryPoint to your Activity/Fragment

// Issue 3: "[Dagger/MissingBinding] Cannot be provided"
// Fix: Create a @Module with @Provides for that type

// Issue 4: App crashes on startup
// Fix: Add android:name=".StarWarsApplication" to manifest!