package com.stackusers.data.model

data class User(
    val userId: Int,
    val displayName: String,
    val reputation: Int,
    val profileImage: String?
)