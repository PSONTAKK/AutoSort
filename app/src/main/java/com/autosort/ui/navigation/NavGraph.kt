package com.autosort.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autosort.ui.screens.AddRuleScreen
import com.autosort.ui.screens.DashboardScreen
import com.autosort.ui.screens.LogsScreen
import com.autosort.ui.screens.SettingsScreen
import com.autosort.ui.viewmodel.LogViewModel
import com.autosort.ui.viewmodel.RuleViewModel
import com.autosort.ui.viewmodel.SettingsViewModel

object Routes {
    const val DASHBOARD  = "dashboard"
    const val ADD_RULE   = "add_rule"
    const val EDIT_RULE  = "edit_rule/{ruleId}"
    const val LOGS       = "logs"
    const val SETTINGS   = "settings"

    fun editRule(ruleId: String) = "edit_rule/$ruleId"
}

@Composable
fun NavGraph(navController: NavHostController) {

    val ruleViewModel: RuleViewModel         = viewModel()
    val logViewModel: LogViewModel           = viewModel()
    val settingsViewModel: SettingsViewModel  = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel    = ruleViewModel,
                onAddRule    = { navController.navigate(Routes.ADD_RULE) },
                onEditRule   = { ruleId -> navController.navigate(Routes.editRule(ruleId)) },
                onViewLogs   = { navController.navigate(Routes.LOGS) },
                onSettings   = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADD_RULE) {
            AddRuleScreen(
                viewModel = ruleViewModel,
                editRuleId = null,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_RULE,
            arguments = listOf(navArgument("ruleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")
            AddRuleScreen(
                viewModel  = ruleViewModel,
                editRuleId = ruleId,
                onBack     = { navController.popBackStack() }
            )
        }

        composable(Routes.LOGS) {
            LogsScreen(
                viewModel = logViewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
