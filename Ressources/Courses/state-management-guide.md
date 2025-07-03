# State Management Complete Guide for Jetpack Compose

## Table of Contents
1. [Understanding State in Compose](#understanding-state-in-compose)
2. [State Hoisting](#state-hoisting)
3. [Remember and State Types](#remember-and-state-types)
4. [ViewModel State Management](#viewmodel-state-management)
5. [Flow vs State in Compose](#flow-vs-state-in-compose)
6. [Advanced State Patterns](#advanced-state-patterns)
7. [Best Practices](#best-practices)

## Understanding State in Compose

### What is State?
State in Compose is any value that can change over time and trigger recomposition when it changes.

```kotlin
// State change triggers recomposition
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    
    Button(onClick = { count++ }) {
        Text("Count: $count") // Recomposes when count changes
    }
}
```

### Recomposition Flow:
```
State Change → Recomposition → UI Update

Initial:     count = 0 → Compose → [Count: 0]
Click:       count = 1 → Recompose → [Count: 1]
Click:       count = 2 → Recompose → [Count: 2]
```

## State Hoisting

### What is State Hoisting?
Moving state up to make composables stateless and reusable.

```kotlin
// ❌ Stateful Composable - Hard to test and reuse
@Composable
fun StatefulCounter() {
    var count by remember { mutableStateOf(0) }
    
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}

// ✅ Stateless Composable - Easy to test and reuse
@Composable
fun StatelessCounter(
    count: Int,
    onIncrement: () -> Unit
) {
    Button(onClick = onIncrement) {
        Text("Count: $count")
    }
}

// Parent manages state
@Composable
fun CounterScreen() {
    var count by remember { mutableStateOf(0) }
    
    StatelessCounter(
        count = count,
        onIncrement = { count++ }
    )
}
```

### State Hoisting Patterns:
```kotlin
// Level 1: Local State
@Composable
fun LocalStateExample() {
    var text by remember { mutableStateOf("") }
    TextField(value = text, onValueChange = { text = it })
}

// Level 2: Hoisted to Parent
@Composable
fun ParentScreen() {
    var text by remember { mutableStateOf("") }
    ChildComponent(text = text, onTextChange = { text = it })
}

// Level 3: ViewModel State
@Composable
fun ViewModelScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    ChildComponent(
        text = state.text,
        onTextChange = viewModel::updateText
    )
}
```

## Remember and State Types

### 1. remember - Survives Recomposition
```kotlin
@Composable
fun RememberExample() {
    // Survives recomposition, lost on configuration change
    val calculation = remember { expensiveCalculation() }
    
    // With keys - recalculates when key changes
    val filtered = remember(searchQuery) {
        list.filter { it.contains(searchQuery) }
    }
}
```

### 2. rememberSaveable - Survives Configuration Changes
```kotlin
@Composable
fun SaveableExample() {
    // Survives configuration changes (rotation)
    var text by rememberSaveable { mutableStateOf("") }
    
    // Custom saver for complex objects
    val customObject = rememberSaveable(
        saver = CustomObjectSaver
    ) { CustomObject() }
}
```

### 3. mutableStateOf - Observable State
```kotlin
// Different ways to use mutableStateOf
@Composable
fun StateExamples() {
    // Destructured
    val (text, setText) = remember { mutableStateOf("") }
    
    // By delegate
    var count by remember { mutableStateOf(0) }
    
    // Direct access
    val state = remember { mutableStateOf(User()) }
    Text(state.value.name)
    state.value = state.value.copy(name = "New Name")
}
```

### 4. derivedStateOf - Computed State
```kotlin
@Composable
fun DerivedStateExample(items: List<Item>) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Only recalculates when searchQuery or items change
    val filteredItems by remember {
        derivedStateOf {
            items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
}
```

### 5. produceState - Async State
```kotlin
@Composable
fun ProduceStateExample(userId: String) {
    val user by produceState<User?>(initialValue = null, userId) {
        value = repository.getUser(userId)
    }
    
    user?.let { UserContent(it) }
}
```

## ViewModel State Management

### StateFlow Pattern:
```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }
}

// In Compose
@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    UserContent(
        state = uiState,
        onNameChange = viewModel::updateName
    )
}
```

### Combined State Pattern:
```kotlin
data class ScreenState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScreenState())
    val state = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val user = async { repository.getUser() }
                val posts = async { repository.getPosts() }
                
                _state.update {
                    it.copy(
                        user = user.await(),
                        posts = posts.await(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
}
```

## Flow vs State in Compose

### Different Collection Methods:
```kotlin
@Composable
fun FlowCollectionExamples(viewModel: MyViewModel) {
    // 1. collectAsState - Basic collection
    val state by viewModel.stateFlow.collectAsState()
    
    // 2. collectAsStateWithLifecycle - Lifecycle aware
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    
    // 3. With initial value
    val state by viewModel.flow.collectAsState(initial = DefaultState())
    
    // 4. Multiple flows combined
    val state by combine(
        viewModel.userFlow,
        viewModel.postsFlow
    ) { user, posts ->
        ScreenState(user, posts)
    }.collectAsState(initial = ScreenState())
}
```

### When to Use Each:
```kotlin
// StateFlow - For UI state
class ViewModel {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow() // Always has value
}

// SharedFlow - For events
class ViewModel {
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow() // No initial value
}

// Flow - For data streams
class ViewModel {
    val searchResults = searchQuery
        .debounce(300)
        .flatMapLatest { repository.search(it) }
}
```

## Advanced State Patterns

### 1. State Restoration:
```kotlin
@Parcelize
data class ScreenState(
    val text: String = "",
    val selectedId: Int = 0
) : Parcelable

@Composable
fun RestorationExample() {
    var state by rememberSaveable {
        mutableStateOf(ScreenState())
    }
    
    // State survives process death
}
```

### 2. Shared State Between Screens:
```kotlin
// Shared ViewModel scoped to navigation graph
@Composable
fun ScreenA(
    sharedViewModel: SharedViewModel = hiltViewModel(
        navBackStackEntry<NavGraph>()
    )
) {
    // Access shared state
}

@Composable
fun ScreenB(
    sharedViewModel: SharedViewModel = hiltViewModel(
        navBackStackEntry<NavGraph>()
    )
) {
    // Same instance as ScreenA
}
```

### 3. Complex State Transformations:
```kotlin
@Composable
fun ComplexStateExample(viewModel: ViewModel) {
    val userState by viewModel.userFlow.collectAsState(null)
    val postsState by viewModel.postsFlow.collectAsState(emptyList())
    val filtersState by viewModel.filtersFlow.collectAsState(Filters())
    
    // Derived state from multiple sources
    val screenState by remember {
        derivedStateOf {
            when {
                userState == null -> ScreenState.Loading
                postsState.isEmpty() -> ScreenState.Empty
                else -> ScreenState.Content(
                    user = userState,
                    posts = postsState.filter { filtersState.apply(it) }
                )
            }
        }
    }
}
```

### 4. Animated State:
```kotlin
@Composable
fun AnimatedStateExample(isExpanded: Boolean) {
    // Animated state change
    val height by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 100.dp,
        animationSpec = spring()
    )
    
    Box(modifier = Modifier.height(height))
}
```

## Best Practices

### 1. State Should Be Immutable:
```kotlin
// ❌ Bad - Mutable state
data class BadState(
    var items: MutableList<Item>
)

// ✅ Good - Immutable state
data class GoodState(
    val items: List<Item>
)

// Update immutably
_state.update { it.copy(items = it.items + newItem) }
```

### 2. Single Source of Truth:
```kotlin
// ❌ Bad - Multiple sources
@Composable
fun BadExample() {
    var localText by remember { mutableStateOf("") }
    val viewModelText by viewModel.text.collectAsState()
    // Which one is correct?
}

// ✅ Good - Single source
@Composable
fun GoodExample() {
    val text by viewModel.text.collectAsState()
    TextField(
        value = text,
        onValueChange = viewModel::updateText
    )
}
```

### 3. Minimize State:
```kotlin
// ❌ Bad - Redundant state
data class BadState(
    val items: List<Item>,
    val itemCount: Int, // Redundant!
    val hasItems: Boolean // Redundant!
)

// ✅ Good - Minimal state
data class GoodState(
    val items: List<Item>
) {
    val itemCount get() = items.size
    val hasItems get() = items.isNotEmpty()
}
```

### 4. Handle All State Cases:
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

@Composable
fun StateHandling(state: UiState<List<Item>>) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Success -> ItemList(state.data)
        is UiState.Error -> ErrorView(state.exception)
    }
}
```

### 5. State Stability:
```kotlin
// ❌ Bad - Unstable state
@Composable
fun UnstableExample(items: List<Item>) {
    val filtered = items.filter { it.isActive } // New list every time!
    ItemList(filtered)
}

// ✅ Good - Stable state
@Composable
fun StableExample(items: List<Item>) {
    val filtered = remember(items) {
        items.filter { it.isActive }
    }
    ItemList(filtered)
}
```

## Testing State

### Testing Composables with State:
```kotlin
@Test
fun testStatefulComposable() {
    composeTestRule.setContent {
        var count by remember { mutableStateOf(0) }
        Counter(
            count = count,
            onIncrement = { count++ }
        )
    }
    
    composeTestRule.onNodeWithText("Count: 0").assertIsDisplayed()
    composeTestRule.onNodeWithText("Increment").performClick()
    composeTestRule.onNodeWithText("Count: 1").assertIsDisplayed()
}
```

### Testing ViewModels:
```kotlin
@Test
fun testViewModel() = runTest {
    val viewModel = MyViewModel(repository)
    
    // Test initial state
    assertEquals(UiState.Loading, viewModel.state.value)
    
    // Test state updates
    advanceUntilIdle()
    assertEquals(UiState.Success(data), viewModel.state.value)
}
```

## Summary

Key Concepts:
- State triggers recomposition
- Hoist state for reusability
- Use appropriate state holders
- Single source of truth
- Immutable state updates
- Handle all state cases

Choose the right tool:
- `remember` for recomposition survival
- `rememberSaveable` for configuration changes
- `StateFlow` for UI state
- `SharedFlow` for events
- `derivedStateOf` for computed values