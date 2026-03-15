package com.stackusers.ui

import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.TopTag
import com.stackusers.data.model.User
import com.stackusers.data.model.UserDetail
import com.stackusers.domain.usecase.GetUserDetailUseCase
import com.stackusers.ui.userdetail.UserDetailViewModel
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [UserDetailViewModel]
 *
 * Verifies all UiState transitions and the retry logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getUserDetailUseCase: GetUserDetailUseCase
    private lateinit var viewModel: UserDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getUserDetailUseCase = mock()
        viewModel = UserDetailViewModel(getUserDetailUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadUserDetailEmitsSuccessWhenUseCaseSucceeds() = runTest {
        val detail = buildUserDetail(userId = 1L)
        whenever(getUserDetailUseCase(1L)).thenReturn(Result.success(detail))

        viewModel.loadUserDetail(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(detail, (state as UiState.Success).data)
    }

    @Test
    fun loadUserDetailEmitsErrorWhenUseCaseFails() = runTest {
        whenever(getUserDetailUseCase(1L)).thenReturn(
            Result.failure(RuntimeException("Network error"))
        )

        viewModel.loadUserDetail(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network error", (state as UiState.Error).message)
    }

    @Test
    fun loadUserDetailEmitsLoadingBeforeResultArrives() = runTest {
        val detail = buildUserDetail(userId = 1L)
        whenever(getUserDetailUseCase(1L)).thenReturn(Result.success(detail))

        viewModel.loadUserDetail(1L)

        // Before advancing the dispatcher, state should be Loading
        assertEquals(UiState.Loading, viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()

        // After advancing, state should be Success
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun loadUserDetailEmitsErrorWithFallbackMessageWhenExceptionHasNoMessage() = runTest {
        whenever(getUserDetailUseCase(1L)).thenReturn(
            Result.failure(RuntimeException())
        )

        viewModel.loadUserDetail(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Failed to load user detail", (state as UiState.Error).message)
    }

    @Test
    fun retryCallsLoadUserDetailAgain() = runTest {
        val detail = buildUserDetail(userId = 1L)
        whenever(getUserDetailUseCase(1L)).thenReturn(Result.success(detail))

        viewModel.retry(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(getUserDetailUseCase, times(1)).invoke(1L)
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun retryAfterErrorRecoversToSuccess() = runTest {
        // First call fails
        whenever(getUserDetailUseCase(1L))
            .thenReturn(Result.failure(RuntimeException("Network error")))

        viewModel.loadUserDetail(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Error)

        // Second call succeeds
        val detail = buildUserDetail(userId = 1L)
        whenever(getUserDetailUseCase(1L)).thenReturn(Result.success(detail))

        viewModel.retry(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    // --- Helpers ---
    @Suppress("SameParameterValue")
    private fun buildUserDetail(userId: Long) = UserDetail(
        user = User(
            userId = userId,
            displayName = "Test User",
            reputation = 1500,
            profileImageUrl = "https://example.com/avatar.jpg",
            location = "London, UK",
            creationDate = 1234567890L,
            badgeCounts = BadgeCounts(gold = 2, silver = 5, bronze = 12),
            profileLink = "https://stackoverflow.com/users/$userId"
        ),
        topTags = listOf(
            TopTag("kotlin", answerCount = 10, questionCount = 5, answerScore = 100, questionScore = 50),
            TopTag("android", answerCount = 8, questionCount = 3, answerScore = 80, questionScore = 30)
        )
    )
}
