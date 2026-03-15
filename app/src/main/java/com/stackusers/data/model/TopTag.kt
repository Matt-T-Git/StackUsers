package com.stackusers.data.model

/**
 * Domain model representing a user's top tag on StackOverflow
 */
data class TopTag(
    val tagName: String,
    val answerCount: Int,
    val questionCount: Int,
    val answerScore: Int,
    val questionScore: Int
)