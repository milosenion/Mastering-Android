// 1. Unit Test for Repository
package com.milosenion.avelios.star_wars.screens.people.list.data

import com.milosenion.avelios.star_wars._core.data.local.database.StarWarsDatabase
import com.milosenion.avelios.star_wars._core.data.service.ApolloService
import com.milosenion.avelios.star_wars._core.data.service.DataStoreService
import com.milosenion.avelios.star_wars._core.data.service.RemoteMediatorService
import com.milosenion.avelios.star_wars._core.domain.error.DataError
import com.milosenion.avelios.star_wars._core.domain.error.Result
import com.milosenion.avelios.star_wars._core.domain.model.people.Person
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeopleRepositoryTest {
    
    private lateinit var repository: RepositoryImpl
    private lateinit var apolloService: ApolloService
    private lateinit var remoteMediatorService: RemoteMediatorService
    private lateinit var dataStoreService: DataStoreService
    private lateinit var database: StarWarsDatabase
    
    @Before
    fun setup() {
        apolloService = mockk(relaxed = true)
        remoteMediatorService = mockk(relaxed = true)
        dataStoreService = mockk(relaxed = true)
        database = mockk(relaxed = true)
        
        repository = RepositoryImpl(
            apolloService,
            remoteMediatorService,
            dataStoreService,
            database
        )
    }
    
    @Test
    fun `toggleFavorite calls apolloService with correct parameter`() = runTest {
        // Given
        val personId = "person1"
        
        // When
        repository.toggleFavorite(personId)
        
        // Then
        coVerify { apolloService.toggleFavorite(personId = personId) }
    }
    
    @Test
    fun `getShowFavoritesOnly returns stored preference`() = runTest {
        // Given
        coEvery { dataStoreService.getBooleanValuePreference(any()) } returns true
        
        // When
        val result = repository.getShowFavoritesOnly()
        
        // Then
        assertTrue(result)
    }
}

// 2. Unit Test for ViewModel
package com.milosenion.avelios.star_wars.screens.people.list.presentation

import androidx.paging.PagingData
import com.milosenion.avelios.star_wars._core.domain.model.people.Person
import com.milosenion.avelios.star_wars.screens.people.list.domain.Repository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleListViewModelTest {
    
    private lateinit var viewModel: PeopleListViewModel
    private lateinit var repository: Repository
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        // Mock repository responses
        coEvery { repository.getShowFavoritesOnly() } returns false
        coEvery { repository.getPeoplePagingData(any()) } returns flowOf(PagingData.empty())
        
        viewModel = PeopleListViewModel(testDispatcher, repository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        // Then
        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.showFavoritesOnly)
        assertEquals(null, state.error)
    }
    
    @Test
    fun `ToggleFavorite intent calls repository`() = runTest {
        // Given
        val personId = "person1"
        
        // When
        viewModel.dispatch(PeopleListIntent.ToggleFavorite(personId))
        advanceUntilIdle()
        
        // Then
        coVerify { repository.toggleFavorite(personId) }
    }
    
    @Test
    fun `PersonClicked intent emits navigation event`() = runTest {
        // Given
        val personId = "person1"
        val events = mutableListOf<PeopleListEvent?>()
        
        // Collect events
        backgroundScope.launch(UnconfinedTestDispatcher()) {
            viewModel.events.collect { events.add(it) }
        }
        
        // When
        viewModel.dispatch(PeopleListIntent.PersonClicked(personId))
        advanceUntilIdle()
        
        // Then
        assertTrue(events.any { it is PeopleListEvent.NavigateToPersonDetail && it.personId == personId })
    }
}

// 3. UI Test for Composables
package com.milosenion.avelios.star_wars.screens.people.list.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.milosenion.avelios.star_wars._core.domain.model.people.Person
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class PeopleListScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `list displays people correctly`() {
        // Given
        val people = listOf(
            Person("1", "Luke Skywalker", 4, false),
            Person("2", "Leia Organa", 4, true)
        )
        
        composeTestRule.setContent {
            val pagingItems = flowOf(PagingData.from(people)).collectAsLazyPagingItems()
            
            // Simplified version for testing
            PeopleListContent(
                pagingItems = pagingItems,
                onPersonClick = {},
                onFavoriteClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Luke Skywalker").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 films").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leia Organa").assertIsDisplayed()
    }
    
    @Test
    fun `clicking favorite button triggers callback`() {
        // Given
        var clickedPersonId: String? = null
        val person = Person("1", "Luke Skywalker", 4, false)
        
        composeTestRule.setContent {
            ListItem(
                person = person,
                onItemClick = {},
                onFavoriteClick = { clickedPersonId = person.id }
            )
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()
        
        // Then
        assertEquals("1", clickedPersonId)
    }
}

// 4. Integration Test
package com.milosenion.avelios.star_wars.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.milosenion.avelios.star_wars._core.data.local.database.StarWarsDatabase
import com.milosenion.avelios.star_wars._core.data.local.database.entities.PersonEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: StarWarsDatabase
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun testFavoriteToggle() = runTest {
        // Given
        val person = PersonEntity(
            id = "1",
            name = "Luke Skywalker",
            filmCount = 4,
            isFavorite = false,
            cursor = "cursor1"
        )
        
        // When
        database.peopleDao().insertAll(listOf(person))
        database.peopleDao().updateFavoriteStatus("1", true)
        
        // Then
        val updated = database.peopleDao().getPersonById("1").first()
        assertTrue(updated?.isFavorite == true)
    }
}

// 5. Test Utilities
package com.milosenion.avelios.star_wars.test

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

object TestData {
    fun createPerson(
        id: String = "1",
        name: String = "Test Person",
        filmCount: Int = 3,
        isFavorite: Boolean = false
    ) = Person(id, name, filmCount, isFavorite)
    
    fun createPagingData(items: List<Person>): Flow<PagingData<Person>> {
        return flowOf(PagingData.from(items))
    }
}

// 6. Test Configuration (app/build.gradle.kts)
/*
android {
    testOptions {
        unitTests {
            includeAndroidResources = true
            returnDefaultValues = true
        }
    }
}

dependencies {
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("app.cash.turbine:turbine:0.12.1")
    
    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("io.mockk:mockk-android:1.13.3")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48")
    
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
*/