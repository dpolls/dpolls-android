package com.blurtopian.dpolls.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blurtopian.dpolls.presentation.screens.home.HomeScreen
import com.blurtopian.dpolls.presentation.screens.polls.PollsScreen
import com.blurtopian.dpolls.presentation.screens.create.CreatePollScreen
import com.blurtopian.dpolls.presentation.screens.vote.VoteScreen
import com.blurtopian.dpolls.presentation.screens.wallet.WalletScreen

@Composable
fun PollsNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToPolls = { navController.navigate("polls") },
                onNavigateToCreate = { navController.navigate("create") },
                onNavigateToWallet = { navController.navigate("wallet") }
            )
        }
        
        composable("polls") {
            PollsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVote = { pollId -> 
                    navController.navigate("vote/$pollId")
                }
            )
        }
        
        composable("create") {
            CreatePollScreen(
                onNavigateBack = { navController.popBackStack() },
                onPollCreated = { 
                    navController.popBackStack()
                    navController.navigate("polls")
                }
            )
        }
        
        composable("vote/{pollId}") { backStackEntry ->
            val pollId = backStackEntry.arguments?.getString("pollId") ?: ""
            VoteScreen(
                pollId = pollId,
                onNavigateBack = { navController.popBackStack() },
                onVoteSubmitted = { navController.popBackStack() }
            )
        }
        
        composable("wallet") {
            WalletScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

