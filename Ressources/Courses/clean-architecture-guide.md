# Clean Architecture Complete Guide for Android

## Table of Contents
1. [Overview](#overview)
2. [Layer Separation](#layer-separation)
3. [Dependency Rule](#dependency-rule)
4. [Implementation Examples](#implementation-examples)
5. [Use Cases Explained](#use-cases-explained)
6. [Mappers and Data Flow](#mappers-and-data-flow)
7. [Services vs DAOs vs APIs](#services-vs-daos-vs-apis)
8. [Best Practices](#best-practices)

## Overview

Clean Architecture separates your app into layers with clear boundaries and dependencies flowing in one direction.

```
┌─────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                      │
│  (UI, ViewModels, Composables, States)                 │
│                           ↓                              │
├─────────────────────────────────────────────────────────┤
│                    DOMAIN LAYER                          │
│  (Use Cases, Business Logic, Domain Models)            │
│                           ↓                              │
├─────────────────────────────────────────────────────────┤
│                     DATA LAYER                           │
│  (Repositories, Data Sources, DTOs, Mappers)           │
└─────────────────────────────────────────────────────────┘

Dependencies flow downward only: Presentation → Domain → Data
Domain NEVER depends on Data or Presentation
```

## Layer Separation

### Domain Layer (Pure Kotlin - No Android Dependencies)
```kotlin
// domain/model/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String,
    val isPremium: Boolean
) {
    // Business logic in domain model
    fun canAccessPremiumFeatures(): Boolean = isPremium
    
    fun isValidEmail(): Boolean = 
        email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
}

// domain/repository/UserRepository.kt
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    fun observeUser(id: String): Flow<User?>
}

// domain/usecase/GetUserProfileUseCase.kt
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsService: AnalyticsService // Domain service
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return userRepository.getUser(userId)
            .onSuccess { user ->
                analyticsService.trackProfileView(userId)
            }
    }
}
```

### Data Layer (Implements Domain Interfaces)
```kotlin
// data/local/entity/UserEntity.kt
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val isPremium: Boolean,
    val lastUpdated: Long
)

// data/remote/dto/UserDto.kt
@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val emailAddress: String,
    val subscriptionType: String
)

// data/mapper/UserMapper.kt
fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email,
    isPremium = isPremium
)

fun UserDto.toEntity() = UserEntity(
    id = id,
    name = fullName,
    email = emailAddress,
    isPremium = subscriptionType == "premium",
    lastUpdated = System.currentTimeMillis()
)

// data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val userDataSource: UserRemoteDataSource
) : UserRepository {
    
    override suspend fun getUser(id: String): Result<User> {
        return try {
            // Try local first
            val localUser = userDao.getUser(id)
            if (localUser != null && !isStale(localUser)) {
                Result.Success(localUser.toDomain())
            } else {
                // Fetch from remote
                val remoteUser = userApi.getUser(id)
                userDao.insert(remoteUser.toEntity())
                Result.Success(remoteUser.toEntity().toDomain())
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Presentation Layer (Android Specific)
```kotlin
// presentation/viewmodel/UserProfileViewModel.kt
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(UserProfileState())
    val state = _state.asStateFlow()
    
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            getUserProfile(userId)
                .onSuccess { user ->
                    _state.update { 
                        it.copy(
                            user = user,
                            isLoading = false
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

// presentation/ui/UserProfileScreen.kt
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    UserProfileContent(
        state = state,
        onEditClick = { viewModel.editProfile() }
    )
}
```

## Use Cases Explained

Use Cases encapsulate single business operations:

```kotlin
// Simple Use Case
class GetFavoriteItemsUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    operator fun invoke(): Flow<List<Item>> {
        return repository.getAllItems()
            .map { items -> 
                items.filter { it.isFavorite }
                     .sortedByDescending { it.addedDate }
            }
    }
}

// Complex Use Case with Business Rules
class PurchaseItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository,
    private val paymentService: PaymentService,
    private val inventoryService: InventoryService
) {
    suspend operator fun invoke(
        userId: String,
        itemId: String
    ): Result<PurchaseReceipt> {
        // Business rule: Check user eligibility
        val user = userRepository.getUser(userId).getOrNull()
            ?: return Result.Error(UserNotFoundError)
            
        if (!user.isVerified) {
            return Result.Error(UnverifiedUserError)
        }
        
        // Business rule: Check item availability
        val item = itemRepository.getItem(itemId).getOrNull()
            ?: return Result.Error(ItemNotFoundError)
            
        if (!inventoryService.isAvailable(itemId)) {
            return Result.Error(OutOfStockError)
        }
        
        // Business rule: Check purchase limits
        val userPurchases = itemRepository.getUserPurchases(userId)
        if (userPurchases.count { it.itemId == itemId } >= 3) {
            return Result.Error(PurchaseLimitExceededError)
        }
        
        // Process payment
        return paymentService.processPayment(user, item)
            .map { transaction ->
                // Update inventory
                inventoryService.decrementStock(itemId)
                
                // Record purchase
                itemRepository.recordPurchase(userId, itemId, transaction.id)
                
                PurchaseReceipt(
                    transactionId = transaction.id,
                    item = item,
                    amount = transaction.amount,
                    timestamp = Clock.System.now()
                )
            }
    }
}
```

## Mappers and Data Flow

```kotlin
// Data flow through layers
// API Response → DTO → Entity → Domain Model → UI State

// 1. API Response (JSON)
{
    "user_id": "123",
    "full_name": "John Doe",
    "email_address": "john@example.com"
}

// 2. DTO (Data Transfer Object)
@Serializable
data class UserResponseDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email_address") val emailAddress: String
)

// 3. Entity (Database)
@Entity
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val createdAt: Long
)

// 4. Domain Model
data class User(
    val id: String,
    val name: String,
    val email: String
)

// 5. UI State
data class UserUiState(
    val displayName: String,
    val emailMasked: String,
    val avatarUrl: String
)

// Mappers connect these layers
fun UserResponseDto.toEntity() = UserEntity(
    id = userId,
    name = fullName,
    email = emailAddress,
    createdAt = System.currentTimeMillis()
)

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email
)

fun User.toUiState() = UserUiState(
    displayName = name,
    emailMasked = email.maskEmail(),
    avatarUrl = "https://avatar.com/$id"
)
```

## Services vs DAOs vs APIs

### When to Use Each:

```kotlin
// DAO - Direct database access
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?
    
    @Insert
    suspend fun insert(user: UserEntity)
}

// API - Network requests
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponseDto
}

// Service - Complex operations, business logic
class UserService @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val cacheManager: CacheManager
) {
    suspend fun syncUser(id: String) {
        // Complex sync logic
        val remoteUser = userApi.getUser(id)
        val localUser = userDao.getUser(id)
        
        if (shouldUpdate(localUser, remoteUser)) {
            userDao.insert(remoteUser.toEntity())
            cacheManager.invalidate("user_$id")
        }
    }
    
    private fun shouldUpdate(local: UserEntity?, remote: UserResponseDto): Boolean {
        // Business logic for sync decision
        return local == null || local.lastUpdated < (System.currentTimeMillis() - CACHE_TTL)
    }
}

// Repository uses all of them
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val userService: UserService
) : UserRepository {
    
    override suspend fun getUser(id: String): Result<User> {
        // Use DAO for local data
        val cachedUser = userDao.getUser(id)
        
        if (cachedUser != null && !isStale(cachedUser)) {
            return Result.Success(cachedUser.toDomain())
        }
        
        // Use Service for complex operations
        return try {
            userService.syncUser(id)
            val updatedUser = userDao.getUser(id)!!
            Result.Success(updatedUser.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

## Best Practices

### 1. Dependency Injection Setup
```kotlin
// Domain Module
@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {
    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// Data Module
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}

// Use Case Module
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun provideGetUserUseCase(
        repository: UserRepository
    ): GetUserProfileUseCase {
        return GetUserProfileUseCase(repository)
    }
}
```

### 2. Error Handling
```kotlin
// Domain errors
sealed class DomainError : Exception() {
    object NetworkError : DomainError()
    object NotFoundError : DomainError()
    data class ValidationError(val field: String) : DomainError()
}

// Use in repository
override suspend fun getUser(id: String): Result<User> {
    return try {
        val user = userApi.getUser(id)
        Result.Success(user.toDomain())
    } catch (e: IOException) {
        Result.Error(DomainError.NetworkError)
    } catch (e: HttpException) {
        when (e.code()) {
            404 -> Result.Error(DomainError.NotFoundError)
            else -> Result.Error(DomainError.NetworkError)
        }
    }
}
```

### 3. Testing
```kotlin
// Domain layer test (no Android dependencies)
class GetUserProfileUseCaseTest {
    @Test
    fun `when user exists returns user`() = runTest {
        // Given
        val repository = mockk<UserRepository>()
        val expectedUser = User("1", "John", "john@example.com")
        coEvery { repository.getUser("1") } returns Result.Success(expectedUser)
        
        val useCase = GetUserProfileUseCase(repository)
        
        // When
        val result = useCase("1")
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedUser, result.data)
    }
}
```

## Summary

Clean Architecture provides:
- **Testability**: Each layer can be tested independently
- **Flexibility**: Easy to change data sources or UI
- **Maintainability**: Clear separation of concerns
- **Scalability**: Easy to add features without affecting other layers

Remember: The domain layer is the heart of your application and should contain all business logic, free from platform-specific code.