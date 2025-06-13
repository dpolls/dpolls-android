package com.blurtopian.dpolls.data.repository

import android.util.Log
import com.blurtopian.dpolls.data.blockchain.PollsContract
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.domain.model.*
import com.blurtopian.dpolls.domain.repository.PollRepository
import com.blurtopian.dpolls.domain.repository.VoteEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PollRepository
 */
@Singleton
class PollRepositoryImpl @Inject constructor(
    private val pollsContract: PollsContract,
    private val walletManager: WalletManager
) : PollRepository {
    
    companion object {
        private const val TAG = "PollRepositoryImpl"
    }
    
    override suspend fun getActivePolls(offset: Int, limit: Int): Result<List<Poll>> = 
        withContext(Dispatchers.IO) {
            try {
                val pollIds = pollsContract.getActivePolls(
                    offset = BigInteger.valueOf(offset.toLong()),
                    limit = BigInteger.valueOf(limit.toLong())
                )
                
                val polls = mutableListOf<Poll>()
                
                for (pollId in pollIds) {
                    val pollDetails = pollsContract.getPoll(pollId)
                    val pollOptions = pollsContract.getPollOptions(pollId)
                    
                    if (pollDetails != null && pollOptions != null) {
                        // Get user vote info if wallet is connected
                        val userVote = if (walletManager.isConnected()) {
                            val userAddress = walletManager.getCurrentAddress()
                            if (userAddress != null) {
                                val voterInfo = pollsContract.getVoterInfo(pollId, userAddress)
                                voterInfo?.let {
                                    UserVote(
                                        hasVoted = it.hasVoted,
                                        voteTimestamp = it.voteTimestamp,
                                        optionIndex = it.optionIndex,
                                        weight = it.weight
                                    )
                                }
                            } else null
                        } else null
                        
                        val options = pollOptions.texts.mapIndexed { index, text ->
                            PollOption(
                                index = index,
                                text = text,
                                voteCount = pollOptions.voteCounts[index]
                            )
                        }
                        
                        val poll = Poll(
                            id = pollDetails.id,
                            title = pollDetails.title,
                            description = pollDetails.description,
                            creator = pollDetails.creator,
                            startTime = pollDetails.startTime,
                            endTime = pollDetails.endTime,
                            isActive = pollDetails.isActive,
                            totalVotes = pollDetails.totalVotes,
                            options = options,
                            userVote = userVote
                        )
                        
                        polls.add(poll)
                    }
                }
                
                Log.d(TAG, "Retrieved ${polls.size} active polls")
                Result.success(polls)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting active polls", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getPollById(pollId: BigInteger): Result<Poll?> = 
        withContext(Dispatchers.IO) {
            try {
                val pollDetails = pollsContract.getPoll(pollId)
                val pollOptions = pollsContract.getPollOptions(pollId)
                
                if (pollDetails == null || pollOptions == null) {
                    return@withContext Result.success(null)
                }
                
                // Get user vote info if wallet is connected
                val userVote = if (walletManager.isConnected()) {
                    val userAddress = walletManager.getCurrentAddress()
                    if (userAddress != null) {
                        val voterInfo = pollsContract.getVoterInfo(pollId, userAddress)
                        voterInfo?.let {
                            UserVote(
                                hasVoted = it.hasVoted,
                                voteTimestamp = it.voteTimestamp,
                                optionIndex = it.optionIndex,
                                weight = it.weight
                            )
                        }
                    } else null
                } else null
                
                val options = pollOptions.texts.mapIndexed { index, text ->
                    PollOption(
                        index = index,
                        text = text,
                        voteCount = pollOptions.voteCounts[index]
                    )
                }
                
                val poll = Poll(
                    id = pollDetails.id,
                    title = pollDetails.title,
                    description = pollDetails.description,
                    creator = pollDetails.creator,
                    startTime = pollDetails.startTime,
                    endTime = pollDetails.endTime,
                    isActive = pollDetails.isActive,
                    totalVotes = pollDetails.totalVotes,
                    options = options,
                    userVote = userVote
                )
                
                Log.d(TAG, "Retrieved poll: ${poll.title}")
                Result.success(poll)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting poll by ID: $pollId", e)
                Result.failure(e)
            }
        }
    
    override suspend fun createPoll(request: CreatePollRequest): Result<BigInteger> = 
        withContext(Dispatchers.IO) {
            try {
                val credentials = walletManager.getCurrentCredentials()
                    ?: return@withContext Result.failure(Exception("Wallet not connected"))
                
                // Validate request
                val validationErrors = request.validate()
                if (validationErrors.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Validation failed: ${validationErrors.joinToString(", ")}"))
                }
                
                val receipt = pollsContract.createPoll(
                    credentials = credentials,
                    title = request.title,
                    description = request.description,
                    options = request.options,
                    durationInHours = BigInteger.valueOf(request.durationInHours.toLong()),
                    allowMultipleVotes = request.allowMultipleVotes
                )
                
                // Extract poll ID from transaction logs
                val pollId = extractPollIdFromReceipt(receipt.transactionHash)
                    ?: return@withContext Result.failure(Exception("Failed to extract poll ID from transaction"))
                
                Log.d(TAG, "Created poll with ID: $pollId")
                Result.success(pollId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating poll", e)
                Result.failure(e)
            }
        }
    
    override suspend fun vote(request: VoteRequest): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val credentials = walletManager.getCurrentCredentials()
                    ?: return@withContext Result.failure(Exception("Wallet not connected"))
                
                val receipt = pollsContract.vote(
                    credentials = credentials,
                    pollId = request.pollId,
                    optionIndex = BigInteger.valueOf(request.optionIndex.toLong())
                )
                
                Log.d(TAG, "Vote cast successfully. Transaction: ${receipt.transactionHash}")
                Result.success(receipt.transactionHash)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error casting vote", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getPollResults(pollId: BigInteger): Result<PollResults> = 
        withContext(Dispatchers.IO) {
            try {
                val poll = getPollById(pollId).getOrNull()
                    ?: return@withContext Result.failure(Exception("Poll not found"))
                
                val optionResults = poll.options.map { option ->
                    OptionResult(
                        option = option,
                        voteCount = option.voteCount,
                        percentage = option.getPercentage(poll.totalVotes)
                    )
                }
                
                val results = PollResults(
                    poll = poll,
                    optionResults = optionResults
                )
                
                Log.d(TAG, "Retrieved poll results for poll: ${poll.title}")
                Result.success(results)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting poll results", e)
                Result.failure(e)
            }
        }
    
    override suspend fun endPoll(pollId: BigInteger): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val credentials = walletManager.getCurrentCredentials()
                    ?: return@withContext Result.failure(Exception("Wallet not connected"))
                
                val receipt = pollsContract.endPoll(
                    credentials = credentials,
                    pollId = pollId
                )
                
                Log.d(TAG, "Poll ended successfully. Transaction: ${receipt.transactionHash}")
                Result.success(receipt.transactionHash)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error ending poll", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getUserCreatedPolls(userAddress: String): Result<List<Poll>> = 
        withContext(Dispatchers.IO) {
            try {
                // This would require additional contract methods to track user-created polls
                // For now, return empty list
                Log.d(TAG, "Getting user created polls for: $userAddress")
                Result.success(emptyList())
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user created polls", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getUserVotedPolls(userAddress: String): Result<List<Poll>> = 
        withContext(Dispatchers.IO) {
            try {
                // This would require additional contract methods to track user votes
                // For now, return empty list
                Log.d(TAG, "Getting user voted polls for: $userAddress")
                Result.success(emptyList())
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user voted polls", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getTotalPollsCount(): Result<BigInteger> = 
        withContext(Dispatchers.IO) {
            try {
                val count = pollsContract.getTotalPollsCount()
                Log.d(TAG, "Total polls count: $count")
                Result.success(count)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting total polls count", e)
                Result.failure(e)
            }
        }
    
    override suspend fun isPollEnded(pollId: BigInteger): Result<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val isEnded = pollsContract.isPollEnded(pollId)
                Log.d(TAG, "Poll $pollId ended: $isEnded")
                Result.success(isEnded)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if poll ended", e)
                Result.failure(e)
            }
        }
    
    override suspend fun getUserVoteInfo(pollId: BigInteger, userAddress: String): Result<UserVote?> = 
        withContext(Dispatchers.IO) {
            try {
                val voterInfo = pollsContract.getVoterInfo(pollId, userAddress)
                
                val userVote = voterInfo?.let {
                    UserVote(
                        hasVoted = it.hasVoted,
                        voteTimestamp = it.voteTimestamp,
                        optionIndex = it.optionIndex,
                        weight = it.weight
                    )
                }
                
                Log.d(TAG, "User vote info for $userAddress on poll $pollId: $userVote")
                Result.success(userVote)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user vote info", e)
                Result.failure(e)
            }
        }
    
    override fun observePollUpdates(): Flow<Poll> = flow {
        // Implementation for real-time poll updates would go here
        // This would typically involve listening to blockchain events
    }
    
    override fun observeVoteEvents(): Flow<VoteEvent> = flow {
        // Implementation for real-time vote events would go here
        // This would typically involve listening to VoteCast events
    }
    
    override suspend fun refreshPolls(): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Refresh logic would go here
                // This could involve clearing cache and fetching fresh data
                Log.d(TAG, "Refreshing polls")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing polls", e)
                Result.failure(e)
            }
        }
    
    /**
     * Extract poll ID from transaction receipt
     * This is a simplified implementation - in practice, you'd parse the transaction logs
     */
    private suspend fun extractPollIdFromReceipt(transactionHash: String): BigInteger? {
        return try {
            // For now, return a mock poll ID
            // In a real implementation, you'd parse the PollCreated event from the transaction logs
            BigInteger.ONE
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting poll ID from receipt", e)
            null
        }
    }
}

