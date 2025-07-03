# Coroutines and Flow Complete Guide for Android

## Table of Contents
1. [Coroutines Basics](#coroutines-basics)
2. [Coroutine Builders](#coroutine-builders)
3. [Coroutine Context and Dispatchers](#coroutine-context-and-dispatchers)
4. [Structured Concurrency](#structured-concurrency)
5. [Flow Basics](#flow-basics)
6. [Flow Operators](#flow-operators)
7. [StateFlow and SharedFlow](#stateflow-and-sharedflow)
8. [Best Practices](#best-practices)

## Coroutines Basics

### What is a Coroutine?
A coroutine is a suspendable computation - code that can be paused and resumed without blocking a thread.

```kotlin
// Regular function - blocks thread
fun loadData(): Data {
    Thread.sleep(1000) // Blocks thread for 1 second
    return Data()
}

// Suspend function - doesn't block thread
suspend fun loadData(): Data {
    delay(1000) // Suspends coroutine, thread is free
    return Data()
}
```

### Visual Representation:
```
Thread Timeline with Blocking:
Thread 1: [---loadData (blocked)---][next task]

Thread Timeline with Coroutines:
Thread 1: [start loadData][other work][resume loadData][next task]
                    ↓ suspend          ↑ resume
```

## Coroutine Builders

### 1. launch - Fire and Forget
```kotlin
// Starts a coroutine that doesn't return a value
val job = scope.launch {
    doSomething()
}

// Can be cancelled
job.cancel()

// Can wait for completion
job.join()
```

### 2. async - Returns a Value
```kotlin
// Starts a coroutine that returns a value
val deferred = scope.async {
    computeValue()
}

// Get the result
val result = deferred.await()
```

### 3. runBlocking - Blocks Current Thread
```kotlin
// Blocks thread until completion - avoid in production
runBlocking {
    delay(1000)
}
```

### Parallel vs Sequential Execution:
```kotlin
// Sequential - takes 2 seconds
suspend fun sequential() {
    val user = fetchUser()      // 1 second
    val posts = fetchPosts()    // 1 second
}

// Parallel - takes 1 second
suspend fun parallel() = coroutineScope {
    val userDeferred = async { fetchUser() }     // Starts immediately
    val postsDeferred = async { fetchPosts() }   // Starts immediately
    
    val user = userDeferred.await()
    val posts = postsDeferred.await()
}
```

## Coroutine Context and Dispatchers

### Dispatchers Determine Thread:
```kotlin
// Main thread - UI operations
withContext(Dispatchers.Main) {
    updateUI()
}

// Background thread - CPU intensive work
withContext(Dispatchers.Default) {
    sortLargeList()
}

// IO thread pool - Network/Database
withContext(Dispatchers.IO) {
    fetchFromNetwork()
}

// Unconfined - Starts in caller thread
withContext(Dispatchers.Unconfined) {
    // Use carefully
}
```

### Visual Thread Switching:
```
Main Thread:    [startCoroutine]------------------>[updateUI]
                        |                               ↑
                        ↓ withContext(IO)               |
IO Thread:             [fetchData]-->[processData]-----
```

## Structured Concurrency

### CoroutineScope Hierarchy:
```kotlin
class MyViewModel : ViewModel() {
    
    fun loadData() {
        // Parent scope
        viewModelScope.launch {
            // Child coroutines
            val userData = async { fetchUser() }
            val postsData = async { fetchPosts() }
            
            // If parent is cancelled, children are cancelled too
            updateUI(userData.await(), postsData.await())
        }
    }
}

// Scope cancellation cascades down
viewModelScope.cancel() // Cancels all child coroutines
```

### Exception Handling:
```kotlin
// Parent-child exception propagation
viewModelScope.launch {
    try {
        launch {
            throw Exception("Child failed")
            // Parent coroutine will also fail
        }
    } catch (e: Exception) {
        // Won't catch child exceptions here
    }
}

// Proper exception handling
viewModelScope.launch {
    // SupervisorJob prevents child failure from cancelling parent
    supervisorScope {
        launch {
            try {
                riskyOperation()
            } catch (e: Exception) {
                handleError(e)
            }
        }
        
        launch {
            safeOperation() // Continues even if sibling fails
        }
    }
}
```

## Flow Basics

### What is Flow?
Flow is a cold asynchronous data stream that emits values sequentially.

```kotlin
// Creating a Flow
fun simpleFlow(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(100) // Simulate async work
        emit(i)    // Emit value
    }
}

// Collecting a Flow
scope.launch {
    simpleFlow().collect { value ->
        println(value) // 1, 2, 3, 4, 5
    }
}
```

### Cold vs Hot Streams:
```kotlin
// Cold Flow - starts fresh for each collector
val coldFlow = flow {
    println("Flow started")
    emit(1)
    emit(2)
}

// Each collector gets their own execution
coldFlow.collect { } // Prints "Flow started"
coldFlow.collect { } // Prints "Flow started" again

// Hot Flow - shared among collectors
val hotFlow = MutableSharedFlow<Int>()
hotFlow.emit(1) // Emitted even without collectors
```

## Flow Operators

### Transform Operators:
```kotlin
flow { emit(1); emit(2); emit(3) }
    .map { it * 2 }           // 2, 4, 6
    .filter { it > 3 }        // 4, 6
    .take(1)                  // 4
    .collect { println(it) }
```

### Combining Flows:
```kotlin
// Zip - combines corresponding elements
val flow1 = flowOf(1, 2, 3)
val flow2 = flowOf("A", "B", "C")

flow1.zip(flow2) { num, letter ->
    "$num$letter"
}.collect { println(it) } // 1A, 2B, 3C

// Combine - combines latest values
val updates = flow1.combine(flow2) { num, letter ->
    "$num$letter"
}
```

### Flow Context:
```kotlin
fun fetchData(): Flow<Data> = flow {
    // Runs in collector's context
    val data = api.getData() // If collected on IO, runs on IO
    emit(data)
}.flowOn(Dispatchers.IO) // Changes upstream context

// Usage
scope.launch(Dispatchers.Main) {
    fetchData()
        .map { it.process() }     // Runs on IO
        .flowOn(Dispatchers.Default) // Changes context for map
        .collect { updateUI(it) } // Runs on Main
}
```

### Error Handling:
```kotlin
flow { 
    emit(1)
    throw Exception("Error")
    emit(2) // Never reached
}
.catch { e ->
    println("Caught $e")
    emit(-1) // Emit fallback value
}
.collect { println(it) } // 1, -1
```

## StateFlow and SharedFlow

### StateFlow - Current State Holder:
```kotlin
class ViewModel {
    // Always has a value, replays latest to new collectors
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun updateState() {
        _state.value = _state.value.copy(loading = true)
        // or
        _state.update { it.copy(loading = true) }
    }
}

// Collecting in Compose
@Composable
fun Screen(viewModel: ViewModel) {
    val state by viewModel.state.collectAsState()
    // Recomposes when state changes
}
```

### SharedFlow - Event Stream:
```kotlin
class ViewModel {
    // No initial value, configurable replay
    private val _events = MutableSharedFlow<Event>(
        replay = 0,          // Don't replay to new subscribers
        extraBufferCapacity = 1 // Buffer 1 event
    )
    val events = _events.asSharedFlow()
    
    fun sendEvent(event: Event) {
        _events.tryEmit(event) // Non-suspending emit
    }
}
```

### StateFlow vs SharedFlow:
```kotlin
// StateFlow
- Always has a value
- Replays latest value to new collectors
- Conflates (skips) duplicate values
- Good for: UI state

// SharedFlow
- May not have initial value
- Configurable replay and buffer
- Doesn't conflate
- Good for: Events, one-time actions
```

## Best Practices

### 1. Scope Management:
```kotlin
class MyActivity : AppCompatActivity() {
    // Activity-scoped coroutines
    private val scope = lifecycleScope
    
    override fun onCreate() {
        // Automatically cancelled when activity destroyed
        scope.launch {
            loadData()
        }
    }
}

class MyViewModel : ViewModel() {
    init {
        // ViewModel-scoped coroutines
        viewModelScope.launch {
            loadInitialData()
        }
    }
}
```

### 2. Cancellation Handling:
```kotlin
suspend fun downloadFile() {
    withContext(Dispatchers.IO) {
        for (i in 1..100) {
            // Check if still active
            ensureActive()
            
            // Or use yield for cooperative cancellation
            yield()
            
            downloadChunk(i)
        }
    }
}
```

### 3. Resource Management:
```kotlin
suspend fun readFile(path: String) = withContext(Dispatchers.IO) {
    FileInputStream(path).use { stream ->
        // Stream automatically closed even if cancelled
        stream.readBytes()
    }
}
```

### 4. Testing Coroutines:
```kotlin
@Test
fun testCoroutine() = runTest { // TestScope
    val repository = TestRepository()
    val viewModel = MyViewModel(repository)
    
    // Advance time in tests
    advanceTimeBy(1000)
    
    assertEquals(expected, viewModel.state.value)
}
```

### 5. Flow Collection:
```kotlin
// In ViewModel - convert Flow to StateFlow
val uiState = repository.dataFlow
    .map { data -> UiState(data) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

// In Compose
@Composable
fun Screen(viewModel: ViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
}
```

## Common Patterns

### 1. Retry Logic:
```kotlin
suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // last attempt
}
```

### 2. Debouncing:
```kotlin
fun EditText.textChanges(): Flow<String> = callbackFlow {
    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            trySend(s.toString())
        }
        // Other methods
    }
    addTextChangedListener(watcher)
    awaitClose { removeTextChangedListener(watcher) }
}

// Usage with debounce
editText.textChanges()
    .debounce(300) // Wait 300ms
    .filter { it.length > 2 }
    .flatMapLatest { query ->
        searchRepository.search(query)
    }
    .collect { results ->
        showResults(results)
    }
```

### 3. Polling:
```kotlin
fun pollData(interval: Long): Flow<Data> = flow {
    while (currentCoroutineContext().isActive) {
        emit(fetchData())
        delay(interval)
    }
}
```

## Summary

Coroutines and Flow provide:
- Asynchronous programming without callbacks
- Structured concurrency
- Cancellation support
- Thread management
- Reactive data streams

Key Points:
- Use appropriate dispatchers
- Handle cancellation
- Manage scope lifecycle
- Choose between Flow types wisely
- Test with TestScope