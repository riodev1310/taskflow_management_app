package com.taskflow.demo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskflow.demo.presentation.auth.LoginScreen
import com.taskflow.demo.presentation.auth.LoginViewModel
import com.taskflow.demo.presentation.auth.RegisterScreen
import com.taskflow.demo.presentation.auth.RegisterViewModel
import com.taskflow.demo.presentation.auth.SplashScreen
import com.taskflow.demo.presentation.auth.SplashViewModel
import com.taskflow.demo.presentation.dashboard.DashboardScreen
import com.taskflow.demo.presentation.dashboard.DashboardViewModel
import com.taskflow.demo.presentation.profile.ProfileScreen
import com.taskflow.demo.presentation.profile.ProfileViewModel
import com.taskflow.demo.presentation.project.ProjectBoardScreen
import com.taskflow.demo.presentation.project.ProjectBoardViewModel
import com.taskflow.demo.presentation.task.TaskDetailScreen
import com.taskflow.demo.presentation.task.TaskDetailViewModel
import com.taskflow.demo.presentation.workspace.WorkspaceListScreen
import com.taskflow.demo.presentation.workspace.WorkspaceListViewModel

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            val viewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = viewModel,
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateWorkspaces = {
                    navController.navigate(Screen.Workspaces.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onNavigateRegister = { navController.navigate(Screen.Register.route) },
                onNavigateWorkspaces = {
                    navController.navigate(Screen.Workspaces.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Screen.Workspaces.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Workspaces.route) {
            val viewModel: WorkspaceListViewModel = hiltViewModel()
            WorkspaceListScreen(
                viewModel = viewModel,
                onOpenWorkspace = { workspaceId -> navController.navigate(Screen.WorkspaceBoard.createRoute(workspaceId)) },
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Workspaces.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.WorkspaceBoard.route,
            arguments = listOf(navArgument("workspaceId") { type = NavType.StringType })
        ) {
            val viewModel: ProjectBoardViewModel = hiltViewModel()
            ProjectBoardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenTask = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onOpenDashboard = { workspaceId -> navController.navigate(Screen.Dashboard.createRoute(workspaceId)) },
                onOpenProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            val viewModel: TaskDetailViewModel = hiltViewModel()
            TaskDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("workspaceId") { type = NavType.StringType })
        ) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
