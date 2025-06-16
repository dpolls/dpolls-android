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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.data.web3.Web3Service
import com.blurtopian.dpolls.domain.model.Poll
import com.blurtopian.dpolls.domain.repository.PollsRepository
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import com.blurtopian.dpolls.presentation.theme.PollsDAppTheme
import kotlinx.coroutines.launch
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVote: (String) -> Unit,
    repository: PollsRepositoryImpl
) {
    var polls by remember { mutableStateOf<List<Poll>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadPolls() {
        scope.launch {
            isLoading = true
            polls = repository.getPolls()
            isLoading = false
        }
    }

    // Load polls when the screen is first displayed
    LaunchedEffect(Unit) {
        loadPolls()
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
                IconButton(onClick = { loadPolls() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Polls List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(polls) { poll ->
                    PollCard(
                        poll = PollItem(
                            id = poll.id,
                            title = poll.title,
                            description = poll.description,
                            totalVotes = poll.options.sumOf { it.voteCount.toInt() },
                            isActive = poll.isActive
                        ),
                        onVoteClick = { onNavigateToVote(poll.id) }
                    )
                }
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
        val mockWeb3j = Web3j.build(HttpService("https://mock-rpc-url"))
        val mockWalletManager = WalletManager(LocalContext.current)
        val mockGasProvider: ContractGasProvider = DefaultGasProvider()
        
        PollsScreen(
            onNavigateBack = {},
            onNavigateToVote = {},
            repository = PollsRepositoryImpl(Web3Service(mockWeb3j, mockWalletManager, mockGasProvider))
        )
    }
}

