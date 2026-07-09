package com.taskflow.demo.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Workspaces : Screen("workspaces")
    data object WorkspaceBoard : Screen("workspace/{workspaceId}") {
        fun createRoute(workspaceId: String): String = "workspace/$workspaceId"
    }
    data object TaskDetail : Screen("task/{taskId}") {
        fun createRoute(taskId: String): String = "task/$taskId"
    }
    data object Dashboard : Screen("dashboard/{workspaceId}") {
        fun createRoute(workspaceId: String): String = "dashboard/$workspaceId"
    }
    data object Profile : Screen("profile")
}
