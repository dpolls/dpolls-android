package com.blurtopian.dpolls.domain.repository

import com.blurtopian.dpolls.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

/**
 * Repository interface for poll operations
 */
interface PollRepository {
    
    /**
     * Get active polls with pagination
     */
    suspend fun getActivePolls(offset: Int = 0, limit: Int = 20): Result<List<Poll>>
    
    /**
     * Get poll by ID
     */
    suspend fun getPollById(pollId: BigInteger): Result<Poll?>
    
    /**
     * Create a new poll
     */
    suspend fun createPoll(request: CreatePollRequest): Result<BigInteger>
    
    /**
     * Vote on a poll
     */
    suspend fun vote(request: VoteRequest): Result<String> // Returns transaction hash
    
    /**
     * Get poll results
     */
    suspend fun getPollResults(pollId: BigInteger): Result<PollResults>
    
    /**
     * End a poll (creator only)
     */
    suspend fun endPoll(pollId: BigInteger): Result<String> // Returns transaction hash
    
    /**
     * Get polls created by user
     */
    suspend fun getUserCreatedPolls(userAddress: String): Result<List<Poll>>
    
    /**
     * Get polls voted on by user
     */
    suspend fun getUserVotedPolls(userAddress: String): Result<List<Poll>>
    
    /**
     * Get total polls count
     */
    suspend fun getTotalPollsCount(): Result<BigInteger>
    
    /**
     * Check if poll has ended
     */
    suspend fun isPollEnded(pollId: BigInteger): Result<Boolean>
    
    /**
     * Get user's vote information for a poll
     */
    suspend fun getUserVoteInfo(pollId: BigInteger, userAddress: String): Result<UserVote?>
    
    /**
     * Observe poll updates (for real-time updates)
     */
    fun observePollUpdates(): Flow<Poll>
    
    /**
     * Observe vote events (for real-time vote counting)
     */
    fun observeVoteEvents(): Flow<VoteEvent>
    
    /**
     * Refresh polls from blockchain
     */
    suspend fun refreshPolls(): Result<Unit>
}

/**
 * Repository interface for wallet operations
 */
interface WalletRepository {
    
    /**
     * Connect wallet using Web3Auth
     */
    suspend fun connectWithWeb3Auth(): Result<WalletInfo>
    
    /**
     * Connect wallet using private key
     */
    suspend fun connectWithPrivateKey(privateKey: String): Result<WalletInfo>
    
    /**
     * Generate new wallet
     */
    suspend fun generateNewWallet(): Result<Pair<String, String>> // Returns (privateKey, address)
    
    /**
     * Get current wallet info
     */
    suspend fun getCurrentWalletInfo(): Result<WalletInfo?>
    
    /**
     * Get wallet balance
     */
    suspend fun getBalance(address: String? = null): Result<BigInteger>
    
    /**
     * Sign message
     */
    suspend fun signMessage(message: String): Result<String>
    
    /**
     * Disconnect wallet
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Check if wallet is connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get current address
     */
    fun getCurrentAddress(): String?
    
    /**
     * Export private key
     */
    fun exportPrivateKey(): String?
    
    /**
     * Observe wallet connection status
     */
    fun observeConnectionStatus(): Flow<Boolean>
    
    /**
     * Observe balance changes
     */
    fun observeBalance(): Flow<BigInteger>
}

/**
 * Repository interface for blockchain operations
 */
interface BlockchainRepository {
    
    /**
     * Initialize blockchain connection
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Check connection status
     */
    fun isConnected(): Boolean
    
    /**
     * Get current network info
     */
    suspend fun getNetworkInfo(): Result<NetworkInfo>
    
    /**
     * Verify network (check if connected to correct chain)
     */
    suspend fun verifyNetwork(): Result<Boolean>
    
    /**
     * Get current gas price
     */
    suspend fun getCurrentGasPrice(): Result<BigInteger>
    
    /**
     * Estimate gas for transaction
     */
    suspend fun estimateGas(
        from: String,
        to: String,
        data: String
    ): Result<GasEstimate>
    
    /**
     * Get transaction info
     */
    suspend fun getTransactionInfo(txHash: String): Result<TransactionInfo>
    
    /**
     * Get current block number
     */
    suspend fun getCurrentBlockNumber(): Result<BigInteger>
    
    /**
     * Reconnect to blockchain
     */
    suspend fun reconnect(): Result<Unit>
    
    /**
     * Observe connection status
     */
    fun observeConnectionStatus(): Flow<Boolean>
    
    /**
     * Observe block updates
     */
    fun observeBlockUpdates(): Flow<BigInteger>
}

/**
 * Data class for vote events
 */
data class VoteEvent(
    val pollId: BigInteger,
    val voter: String,
    val optionIndex: Int,
    val timestamp: Long
)

