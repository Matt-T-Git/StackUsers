package com.stackusers.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface defining all StackExchange API endpoints used.
 * Base URL: https://api.stackexchange.com/2.3/
 */
interface StackExchangeApiService {

    /**
     * Search for users by name fragment.
     * Results are fetched with pagesize capped at 20 as per test requirements.
     *
     * @param inname  Name fragment to search for
     * @param pageSize Maximum number of results
     * @param order   Sort order for results
     * @param sort    Sort field
     * @param site    StackExchange site to query
     */
    @GET("users")
    suspend fun searchUsers(
        @Query("inname") inname: String,
        @Query("pagesize") pageSize: Int = 20,
        @Query("order") order: String = "asc",
        @Query("sort") sort: String = "name",
        @Query("site") site: String = "stackoverflow"
    ): StackExchangeResponse<UserDto>

    /**
     * Retrieve detailed info for a single user by their ID.
     *
     * @param userId  The unique user ID on StackOverflow
     * @param site    StackExchange site to query
     */
    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") userId: Long,
        @Query("site") site: String = "stackoverflow"
    ): StackExchangeResponse<UserDto>

    /**
     * Retrieve the top tags a user has participated in.
     * Used to populate the 'Top Tags' section on the detail screen.
     *
     * @param userId  The unique user ID on StackOverflow
     * @param pageSize Number of top tags to retrieve
     * @param site    StackExchange site to query
     */
    @GET("users/{id}/top-tags")
    suspend fun getUserTopTags(
        @Path("id") userId: Long,
        @Query("pagesize") pageSize: Int = 5,
        @Query("site") site: String = "stackoverflow"
    ): StackExchangeResponse<TopTagDto>
}
