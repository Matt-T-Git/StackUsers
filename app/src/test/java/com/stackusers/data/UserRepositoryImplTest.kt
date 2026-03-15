package com.stackusers.data

import com.stackusers.data.api.BadgeCountsDto
import com.stackusers.data.api.StackExchangeApiService
import com.stackusers.data.api.StackExchangeResponse
import com.stackusers.data.api.TopTagDto
import com.stackusers.data.api.UserDto
import com.stackusers.data.repository.UserRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [UserRepositoryImpl].
 *
 * Verifies correct mapping from DTOs to domain models and proper error handling
 * when API returns unexpected responses
 */
class UserRepositoryImplTest {

    private lateinit var apiService: StackExchangeApiService
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        apiService = mock()
        repository = UserRepositoryImpl(apiService)
    }

    @Test
    fun searchUsersMapsUserDtoListToUserDomainModelsCorrectly() = runTest {
        val dtos = listOf(
            buildUserDto(id = 1L, name = "Alice", reputation = 1500),
            buildUserDto(id = 2L, name = "Bob", reputation = 300)
        )
        whenever(apiService.searchUsers(inname = "al")).thenReturn(
            StackExchangeResponse(items = dtos)
        )

        val result = repository.searchUsers("al")

        assertTrue(result.isSuccess)
        val users = result.getOrNull()!!
        assertEquals(2, users.size)
        assertEquals(1L, users[0].userId)
        assertEquals("Alice", users[0].displayName)
        assertEquals(1500, users[0].reputation)
        assertEquals(2L, users[1].userId)
    }

    @Test
    fun searchUsersMapsBadgeCountsCorrectly() = runTest {
        val dto = buildUserDto(
            id = 1L,
            badgeCounts = BadgeCountsDto(gold = 5, silver = 10, bronze = 20)
        )
        whenever(apiService.searchUsers(inname = "test")).thenReturn(
            StackExchangeResponse(items = listOf(dto))
        )

        val result = repository.searchUsers("test")

        val badges = result.getOrNull()!!.first().badgeCounts
        assertEquals(5, badges.gold)
        assertEquals(10, badges.silver)
        assertEquals(20, badges.bronze)
    }

    @Test
    fun searchUsersReturnsEmptyListWhenApiReturnsEmptyItems() = runTest {
        whenever(apiService.searchUsers(inname = "nobody")).thenReturn(
            StackExchangeResponse(items = emptyList())
        )

        val result = repository.searchUsers("nobody")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun searchUsersWrapsApiExceptionInResultFailure() = runTest {
        whenever(apiService.searchUsers(inname = "fail")).thenThrow(
            RuntimeException("Timeout")
        )

        val result = repository.searchUsers("fail")

        assertTrue(result.isFailure)
        assertEquals("Timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun getUserDetailReturnsFailureWhenUserNotFoundInResponse() = runTest {
        whenever(apiService.getUserById(999L)).thenReturn(
            StackExchangeResponse(items = emptyList())
        )

        val result = repository.getUserDetail(999L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getUserDetailMapsTopTagsCorrectly() = runTest {
        val userDto = buildUserDto(id = 1L)
        val topTagDtos = listOf(
            TopTagDto("kotlin", answerCount = 10, questionCount = 5, answerScore = 100, questionScore = 50),
            TopTagDto("android", answerCount = 8, questionCount = 3, answerScore = 80, questionScore = 30)
        )

        whenever(apiService.getUserById(1L)).thenReturn(
            StackExchangeResponse(items = listOf(userDto))
        )
        whenever(apiService.getUserTopTags(1L)).thenReturn(
            StackExchangeResponse(items = topTagDtos)
        )

        val result = repository.getUserDetail(1L)

        assertTrue(result.isSuccess)
        val detail = result.getOrNull()!!
        assertEquals(2, detail.topTags.size)
        assertEquals("kotlin", detail.topTags[0].tagName)
        assertEquals(10, detail.topTags[0].answerCount)
    }

    @Test
    fun getUserDetailHandlesNullBadgeCountsByDefaultingToZero() = runTest {
        val dto = buildUserDto(id = 1L, badgeCounts = null)
        whenever(apiService.getUserById(1L)).thenReturn(
            StackExchangeResponse(items = listOf(dto))
        )
        whenever(apiService.getUserTopTags(1L)).thenReturn(
            StackExchangeResponse(items = emptyList())
        )

        val result = repository.getUserDetail(1L)

        assertTrue(result.isSuccess)
        val badges = result.getOrNull()!!.user.badgeCounts
        assertEquals(0, badges.gold)
        assertEquals(0, badges.silver)
        assertEquals(0, badges.bronze)
    }

    // --- Helpers ---

    private fun buildUserDto(
        id: Long = 1L,
        name: String = "Test User",
        reputation: Int = 100,
        badgeCounts: BadgeCountsDto? = BadgeCountsDto(1, 2, 3)
    ) = UserDto(
        userId = id,
        displayName = name,
        reputation = reputation,
        profileImage = "https://example.com/avatar.jpg",
        location = "London, UK",
        creationDate = 1234567890L,
        badgeCounts = badgeCounts,
        profileLink = "https://stackoverflow.com/users/$id"
    )
}
