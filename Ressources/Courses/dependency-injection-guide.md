# Dependency Injection Complete Guide with Dagger-Hilt

## Table of Contents
1. [What is Dependency Injection?](#what-is-dependency-injection)
2. [Hilt Basics](#hilt-basics)
3. [Lifecycle Scopes](#lifecycle-scopes)
4. [Modules Explained](#modules-explained)
5. [Object vs Interface Modules](#object-vs-interface-modules)
6. [How Dependencies are Mapped](#how-dependencies-are-mapped)
7. [Best Practices](#best-practices)

## What is Dependency Injection?

Dependency Injection (DI) is a design pattern where objects receive their dependencies from external sources rather than creating them internally.

### Without DI:
```kotlin
class UserRepository {
    // Creating dependencies internally - BAD!
    private val database = UserDatabase()
    private val api = UserApi()
    private val logger = Logger()
}

// Problems:
// - Hard to test (can't mock dependencies)
// - Tightly coupled
// - Hard to change implementations
```

### With DI:
```kotlin
class UserRepository @Inject constructor(
    private val database: UserDatabase,  // Injected
    private val api: UserApi,            // Injected
    private val logger: Logger           // Injected
)

// Benefits:
// - Easy to test (inject mocks)
// - Loosely coupled
// - Easy to swap implementations
```

## Hilt Basics

### Setup
```kotlin
// Application class
@HiltAndroidApp
class MyApplication : Application()

// Activity/Fragment
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

// ViewModel
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel()
```

## Lifecycle Scopes

```
┌─────────────────────────────────────────────┐
│          @Singleton (Application)           │
│  Lives as long as the app is running       │
│  (Database, Retrofit, Repositories)         │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│      @ActivityRetainedScoped                │
│  Survives configuration changes             │
│  (ViewModels behind the scenes)            │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         @ActivityScoped                     │
│  Lives as long as Activity                  │
│  (Activity-specific helpers)                │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         @FragmentScoped                     │
│  Lives as long as Fragment                  │
│  (Fragment-specific helpers)                │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         @ViewScoped                         │
│  Lives as long as View                      │
│  (View-specific helpers)                    │
└─────────────────────────────────────────────┘
```

### Scoping Guidelines:

```kotlin
// Singleton - One instance for entire app lifetime
@Singleton
class DatabaseService @Inject constructor()

@Singleton
class NetworkService @Inject constructor()

@Singleton
class UserRepository @Inject constructor()

// ViewModelScoped - Survives configuration changes
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase // Can be unscoped
) : ViewModel()

// Unscoped - New instance every time
class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
)
```

## Modules Explained

### Object Modules (For classes you don't own or need custom creation)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com")
            .client(okHttpClient)
            .build()
    }
    
    @Provides
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
}
```

### Interface Modules (For binding implementations to interfaces)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    
    @Binds
    @Singleton
    fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    fun bindAnalyticsService(
        impl: FirebaseAnalyticsService
    ): AnalyticsService
}
```

## Object vs Interface Modules

### Use Object Module When:
- Creating instances of classes you don't own (Retrofit, Room, etc.)
- Complex initialization logic
- Need to call methods to create instance

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Complex creation logic
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_db"
        ).fallbackToDestructiveMigration()
         .build()
    }
}
```

### Use Interface Module When:
- Binding implementations to interfaces
- Simple constructor injection
- You own both interface and implementation

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface ServiceModule {
    
    @Binds
    fun bindAuthService(impl: AuthServiceImpl): AuthService
}

// Implementation
class AuthServiceImpl @Inject constructor(
    private val api: AuthApi
) : AuthService {
    // Implementation
}
```

## How Dependencies are Mapped

Hilt automatically wires dependencies based on types:

```kotlin
// 1. Hilt sees this ViewModel needs UserRepository
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel()

// 2. Hilt looks for how to provide UserRepository
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// 3. Hilt sees UserRepositoryImpl needs UserDao and UserApi
class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    private val api: UserApi
) : UserRepository

// 4. Hilt looks for how to provide UserDao
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}

// 5. The chain continues until all dependencies are satisfied
```

### Dependency Graph Visualization:
```
UserViewModel
    └── UserRepository (interface)
        └── UserRepositoryImpl (implementation)
            ├── UserDao
            │   └── AppDatabase
            │       └── Context (@ApplicationContext)
            └── UserApi
                └── Retrofit
                    └── OkHttpClient
```

## Best Practices

### 1. Prefer Constructor Injection
```kotlin
// Good
class UserService @Inject constructor(
    private val repository: UserRepository
)

// Avoid field injection
class UserService {
    @Inject lateinit var repository: UserRepository // Avoid
}
```

### 2. Use Interfaces for Flexibility
```kotlin
// Define interface
interface DataSource {
    suspend fun getData(): List<Item>
}

// Implementations
class RemoteDataSource @Inject constructor() : DataSource
class LocalDataSource @Inject constructor() : DataSource

// Bind based on build variant
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    fun bindDataSource(impl: RemoteDataSource): DataSource
    // Or use LocalDataSource for debug builds
}
```

### 3. Scope Appropriately
```kotlin
// Singleton for expensive objects
@Singleton
class DatabaseConnection @Inject constructor()

// Unscoped for lightweight objects
class StringFormatter @Inject constructor()
```

### 4. Qualifiers for Multiple Implementations
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalSource

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @RemoteSource
    fun provideRemoteDataSource(): DataSource {
        return RemoteDataSource()
    }
    
    @Provides
    @LocalSource
    fun provideLocalDataSource(): DataSource {
        return LocalDataSource()
    }
}

// Usage
class Repository @Inject constructor(
    @RemoteSource private val remoteSource: DataSource,
    @LocalSource private val localSource: DataSource
)
```

### 5. Testing with Hilt
```kotlin
@HiltAndroidTest
class UserViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: UserRepository
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun testViewModel() {
        // Repository is automatically injected
        val viewModel = UserViewModel(repository)
        // Test viewModel
    }
}

// Test module to replace production module
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
interface TestRepositoryModule {
    @Binds
    fun bindRepository(impl: FakeUserRepository): UserRepository
}
```

## Common Patterns

### Repository Pattern with DI
```kotlin
// Domain layer
interface UserRepository {
    suspend fun getUser(id: String): User
}

// Data layer
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {
    override suspend fun getUser(id: String): User {
        // Implementation
    }
}

// DI Module
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

### Use Case Pattern with DI
```kotlin
class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return try {
            Result.Success(repository.getUser(userId))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// No module needed - Hilt can construct this automatically
```

## Summary

Hilt simplifies dependency injection in Android by:
- Automatically generating DI code
- Managing component lifecycles
- Providing compile-time verification
- Integrating with Android components

Remember:
- Use constructor injection
- Scope appropriately
- Bind interfaces to implementations
- Test with dependency injection