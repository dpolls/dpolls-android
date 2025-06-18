package com.blurtopian.dpolls.presentation.navigation

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.data.web3.Web3Service
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import com.blurtopian.dpolls.presentation.screens.create.CreatePollScreen
import com.blurtopian.dpolls.presentation.screens.home.HomeScreen
import com.blurtopian.dpolls.presentation.screens.polls.PollsScreen
import com.blurtopian.dpolls.presentation.screens.vote.VoteScreen
import com.blurtopian.dpolls.presentation.screens.wallet.WalletScreen
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Polls : BottomNavItem("polls", Icons.Default.Poll, "Polls")
    object Create : BottomNavItem("create", Icons.Default.Add, "Create")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    @ApplicationContext context: Context,
    walletManager: WalletManager,
    web3Service: Web3Service,
    pollsRepository: PollsRepositoryImpl
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Polls,
        BottomNavItem.Create
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
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
                    },
                    repository = pollsRepository
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
                    onNavigateBack = { navController.popBackStack() },
                    walletManager = walletManager
                )
            }
        }
    }
}

