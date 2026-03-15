package com.stackusers.ui

import app.cash.turbine.test
import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.User
import com.stackusers.domain.usecase.SearchUsersUseCase
import com.stackusers.ui.screens.userlist.UserListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [UserListViewModel]
 *
 * Reviewer Note:
 * Uses Turbine to test StateFlow emissions and a TestDispatcher to control
 * coroutine execution timing, avoiding relying on real delays
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var searchUsersUseCase: SearchUsersUseCase
    private lateinit var viewModel: UserListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        searchUsersUseCase = mock()
        viewModel = UserListViewModel(searchUsersUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsIdle() = runTest {
        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSearchSubmittedEmitsLoadingThenSuccessWhenUseCaseSucceeds() = runTest {
        val users = listOf(buildUser(1L, "Alice"), buildUser(2L, "Bob"))
        whenever(searchUsersUseCase("alice")).thenReturn(Result.success(users))

        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())

            viewModel.onSearchQueryChanged("alice")
            viewModel.onSearchSubmitted()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(UiState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is UiState.Success)
            assertEquals(users, (success as UiState.Success).data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSearchSubmittedEmitsEmptyWhenUseCaseReturnsEmptyList() = runTest {
        whenever(searchUsersUseCase("nobody")).thenReturn(Result.success(emptyList()))

        viewModel.uiState.test {
            awaitItem() // initial Idle

            viewModel.onSearchQueryChanged("nobody")
            viewModel.onSearchSubmitted()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Empty, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSearchSubmittedEmitsErrorWhenUseCaseFails() = runTest {
        whenever(searchUsersUseCase("error")).thenReturn(
            Result.failure(RuntimeException("Network error"))
        )

        viewModel.uiState.test {
            awaitItem() // initial Idle

            viewModel.onSearchQueryChanged("error")
            viewModel.onSearchSubmitted()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(UiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is UiState.Error)
            assertEquals("Network error", (error as UiState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSearchClearedSetsQueryToEmptyAndStateToIdle() = runTest {
        // Verify the synchronous state mutations directly rather than via Flow emissions,
        // since StateFlow deduplicates equal values and Idle->Idle won't emit.
        viewModel.onSearchQueryChanged("kotlin")
        assertEquals("kotlin", viewModel.searchQuery.value)

        viewModel.onSearchCleared()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun onSearchClearedAfterSuccessfulSearchTransitionsBackToIdle() = runTest {
        val users = listOf(buildUser(1L, "Alice"))
        whenever(searchUsersUseCase("kotlin")).thenReturn(Result.success(users))

        viewModel.onSearchQueryChanged("kotlin")
        viewModel.onSearchSubmitted()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiState.Success(users), viewModel.uiState.value)

        viewModel.onSearchCleared()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun onSearchSubmittedWithBlankQueryDoesNotTriggerSearch() = runTest {
        viewModel.uiState.test {
            awaitItem() // Idle

            viewModel.onSearchQueryChanged("")
            viewModel.onSearchSubmitted()
            testDispatcher.scheduler.advanceUntilIdle()

            // Should remain Idle — no Loading emitted
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Helpers ---

    private fun buildUser(id: Long, name: String) = User(
        userId = id,
        displayName = name,
        reputation = 100,
        profileImageUrl = null,
        location = null,
        creationDate = 0L,
        badgeCounts = BadgeCounts(0, 0, 0),
        profileLink = null
    )
}
