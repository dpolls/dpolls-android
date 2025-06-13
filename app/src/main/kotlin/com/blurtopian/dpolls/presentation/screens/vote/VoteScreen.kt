package com.blurtopian.dpolls.presentation.screens.vote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blurtopian.dpolls.presentation.theme.PollsDAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteScreen(
    pollId: String,
    onNavigateBack: () -> Unit,
    onVoteSubmitted: () -> Unit
) {
    // Mock poll data
    val poll = remember {
        PollDetails(
            id = pollId,
            title = "Favorite Programming Language",
            description = "What's your favorite programming language for blockchain development? This poll will help us understand the community preferences.",
            options = listOf(
                VoteOption("1", "Solidity", 45),
                VoteOption("2", "Rust", 32),
                VoteOption("3", "JavaScript", 28),
                VoteOption("4", "Python", 15)
            ),
            totalVotes = 120,
            isActive = true,
            timeRemaining = "2 days 5 hours"
        )
    }
    
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isVoting by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Vote",
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
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poll Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = poll.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = poll.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${poll.totalVotes} total votes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "Ends in ${poll.timeRemaining}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Options Section
            item {
                Text(
                    text = "Select your choice:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            itemsIndexed(poll.options) { index, option ->
                VoteOptionCard(
                    option = option,
                    isSelected = selectedOption == option.id,
                    onSelect = { selectedOption = option.id },
                    totalVotes = poll.totalVotes
                )
            }
            
            // Vote Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        isVoting = true
                        // TODO: Implement voting logic
                        // For now, just simulate voting
                        onVoteSubmitted()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isVoting && selectedOption != null && poll.isActive
                ) {
                    if (isVoting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting Vote...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Vote")
                    }
                }
                
                if (!poll.isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This poll has ended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun VoteOptionCard(
    option: VoteOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    totalVotes: Int
) {
    val percentage = if (totalVotes > 0) (option.votes * 100f / totalVotes) else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${option.votes} votes (${percentage.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class PollDetails(
    val id: String,
    val title: String,
    val description: String,
    val options: List<VoteOption>,
    val totalVotes: Int,
    val isActive: Boolean,
    val timeRemaining: String
)

data class VoteOption(
    val id: String,
    val text: String,
    val votes: Int
)

@Preview(showBackground = true)
@Composable
fun VoteScreenPreview() {
    PollsDAppTheme {
        VoteScreen(
            pollId = "1",
            onNavigateBack = {},
            onVoteSubmitted = {}
        )
    }
}

