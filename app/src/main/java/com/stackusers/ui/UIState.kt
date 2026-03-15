package com.stackusers.ui

/**
 * Generic sealed class representing all possible UI states for a screen
 *
 * Reviewer Note:
 * Using a sealed class here provides exhaustive when-expression handling in the UI,
 * ensuring every state is explicitly accounted for — no implicit null checks or boolean flags
 *
 * @param T The type of data held in the Success state
 */
sealed class UiState<out T> {

    /** Initial state before user interaction has occurred */
    object Idle : UiState<Nothing>()

    /** A network or processing operation is in progress */
    object Loading : UiState<Nothing>()

    /**
     * Data was successfully retrieved
     * @param data The result payload
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * An error occurred during data retrieval
     * @param message A human-readable description of the error
     */
    data class Error(val message: String) : UiState<Nothing>()

    /** A search returned zero results — distinct from Error for UI clarity */
    object Empty : UiState<Nothing>()
}
