package com.stackusers.domain

import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.TopTag
import com.stackusers.data.model.User
import com.stackusers.data.model.UserDetail
import com.stackusers.domain.repository.UserRepository
import com.stackusers.domain.usecase.GetUserDetailUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [GetUserDetailUseCase].
 *
 * Verifies:
 * - Invalid user IDs (zero or negative) are rejected without hitting repo
 * - Valid IDs are delegated to the repository
 * - Repository failures are propagated correctly
 */
class GetUserDetailUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: GetUserDetailUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetUserDetailUseCase(repository)
    }

    @Test
    fun invokeWithZeroUserIdReturnsFailure() = runTest {
        val result = useCase(0L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun invokeWithNegativeUserIdReturnsFailure() = runTest {
        val result = useCase(-1L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun invokeWithValidUserIdDelegatesToRepository() = runTest {
        val detail = buildUserDetail(userId = 42L)
        whenever(repository.getUserDetail(42L)).thenReturn(Result.success(detail))

        val result = useCase(42L)

        assertTrue(result.isSuccess)
        verify(repository).getUserDetail(42L)
    }

    @Test
    fun invokeReturnsUserDetailOnSuccess() = runTest {
        val detail = buildUserDetail(userId = 1L)
        whenever(repository.getUserDetail(1L)).thenReturn(Result.success(detail))

        val result = useCase(1L)

        assertEquals(detail, result.getOrNull())
    }

    @Test
    fun invokePropagatesRepositoryFailure() = runTest {
        val exception = RuntimeException("Not found")
        whenever(repository.getUserDetail(99L)).thenReturn(Result.failure(exception))

        val result = useCase(99L)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    // --- Helpers ---

    private fun buildUserDetail(userId: Long) = UserDetail(
        user = User(
            userId = userId,
            displayName = "Test User",
            reputation = 500,
            profileImageUrl = null,
            location = "London, UK",
            creationDate = 1234567890L,
            badgeCounts = BadgeCounts(gold = 1, silver = 3, bronze = 10),
            profileLink = null
        ),
        topTags = listOf(
            TopTag("kotlin", 10, 5, 100, 50),
            TopTag("android", 8, 3, 80, 30)
        )
    )
}
