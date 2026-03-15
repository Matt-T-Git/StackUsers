package com.stackusers.data.repository

import com.stackusers.data.api.BadgeCountsDto
import com.stackusers.data.api.StackExchangeApiService
import com.stackusers.data.api.TopTagDto
import com.stackusers.data.api.UserDto
import com.stackusers.data.model.BadgeCounts
import com.stackusers.data.model.TopTag
import com.stackusers.data.model.User
import com.stackusers.data.model.UserDetail
import com.stackusers.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [UserRepository] using the StackExchange REST API.
 * Handles network calls, maps DTOs to domain models, and wraps
 * any exceptions in [Result] so callers don't need to try/catch.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: StackExchangeApiService
) : UserRepository {

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return runCatching {
            val response = apiService.searchUsers(inname = query)
            response.items.map { it.toDomain() }
        }
    }

    /**
     * Fetches the user and their top tags sequentially.
     * Both calls are wrapped in runCatching so either failing
     * returns a clean Result .failure to the caller.
     */
    override suspend fun getUserDetail(userId: Long): Result<UserDetail> {
        return runCatching {
            val userResponse = apiService.getUserById(userId)
            val user = userResponse.items.firstOrNull()
                ?: throw NoSuchElementException("User with ID $userId not found")

            val topTagsResponse = apiService.getUserTopTags(userId)

            UserDetail(
                user = user.toDomain(),
                topTags = topTagsResponse.items.map { it.toDomain() }
            )
        }
    }

    // Mapping extensions kept private here rather than as top-level functions
    // to avoid leaking data-layer concerns into the rest of the codebase.

    private fun UserDto.toDomain(): User = User(
        userId = userId,
        displayName = displayName,
        reputation = reputation,
        profileImageUrl = profileImage,
        location = location,
        creationDate = creationDate,
        badgeCounts = badgeCounts?.toDomain() ?: BadgeCounts(0, 0, 0),
        profileLink = profileLink
    )

    private fun BadgeCountsDto.toDomain(): BadgeCounts = BadgeCounts(
        gold = gold,
        silver = silver,
        bronze = bronze
    )

    private fun TopTagDto.toDomain(): TopTag = TopTag(
        tagName = tagName,
        answerCount = answerCount,
        questionCount = questionCount,
        answerScore = answerScore,
        questionScore = questionScore
    )
}