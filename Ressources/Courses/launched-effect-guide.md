# LaunchedEffect Complete Guide for Jetpack Compose

## Table of Contents
1. [What is LaunchedEffect?](#what-is-launchedeffect)
2. [Understanding Composition](#understanding-composition)
3. [When to Use LaunchedEffect](#when-to-use-launchedeffect)
4. [LaunchedEffect vs Init Block](#launchedeffect-vs-init-block)
5. [Common Use Cases](#common-use-cases)
6. [Best Practices](#best-practices)

## What is LaunchedEffect?

`LaunchedEffect` is a Composable function that launches a coroutine tied to the composition lifecycle. It's used for side effects that need to happen in response to state changes.

```kotlin
@Composable
fun LaunchedEffect(
    key1: Any?,
    block: suspend CoroutineScope.() -> Unit
)
```

## Understanding Composition

### Composition Lifecycle:
```
1. Composition Enters    → Composable first appears
2. Recomposition        → State changes, UI updates
3. Composition Leaves   → Composable removed from UI
```

### Visual Example:
```kotlin
@Composable
fun MyScreen(userId: String) {
    // This Composable "enters composition" when first shown
    
    LaunchedEffect(userId) {
        // Runs when:
        // 1. MyScreen enters composition (first time shown)
        // 2. userId changes (key change)
        
        loadUserData(userId)
    }
    
    // If MyScreen is removed from UI, it "leaves composition"
    // and the LaunchedEffect coroutine is cancelled
}
```

## When to Use LaunchedEffect

### 1. Initial Data Loading
```kotlin
@Composable
fun UserProfileScreen(userId: String) {
    val viewModel = hiltViewModel<UserViewModel>()
    
    // Load data when screen appears or userId changes
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
}
```

### 2. Navigation Side Effects
```kotlin
@Composable
fun LoginScreen(
    navigator: Navigator,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState(null)
    
    // Handle navigation events
    LaunchedEffect(events) {
        when (events) {
            is LoginEvent.NavigateToHome -> {
                navigator.navigate("home")
            }
            is LoginEvent.ShowError -> {
                // Show error
            }
        }
    }
}
```

### 3. Animations and Timers
```kotlin
@Composable
fun CountdownTimer(seconds: Int) {
    var remainingTime by remember { mutableStateOf(seconds) }
    
    LaunchedEffect(seconds) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
    }
    
    Text("Time: $remainingTime")
}
```

### 4. Observing External Changes
```kotlin
@Composable
fun LocationTracker() {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    
    LaunchedEffect(Unit) {
        val locationFlow = context.locationUpdates()
        locationFlow.collect { newLocation ->
            location = newLocation
        }
    }
}
```

## LaunchedEffect vs Init Block

### ViewModel Init Block:
```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    init {
        // Runs ONCE when ViewModel is created
        // Good for:
        // - Initial setup
        // - Starting observations
        // - Loading default data
        
        loadInitialData()
        observeUserUpdates()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = State.Loading
            val data = repository.getData()
            _state.value = State.Success(data)
        }
    }
}
```

### LaunchedEffect in Composable:
```kotlin
@Composable
fun UserScreen(userId: String) {
    val viewModel = hiltViewModel<UserViewModel>()
    
    // Runs when:
    // - Composable enters composition
    // - userId changes
    // Good for:
    // - Reacting to parameter changes
    // - Side effects based on UI state
    // - Cleanup when leaving composition
    
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
}
```

### Comparison:
```kotlin
// ❌ WRONG - Loading in Composable without LaunchedEffect
@Composable
fun BadExample(userId: String) {
    val viewModel = hiltViewModel<UserViewModel>()
    
    // This runs on EVERY recomposition!
    viewModel.loadUser(userId) // DON'T DO THIS
}

// ✅ CORRECT - Using LaunchedEffect
@Composable
fun GoodExample(userId: String) {
    val viewModel = hiltViewModel<UserViewModel>()
    
    LaunchedEffect(userId) {
        // Only runs when userId changes
        viewModel.loadUser(userId)
    }
}

// ✅ ALSO CORRECT - Loading in ViewModel
@HiltViewModel
class UserViewModel : ViewModel() {
    fun loadUser(userId: String) {
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
            viewModelScope.launch {
                // Load user
            }
        }
    }
}
```

## Understanding Keys in LaunchedEffect

### Single Key:
```kotlin
LaunchedEffect(userId) {
    // Restarts when userId changes
    loadUser(userId)
}
```

### Multiple Keys:
```kotlin
LaunchedEffect(userId, filter) {
    // Restarts when EITHER userId OR filter changes
    loadFilteredUserData(userId, filter)
}
```

### Unit Key (Run Once):
```kotlin
LaunchedEffect(Unit) {
    // Runs once when entering composition
    // Never restarts
    analytics.trackScreenView("UserProfile")
}
```

### True Key (Always Active):
```kotlin
LaunchedEffect(true) {
    // Same as Unit - runs once
    // Use Unit for clarity
}
```

## Common Use Cases

### 1. Collecting Flows
```kotlin
@Composable
fun ErrorHandler(
    viewModel: MyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.errors.collect { error ->
            Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 2. Debounced Search
```kotlin
@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    val viewModel = hiltViewModel<SearchViewModel>()
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(500) // Debounce
            viewModel.search(searchQuery)
        }
    }
    
    TextField(
        value = searchQuery,
        onValueChange = { searchQuery = it }
    )
}
```

### 3. Periodic Updates
```kotlin
@Composable
fun LiveDataScreen() {
    var data by remember { mutableStateOf<Data?>(null) }
    
    LaunchedEffect(Unit) {
        while (true) {
            data = fetchLatestData()
            delay(30_000) // Update every 30 seconds
        }
    }
}
```

### 4. Cleanup Operations
```kotlin
@Composable
fun WebSocketScreen(roomId: String) {
    LaunchedEffect(roomId) {
        val connection = WebSocketConnection(roomId)
        connection.connect()
        
        try {
            connection.messages.collect { message ->
                // Handle message
            }
        } finally {
            // Cleanup when leaving composition or roomId changes
            connection.disconnect()
        }
    }
}
```

## DisposableEffect vs LaunchedEffect

### DisposableEffect for Non-Coroutine Side Effects:
```kotlin
@Composable
fun LocationPermissionHandler() {
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        val listener = LocationListener { location ->
            // Handle location
        }
        
        context.requestLocationUpdates(listener)
        
        onDispose {
            // Cleanup
            context.removeLocationUpdates(listener)
        }
    }
}
```

### LaunchedEffect for Coroutines:
```kotlin
@Composable
fun DataLoader(id: String) {
    LaunchedEffect(id) {
        val data = loadData(id) // Suspend function
        // Use data
    }
}
```

## Best Practices

### 1. Choose the Right Key
```kotlin
// ❌ Wrong - Unnecessary recomposition
LaunchedEffect(Math.random()) {
    // This runs on EVERY recomposition!
}

// ✅ Correct - Stable key
LaunchedEffect(userId) {
    // Runs only when userId changes
}
```

### 2. Avoid Capturing Unstable References
```kotlin
// ❌ Wrong - Captures changing lambda
@Composable
fun BadExample() {
    LaunchedEffect(Unit) {
        val onClick = { /* ... */ } // New lambda each time
        someApi.setClickListener(onClick)
    }
}

// ✅ Correct - Stable reference
@Composable
fun GoodExample() {
    val onClick = remember { { /* ... */ } }
    
    LaunchedEffect(Unit) {
        someApi.setClickListener(onClick)
    }
}
```

### 3. Handle Cancellation
```kotlin
LaunchedEffect(key) {
    try {
        while (isActive) { // Check if coroutine is active
            val data = fetchData()
            processData(data)
            delay(1000)
        }
    } catch (e: CancellationException) {
        // Handle cancellation
        cleanup()
        throw e // Re-throw to properly cancel
    }
}
```

### 4. Prefer ViewModel for Business Logic
```kotlin
// ❌ Avoid complex logic in LaunchedEffect
@Composable
fun BadExample(userId: String) {
    LaunchedEffect(userId) {
        val user = repository.getUser(userId)
        val processed = processUser(user)
        val validated = validateUser(processed)
        // Too much logic here
    }
}

// ✅ Keep LaunchedEffect simple
@Composable
fun GoodExample(userId: String) {
    val viewModel = hiltViewModel<UserViewModel>()
    
    LaunchedEffect(userId) {
        viewModel.loadUser(userId) // Logic in ViewModel
    }
}
```

## Common Patterns

### Pattern 1: Event Handling
```kotlin
@Composable
fun EventHandlingScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState(null)
    
    LaunchedEffect(events) {
        events?.let { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
            viewModel.clearEvent()
        }
    }
}
```

### Pattern 2: Initialization
```kotlin
@Composable
fun InitializedScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    // One-time initialization
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    // Or with cleanup
    DisposableEffect(Unit) {
        val connection = viewModel.connect()
        
        onDispose {
            connection.close()
        }
    }
}
```

### Pattern 3: Reactive Updates
```kotlin
@Composable
fun ReactiveScreen(
    dependency: String,
    viewModel: MyViewModel = hiltViewModel()
) {
    // React to dependency changes
    LaunchedEffect(dependency) {
        viewModel.onDependencyChanged(dependency)
    }
}
```

## Summary

LaunchedEffect is essential for:
- Side effects in Compose
- Bridging coroutines with UI
- Reacting to state changes
- Managing lifecycle-aware operations

Remember:
- Use appropriate keys
- Keep effects simple
- Prefer ViewModel for logic
- Handle cancellation properly