package com.stackusers.domain

import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.User
import com.stackusers.domain.repository.UserRepository
import com.stackusers.domain.usecase.SearchUsersUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SearchUsersUseCase].
 *
 * Verifies:
 * - Blank/empty queries are rejected
 * - Results are sorted alphabetically by display name (case-insensitive)
 * - Repository failures are propagated as Result.failure
 * - Successful responses are passed correctly
 */
class SearchUsersUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var useCase: SearchUsersUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = SearchUsersUseCase(repository)
    }

    @Test
    fun `invoke with blank query returns failure without calling repository`() = runTest {
        val result = useCase("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke with empty query returns failure without calling repository`() = runTest {
        val result = useCase("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke with valid query returns alphabetically sorted results`() = runTest {
        val unsortedUsers = listOf(
            buildUser(1L, "Zara"),
            buildUser(2L, "Alice"),
            buildUser(3L, "mike"),   // lowercase — case-insensitive sort
            buildUser(4L, "Bob")
        )
        whenever(repository.searchUsers("test")).thenReturn(Result.success(unsortedUsers))

        val result = useCase("test")

        assertTrue(result.isSuccess)
        val names = result.getOrNull()!!.map { it.displayName }
        assertEquals(listOf("Alice", "Bob", "mike", "Zara"), names)
    }

    @Test
    fun `invoke trims whitespace from query before searching`() = runTest {
        whenever(repository.searchUsers("alice")).thenReturn(Result.success(emptyList()))

        val result = useCase("  alice  ")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke returns empty list when repository returns empty list`() = runTest {
        whenever(repository.searchUsers("nobody")).thenReturn(Result.success(emptyList()))

        val result = useCase("nobody")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<User>(), result.getOrNull())
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val exception = RuntimeException("Network error")
        whenever(repository.searchUsers("test")).thenReturn(Result.failure(exception))

        val result = useCase("test")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke with single result returns list with one user`() = runTest {
        val user = buildUser(1L, "Alice")
        whenever(repository.searchUsers("alice")).thenReturn(Result.success(listOf(user)))

        val result = useCase("alice")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("Alice", result.getOrNull()!!.first().displayName)
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
