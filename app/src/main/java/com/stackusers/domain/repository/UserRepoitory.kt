package com.stackusers.domain.repository

import com.stackusers.data.model.User
import com.stackusers.data.model.UserDetail

/**
 * Repository interface defined in the domain layer.
 * This abstraction ensures the domain and UI layers remain decoupled
 * from any specific data source implementation (network, cache, etc.).
 *
 * The implementation lives in the data layer and is injected via Hilt.
 */
interface UserRepository {

    /**
     * Search for users matching the given name fragment
     * Results are sorted alphabetically and limited to 20 items
     *
     * @param query Name fragment to search for
     * @return Result wrapping list of users, or a failure
     */
    suspend fun searchUsers(query: String): Result<List<User>>

    /**
     * Retrieve the full detail for a single user, including their top tags
     *
     * @param userId The StackOverflow user ID
     * @return Result wrapping the user's full detail, or a failure
     */
    suspend fun getUserDetail(userId: Long): Result<UserDetail>
}
