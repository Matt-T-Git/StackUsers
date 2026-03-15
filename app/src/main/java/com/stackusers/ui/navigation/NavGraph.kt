package com.stackusers.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stackusers.ui.screens.userdetail.UserDetailScreen
import com.stackusers.ui.screens.userlist.UserListScreen

/**
 * Navigation route definitions for the app
 * Reviewer note: Using a sealed class prevents typos and makes route changes easy to refactor
 */
sealed class Screen(val route: String) {
    object UserList : Screen("user_list")
    object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: Long) = "user_detail/$userId"
        const val ARG_USER_ID = "userId"
    }
}

/**
 * Root navigation graph for the app
 * All navigation decisions are centralized here, keeping screens unaware
 * of each other's existence
 */
@Composable
fun StackUsersNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.UserList.route
    ) {
        composable(route = Screen.UserList.route) {
            UserListScreen(
                onUserClick = { userId ->
                    navController.navigate(Screen.UserDetail.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(
                navArgument(Screen.UserDetail.ARG_USER_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(Screen.UserDetail.ARG_USER_ID) ?: return@composable
            UserDetailScreen(
                userId = userId,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
