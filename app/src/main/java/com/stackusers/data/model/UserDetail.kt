package com.stackusers.data.model

/**
 * Model bundling a user's full profile detail, including top tags
 * Used exclusively on the detail screen
 */
data class UserDetail(
    val user: User,
    val topTags: List<TopTag>
)