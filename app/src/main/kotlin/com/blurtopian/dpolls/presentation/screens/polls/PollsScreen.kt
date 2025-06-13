package com.blurtopian.dpolls.presentation.screens.polls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blurtopian.dpolls.presentation.theme.PollsDAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVote: (String) -> Unit
) {
    // Mock data for preview
    val mockPolls = remember {
        listOf(
            PollItem("1", "Favorite Programming Language", "What's your favorite programming language for blockchain development?", 156, true),
            PollItem("2", "Best DeFi Protocol", "Which DeFi protocol do you trust the most?", 89, true),
            PollItem("3", "NFT Market Prediction", "Where do you think the NFT market is heading?", 234, false)
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Active Polls",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* Refresh polls */ }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        )
        
        // Polls List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mockPolls) { poll ->
                PollCard(
                    poll = poll,
                    onVoteClick = { onNavigateToVote(poll.id) }
                )
            }
        }
    }
}

@Composable
fun PollCard(
    poll: PollItem,
    onVoteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = poll.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = poll.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                Surface(
                    color = if (poll.isActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (poll.isActive) "Active" else "Ended",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (poll.isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${poll.totalVotes} votes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (poll.isActive) {
                    Button(
                        onClick = onVoteClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Vote")
                    }
                } else {
                    OutlinedButton(
                        onClick = onVoteClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("View Results")
                    }
                }
            }
        }
    }
}

data class PollItem(
    val id: String,
    val title: String,
    val description: String,
    val totalVotes: Int,
    val isActive: Boolean
)

@Preview(showBackground = true)
@Composable
fun PollsScreenPreview() {
    PollsDAppTheme {
        PollsScreen(
            onNavigateBack = {},
            onNavigateToVote = {}
        )
    }
}

