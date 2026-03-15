package com.stackusers.domain.usecase

import com.stackusers.data.model.User
import com.stackusers.data.model.UserDetail
import com.stackusers.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case encapsulating the business logic for searching users
 *
 * Responsibilities:
 * - Validates the query - blank queries are rejected early
 * - Delegates to the repository for fetching
 * - Ensures results are sorted alphabetically by display name
 */
class SearchUsersUseCase @Inject constructor(
    private val repository: UserRepository
) {
    /**
     * @param query The search string entered by the user
     * @return Result containing an alphabetically sorted list of up to 20 users,
     *         or a failure if the query is blank or the network call fails
     */
    suspend operator fun invoke(query: String): Result<List<User>> {
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("Search query must not be blank"))
        }
        return repository.searchUsers(query.trim()).map { users ->
            users.sortedBy { it.displayName.lowercase() }
        }
    }
}

/**
 * Use case for retrieving a user's full profile detail
 *
 * Kept intentionally thin — the composition of user + top tags is handled
 * in the repository implementation, so this use case focuses on validation
 * and delegation
 */
class GetUserDetailUseCase @Inject constructor(
    private val repository: UserRepository
) {
    /**
     * @param userId The StackOverflow user ID (must be positive)
     * @return Result containing the user's full detail, or a failure
     */
    suspend operator fun invoke(userId: Long): Result<UserDetail> {
        if (userId <= 0) {
            return Result.failure(IllegalArgumentException("User ID must be a positive value"))
        }
        return repository.getUserDetail(userId)
    }
}