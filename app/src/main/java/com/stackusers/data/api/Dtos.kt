package com.stackusers.data.api

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for all StackExchange API list responses.
 * The API always returns items nested within this wrapper.
 */
data class StackExchangeResponse<T>(
    @SerializedName("items") val items: List<T> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("quota_max") val quotaMax: Int = 0,
    @SerializedName("quota_remaining") val quotaRemaining: Int = 0
)

/**
 * Raw DTO for a StackExchange user, mapped directly from the API response
 * Not to be used directly in the UI layer — mapped to domain model instead!
 */
data class UserDto(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("reputation") val reputation: Int,
    @SerializedName("profile_image") val profileImage: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("creation_date") val creationDate: Long,
    @SerializedName("badge_counts") val badgeCounts: BadgeCountsDto?,
    @SerializedName("link") val profileLink: String?
)

/**
 * User badge count (gold, silver, bronze)
 */
data class BadgeCountsDto(
    @SerializedName("gold") val gold: Int = 0,
    @SerializedName("silver") val silver: Int = 0,
    @SerializedName("bronze") val bronze: Int = 0
)

/**
 * DTO for a user's top tag entry.
 */
data class TopTagDto(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("answer_count") val answerCount: Int = 0,
    @SerializedName("question_count") val questionCount: Int = 0,
    @SerializedName("answer_score") val answerScore: Int = 0,
    @SerializedName("question_score") val questionScore: Int = 0
)
