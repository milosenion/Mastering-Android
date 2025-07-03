// 🎯 ENTITY DESIGN PRINCIPLES

// Let's look at the GraphQL Person type from schema.graphqls:
/*
type Person implements Node {
  name: String
  birthYear: String
  eyeColor: String
  gender: String
  hairColor: String
  height: Int
  mass: Float
  skinColor: String
  homeworld: Planet
  filmConnection(...): PersonFilmsConnection
  species: Species
  starshipConnection(...): PersonStarshipsConnection
  vehicleConnection(...): PersonVehiclesConnection
  created: String
  edited: String
  id: ID!
}
*/

// 🤔 DECISION PROCESS: What fields to include in PersonEntity?

// STEP 1: Identify your list screen needs
// For the list, you need:
// - Person's name (to display)
// - Film count (requirement from task)
// - Favorite status (your business logic)
// - ID (to identify uniquely)

// STEP 2: Consider technical requirements
// - Cursor (for pagination position)
// - CreatedAt (for consistent ordering)

// STEP 3: Apply the principle - Store only what you need for the list!
@Entity(tableName = "persons")  // Using your preferred naming
data class PersonEntity(
    @PrimaryKey
    val id: String,           // From GraphQL schema - required
    val name: String,         // From GraphQL schema - for display
    val filmCount: Int,       // Calculated from filmConnection.totalCount
    val isFavorite: Boolean = false,  // Your business requirement
    val cursor: String,       // Technical requirement for pagination
    val createdAt: Long = System.currentTimeMillis()  // For ordering
)

// ❌ WHY NOT STORE EVERYTHING?
// Imagine if we stored all fields:
@Entity(tableName = "persons_wrong_approach")
data class PersonEntityWrong(
    @PrimaryKey val id: String,
    val name: String,
    val birthYear: String?,    // Not needed for list
    val eyeColor: String?,     // Not needed for list
    val gender: String?,       // Not needed for list
    val hairColor: String?,    // Not needed for list
    val height: Int?,          // Not needed for list
    val mass: Float?,          // Not needed for list
    val skinColor: String?,    // Not needed for list
    // ... and how do we store homeworld? Another table?
    // ... and what about starships? More tables?
)
// Problems:
// 1. Wastes storage space
// 2. Complex to maintain
// 3. Need complex queries/joins
// 4. Most data never used in list view

// ✅ THE SMART APPROACH: Different data for different purposes
// List Entity: Minimal data for efficient scrolling
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmCount: Int,
    val isFavorite: Boolean = false,
    val cursor: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Detail data: Fetched fresh from API when needed
// Why? Details can change, user views them rarely
// Better to get fresh data than store stale data

// 🎓 THE RULE OF THUMB:
// Store in Room if:
// 1. Needed for list display
// 2. Needed for offline functionality
// 3. User-generated (like favorites)
// 4. Technical metadata (cursor, timestamps)

// Fetch fresh from API if:
// 1. Only needed in detail view
// 2. Changes frequently
// 3. Has complex relationships
// 4. Rarely accessed