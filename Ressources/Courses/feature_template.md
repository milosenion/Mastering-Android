# Feature Template Guide

## Creating a New List Feature (e.g., Starships)

### 1. Domain Models
```kotlin
// _core/domain/model/starship/Starship.kt
data class Starship(
    val id: String,
    val name: String,
    val filmCount: Int,
    val isFavorite: Boolean = false
)

// _core/domain/model/starship/StarshipDetail.kt
data class StarshipDetail(
    val id: String,
    val name: String,
    val model: String?,
    // ... other fields
)
```

### 2. Database Entity
```kotlin
// _core/data/local/database/entities/StarshipEntity.kt
@Entity(tableName = "starships")
data class StarshipEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmCount: Int,
    val isFavorite: Boolean = false,
    val cursor: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 3. DAO
```kotlin
// _core/data/local/database/dao/StarshipDao.kt
@Dao
interface StarshipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(starships: List<StarshipEntity>)
    
    @Query("SELECT * FROM starships ORDER BY createdAt ASC")
    fun pagingSource(): PagingSource<Int, StarshipEntity>
    
    // ... other methods
}
```

### 4. Mappers
```kotlin
// _core/data/mapper/StarshipMappers.kt
fun GetStarshipsQuery.Edge.toEntity(cursor: String): StarshipEntity { ... }
fun StarshipEntity.toDomain(): Starship { ... }
fun GetStarshipDetailQuery.Starship.toDomain(): StarshipDetail { ... }
```

### 5. List Feature Structure
```
screens/starship/list/
├── data/
│   ├── StarshipListRepositoryImpl.kt
│   ├── mediator/
│   │   └── StarshipRemoteMediator.kt
│   └── di/
│       └── StarshipListModule.kt
├── domain/
│   └── StarshipListRepository.kt
└── presentation/
    ├── StarshipListContract.kt
    ├── StarshipListViewModel.kt
    └── StarshipListScreen.kt
```

### 6. Contract
```kotlin
data class StarshipListState(
    val starships: Flow<PagingData<Starship>>,
    val isLoading: Boolean,
    val showFavoritesOnly: Boolean,
    val error: RootError?
) : State

sealed class StarshipListIntent : Intent {
    object LoadStarships : StarshipListIntent()
    data class StarshipClicked(val starshipId: String) : StarshipListIntent()
    data class ToggleFavorite(val starshipId: String) : StarshipListIntent()
    object ToggleFavoritesFilter : StarshipListIntent()
}

sealed class StarshipListEvent : Event {
    data class NavigateToStarshipDetail(val starshipId: String) : StarshipListEvent()
}
```

### 7. Repository
```kotlin
interface StarshipListRepository : BaseListRepository<Starship>
```

### 8. RemoteMediator
Copy PeopleRemoteMediator and adjust:
- Change entity types
- Change GraphQL query
- Change remote key label

### 9. ViewModel
```kotlin
@HiltViewModel
class StarshipListViewModel @Inject constructor(
    ioDispatcher: CoroutineDispatcher,
    private val repository: StarshipListRepository
) : BaseViewModel<StarshipListState, StarshipListIntent, StarshipListEvent>(
    StarshipListState.initial,
    ioDispatcher
) {
    // Implementation similar to PeopleListViewModel
}
```

### 10. Screen
Copy PeopleListScreen and adjust:
- Change data types
- Update UI labels
- Adjust navigation

## Quick Checklist

- [ ] Create domain models (list + detail)
- [ ] Create database entity
- [ ] Create/update DAO
- [ ] Create mappers
- [ ] Create repository interface
- [ ] Create repository implementation
- [ ] Create RemoteMediator
- [ ] Create Contract (State, Intent, Event)
- [ ] Create ViewModel
- [ ] Create Screen composable
- [ ] Create DI module
- [ ] Add GraphQL queries
- [ ] Update Database class
- [ ] Update navigation routes