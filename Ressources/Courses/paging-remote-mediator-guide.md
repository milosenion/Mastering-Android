# Paging 3 and RemoteMediator Complete Guide

## Table of Contents
1. [Understanding Paging 3](#understanding-paging-3)
2. [Core Components](#core-components)
3. [RemoteMediator Explained](#remotemediator-explained)
4. [GraphQL Cursor Pagination](#graphql-cursor-pagination)
5. [Complete Implementation](#complete-implementation)
6. [Data Flow Visualization](#data-flow-visualization)
7. [Best Practices](#best-practices)

## Understanding Paging 3

### What is Paging 3?
Paging 3 is a library that helps load and display large datasets efficiently by loading data in small chunks (pages).

### Architecture Overview:
```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│     UI      │────▶│   Pager     │────▶│ PagingSource │
│ (LazyColumn)│     │             │     │ (Local DB)   │
└─────────────┘     └──────┬──────┘     └──────────────┘
                           │                     ▲
                           │                     │
                           ▼                     │
                    ┌──────────────┐             │
                    │RemoteMediator│─────────────┘
                    │ (Network)    │
                    └──────────────┘
```

## Core Components

### 1. PagingData
```kotlin
// Container for paginated data
Flow<PagingData<T>> // Stream of paginated data
```

### 2. PagingSource
```kotlin
// Loads data from a single source (DB or Network)
abstract class PagingSource<Key, Value> {
    abstract suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value>
}
```

### 3. RemoteMediator
```kotlin
// Orchestrates between network and database
abstract class RemoteMediator<Key, Value> {
    abstract suspend fun load(
        loadType: LoadType,
        state: PagingState<Key, Value>
    ): MediatorResult
}
```

### 4. Pager
```kotlin
// Coordinates everything
Pager(
    config = PagingConfig(pageSize = 20),
    remoteMediator = MyRemoteMediator(),
    pagingSourceFactory = { database.myDao().pagingSource() }
)
```

## RemoteMediator Explained

### LoadType States:
```kotlin
sealed class LoadType {
    object Refresh : LoadType() // Initial load or refresh
    object Prepend : LoadType() // Load previous data
    object Append : LoadType()  // Load next data
}
```

### Visual LoadType Flow:
```
Initial Load:
└─ REFRESH (Load first page)

Scroll Down:
└─ APPEND (Load next page)

Pull to Refresh:
└─ REFRESH (Reload from start)

Scroll Up (rare):
└─ PREPEND (Load previous page)
```

## GraphQL Cursor Pagination

### How Cursors Work:
```graphql
# First Request
query GetPeople($first: Int!, $after: String) {
  allPeople(first: 10, after: null) {
    pageInfo {
      hasNextPage  # true
      endCursor    # "cursor_10"
    }
    edges {
      cursor      # Position marker
      node {      # Actual data
        id
        name
      }
    }
  }
}

# Next Request - Use endCursor from previous
query GetPeople($first: Int!, $after: String) {
  allPeople(first: 10, after: "cursor_10") {
    # Returns items 11-20
  }
}
```

### Cursor Visualization:
```
Database: [1][2][3][4][5][6][7][8][9][10][11][12][13][14][15]...
               ↑                        ↑
          First Request            endCursor
          (items 1-10)             

Second Request uses endCursor:
Database: [1][2][3][4][5][6][7][8][9][10][11][12][13][14][15]...
                                          ↑                  ↑
                                    Start here          endCursor
                                    (items 11-15)
```

## Complete Implementation

### 1. Entity (Database Model):
```kotlin
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmCount: Int,
    val isFavorite: Boolean = false,
    val cursor: String, // Store cursor for pagination
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. DAO with PagingSource:
```kotlin
@Dao
interface PeopleDao {
    // PagingSource for list display
    @Query("SELECT * FROM people ORDER BY createdAt ASC")
    fun pagingSource(): PagingSource<Int, PersonEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)
    
    @Query("DELETE FROM people")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM people")
    suspend fun count(): Int
}
```

### 3. RemoteMediator Implementation:
```kotlin
@OptIn(ExperimentalPagingApi::class)
class PeopleRemoteMediator(
    private val database: StarWarsDatabase,
    private val apolloService: ApolloService,
    private val dataStoreService: DataStoreService
) : RemoteMediator<Int, PersonEntity>() {
    
    override suspend fun initialize(): InitializeAction {
        // Check if we need to refresh
        val lastSyncTime = dataStoreService.getLongValuePreference("last_sync") ?: 0
        val cacheTimeout = TimeUnit.HOURS.toMillis(1)
        
        return if (System.currentTimeMillis() - lastSyncTime > cacheTimeout) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
    
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PersonEntity>
    ): MediatorResult {
        return try {
            // Determine what to load
            val loadKey = when (loadType) {
                LoadType.REFRESH -> null // Start from beginning
                LoadType.PREPEND -> return MediatorResult.Success(
                    endOfPaginationReached = true
                )
                LoadType.APPEND -> {
                    // Get the last cursor from database
                    getRemoteKeyForLastItem(state)
                }
            }
            
            // Make network request
            val response = apolloService.fetchPeople(
                pageSize = state.config.pageSize,
                cursor = loadKey
            )
            
            val people = response.allPeople?.edges ?: emptyList()
            val endOfPaginationReached = response.allPeople?.pageInfo?.hasNextPage == false
            
            // Save to database
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    database.peopleDao().clearAll()
                    database.remoteKeyDao().clearAll()
                }
                
                // Insert new data
                val entities = people.map { edge ->
                    PersonEntity(
                        id = edge.node.id,
                        name = edge.node.name,
                        filmCount = edge.node.filmConnection.totalCount,
                        cursor = edge.cursor
                    )
                }
                database.peopleDao().insertAll(entities)
                
                // Save next cursor
                val nextCursor = response.allPeople?.pageInfo?.endCursor
                if (nextCursor != null && !endOfPaginationReached) {
                    database.remoteKeyDao().insert(
                        RemoteKey(
                            label = "people",
                            nextKey = nextCursor
                        )
                    )
                }
                
                // Update sync time
                if (loadType == LoadType.REFRESH) {
                    dataStoreService.saveLongValuePreference(
                        "last_sync",
                        System.currentTimeMillis()
                    )
                }
            }
            
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
```

### 4. Repository Pattern:
```kotlin
class PeopleRepository(
    private val database: StarWarsDatabase,
    private val mediator: PeopleRemoteMediator
) {
    fun getPeoplePagingData(): Flow<PagingData<Person>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            remoteMediator = mediator,
            pagingSourceFactory = { database.peopleDao().pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                entity.toDomainModel()
            }
        }
    }
}
```

### 5. ViewModel:
```kotlin
@HiltViewModel
class PeopleViewModel @Inject constructor(
    repository: PeopleRepository
) : ViewModel() {
    
    val peoplePagingData = repository.getPeoplePagingData()
        .cachedIn(viewModelScope) // Cache for configuration changes
}
```

### 6. UI Implementation:
```kotlin
@Composable
fun PeopleList(viewModel: PeopleViewModel = hiltViewModel()) {
    val pagingItems = viewModel.peoplePagingData.collectAsLazyPagingItems()
    
    LazyColumn {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index }
        ) { index ->
            val person = pagingItems[index]
            if (person != null) {
                PersonItem(person)
            } else {
                // Placeholder while loading
                PersonItemPlaceholder()
            }
        }
        
        // Handle load states
        pagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading -> {
                    item { LoadingView() }
                }
                loadState.append is LoadState.Loading -> {
                    item { LoadingItem() }
                }
                loadState.refresh is LoadState.Error -> {
                    item {
                        ErrorView(
                            error = (loadState.refresh as LoadState.Error).error,
                            onRetry = { retry() }
                        )
                    }
                }
            }
        }
    }
}
```

## Data Flow Visualization

### Complete Flow:
```
1. UI requests data
   └─▶ Pager checks PagingSource (DB)
       
2. If DB empty/stale
   └─▶ RemoteMediator.load(REFRESH)
       └─▶ Fetch from Network
           └─▶ Save to DB
               └─▶ PagingSource emits new data
                   └─▶ UI updates

3. User scrolls to bottom
   └─▶ Pager detects near end
       └─▶ RemoteMediator.load(APPEND)
           └─▶ Fetch next page with cursor
               └─▶ Append to DB
                   └─▶ PagingSource emits update
                       └─▶ UI shows new items
```

### State Management:
```
┌─────────────────────────────────────────┐
│            PagingData States            │
├─────────────────────────────────────────┤
│ NotLoading ──▶ Loading ──▶ NotLoading  │
│     ↑                          │        │
│     └──────── Error ◀──────────┘        │
│                 │                       │
│                 └──▶ Retry              │
└─────────────────────────────────────────┘
```

## Best Practices

### 1. Error Handling:
```kotlin
when (loadState.refresh) {
    is LoadState.Error -> {
        // Show full screen error
    }
}

when (loadState.append) {
    is LoadState.Error -> {
        // Show inline error at bottom
    }
}
```

### 2. Cache Configuration:
```kotlin
Pager(
    config = PagingConfig(
        pageSize = 20,          // Items per page
        prefetchDistance = 5,   // Prefetch when 5 items from end
        enablePlaceholders = false, // No null placeholders
        initialLoadSize = 40,   // First load is bigger
        maxSize = 200          // Maximum items in memory
    )
)
```

### 3. Refresh Handling:
```kotlin
// SwipeRefresh integration
SwipeRefresh(
    state = rememberSwipeRefreshState(
        pagingItems.loadState.refresh is LoadState.Loading
    ),
    onRefresh = { pagingItems.refresh() }
) {
    LazyColumn { /* ... */ }
}
```

### 4. Testing:
```kotlin
@Test
fun testPaging() = runTest {
    val pagingSource = MyPagingSource()
    
    val result = pagingSource.load(
        PagingSource.LoadParams.Refresh(
            key = null,
            loadSize = 20,
            placeholdersEnabled = false
        )
    )
    
    assertTrue(result is PagingSource.LoadResult.Page)
    assertEquals(20, result.data.size)
}
```

## Common Issues and Solutions

### 1. Duplicate Loads:
```kotlin
// Use cachedIn to prevent duplicate network calls
val pagingData = repository.getPagingData()
    .cachedIn(viewModelScope)
```

### 2. Scroll Position Loss:
```kotlin
// Save and restore scroll position
val listState = rememberLazyListState()
LazyColumn(state = listState) { /* ... */ }
```

### 3. Memory Leaks:
```kotlin
// Always use viewModelScope for caching
.cachedIn(viewModelScope) // ✅
// Not
.cachedIn(GlobalScope) // ❌
```

## Summary

Paging 3 with RemoteMediator provides:
- Efficient data loading
- Network + Database coordination
- Automatic error handling
- Built-in loading states
- Configuration change handling

Key Points:
- RemoteMediator orchestrates data flow
- PagingSource provides data to UI
- Cursor pagination for GraphQL
- Proper error and state handling
- Cache data appropriately