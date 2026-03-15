package com.stackusers.data.model

data class User(
    val userId: Long,
    val displayName: String,
    val reputation: Int,
    val profileImageUrl: String?,
    val location: String?,
    val creationDate: Long,
    val badgeCounts: BadgeCounts,
    val profileLink: String?
)