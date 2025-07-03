// 🔖 REMOTE KEYS - THE BOOKMARK SYSTEM FOR PAGINATION

// Think of remote keys as bookmarks in a library system
// They remember "where you left off" when loading data

// 📚 THE PROBLEM REMOTE KEYS SOLVE:
// Scenario WITHOUT remote keys:
/*
1. User scrolls, loads 30 persons (3 pages)
2. App gets killed (low memory, user switches apps)
3. User returns to app
4. Problem: Where do we start loading?
   - Start from beginning? User loses position ❌
   - But we don't know where page 3 ended!
*/

// Scenario WITH remote keys:
/*
1. User scrolls, loads 30 persons
2. We save: "Last loaded cursor: 'cGVvcGxlOjMw'"
3. App gets killed
4. User returns
5. We check: "Oh, we have data up to cursor 'cGVvcGxlOjMw'"
6. Continue loading from there ✅
*/

// 🔑 WHAT EXACTLY ARE REMOTE KEYS?
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey
    val label: String,      // Which list? "persons", "starships", "planets"
    val nextKey: String?    // The cursor/key for the next page
)

// Real example data in your database:
// | label      | nextKey          |
// |------------|------------------|
// | "persons"  | "cGVvcGxlOjEw"   |  <- After person #10
// | "starships"| "c3RhcnNoaXA6MjA" |  <- After starship #20
// | "planets"  | null             |  <- No more planets to load

// 🎯 HOW REMOTE KEYS WORK WITH GRAPHQL CURSORS:

// First API call:
query {
    allPeople(first: 10) {
        pageInfo {
            endCursor      // "cGVvcGxlOjEw" - bookmark for page 1
            hasNextPage    // true - more data available
        }
        edges { ... }
    }
}

// We save the remote key:
remoteKeyDao.insert(
    RemoteKeyEntity(
        label = "persons",
        nextKey = "cGVvcGxlOjEw"  // Save the bookmark!
    )
)

// Next API call uses the saved cursor:
query {
    allPeople(first: 10, after: "cGVvcGxlOjEw") {  // Start after bookmark
        pageInfo {
            endCursor      // "cGVvcGxlOjIw" - new bookmark
            hasNextPage
        }
        edges { ... }
    }
}

// 🔄 THE COMPLETE FLOW IN YOUR APP:

class PersonRemoteMediator(
    private val apolloClient: ApolloClient,
    private val database: StarWarsDatabase
) : RemoteMediator<Int, PersonEntity>() {
    
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PersonEntity>
    ): MediatorResult {
        
        return when (loadType) {
            LoadType.REFRESH -> {
                // User pulled to refresh - start over
                database.remoteKeyDao().deleteByLabel("persons")
                database.personDao().clearAll()
                loadPage(cursor = null)  // Start from beginning
            }
            
            LoadType.APPEND -> {
                // User scrolled to bottom - load more
                val remoteKey = database.remoteKeyDao().getRemoteKey("persons")
                if (remoteKey?.nextKey == null) {
                    // No bookmark = no more pages
                    MediatorResult.Success(endOfPaginationReached = true)
                } else {
                    // Use bookmark to load next page
                    loadPage(cursor = remoteKey.nextKey)
                }
            }
            
            LoadType.PREPEND -> {
                // We don't support scrolling up to load previous
                MediatorResult.Success(endOfPaginationReached = true)
            }
        }
    }
    
    private suspend fun loadPage(cursor: String?): MediatorResult {
        // Make API call with cursor
        val response = apolloClient.query(
            GetPeopleQuery(first = 10, after = cursor)
        ).execute()
        
        // Save new bookmark for next time
        database.remoteKeyDao().insert(
            RemoteKeyEntity(
                label = "persons",
                nextKey = response.data?.allPeople?.pageInfo?.endCursor
            )
        )
        
        // ... save persons to database ...
        
        return MediatorResult.Success(
            endOfPaginationReached = !response.data?.allPeople?.pageInfo?.hasNextPage
        )
    }
}

// 💡 WHY THIS DESIGN?
// 1. Survives process death (stored in database)
// 2. Each list has its own bookmark (persons, starships, planets)
// 3. Enables seamless pagination
// 4. Users continue where they left off