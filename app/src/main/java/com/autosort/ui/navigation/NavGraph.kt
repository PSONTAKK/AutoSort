package com.autosort.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
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
    const val MONTHLY_REPORT = "monthly_report"

    fun editRule(ruleId: String) = "edit_rule/$ruleId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.DASHBOARD
) {
    val ruleViewModel: RuleViewModel         = hiltViewModel()
    val logViewModel: LogViewModel           = hiltViewModel()
    val settingsViewModel: SettingsViewModel  = hiltViewModel()

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        composable(Routes.DASHBOARD) {
            val aiViewModel: com.autosort.ui.viewmodel.AiViewModel = hiltViewModel()

            DashboardScreen(
                viewModel    = ruleViewModel,
                aiViewModel  = aiViewModel,
                onAddRule    = { navController.navigate(Routes.ADD_RULE) },
                onEditRule   = { ruleId -> navController.navigate(Routes.editRule(ruleId)) },
                onViewLogs   = { navController.navigate(Routes.LOGS) },
                onSettings   = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADD_RULE) {
            AddRuleScreen(
                viewModel = ruleViewModel,
                settingsViewModel = settingsViewModel,
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
                settingsViewModel = settingsViewModel,
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

        composable(Routes.MONTHLY_REPORT) {
            val logViewModel: com.autosort.ui.viewmodel.LogViewModel = hiltViewModel()
            com.autosort.ui.screens.MonthlyReportScreen(
                viewModel = logViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
