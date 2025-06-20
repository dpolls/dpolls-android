package com.blurtopian.dpolls.domain.repository

import android.util.Log
import com.blurtopian.dpolls.data.web3.Web3Service
import com.blurtopian.dpolls.domain.model.Poll
import javax.inject.Inject
import javax.inject.Singleton

interface PollsRepository {
    suspend fun getPolls(): List<Poll>
    suspend fun getPoll(id: String): Poll?
    suspend fun createPoll(poll: Poll): String?
    suspend fun vote(pollId: String, optionId: String)
}

@Singleton
class PollsRepositoryImpl @Inject constructor(
    private val web3Service: Web3Service
) : PollsRepository {
    
    override suspend fun getPolls(): List<Poll> {
        Log.d("PollsRepository", "Calling web3Service.getPolls()")
        return try {
            // Check connection first
            val isConnected = web3Service.checkConnection()
            if (!isConnected) {
                Log.e("PollsRepository", "Web3j connection failed")
                throw Exception("Unable to connect to blockchain network")
            }
            
            val polls = web3Service.getPolls()
            Log.d("PollsRepository", "Successfully retrieved ${polls.size} polls from Web3Service")
            polls.forEachIndexed { index, poll ->
                Log.d("PollsRepository", "Poll $index: id=${poll.id}, title=${poll.title}")
            }
            polls
        } catch (e: Exception) {
            Log.e("PollsRepository", "Error getting polls from Web3Service", e)
            throw e
        }
    }
    
    override suspend fun getPoll(id: String): Poll? {
        Log.d("PollsRepository", "Getting poll with id: $id")
        return try {
            val poll = web3Service.getPoll(id)
            Log.d("PollsRepository", "Retrieved poll: ${poll?.title}")
            poll
        } catch (e: Exception) {
            Log.e("PollsRepository", "Error getting poll $id", e)
            null
        }
    }
    
    override suspend fun createPoll(poll: Poll): String? {
        Log.d("PollsRepository", "Creating poll: ${poll.title}")
        return try {
            val result = web3Service.createPoll(poll)
            Log.d("PollsRepository", "Poll created with hash: ${result?.transactionHash}")
            result?.transactionHash
        } catch (e: Exception) {
            Log.e("PollsRepository", "Error creating poll", e)
            null
        }
    }
    
    override suspend fun vote(pollId: String, optionId: String) {
        Log.d("PollsRepository", "Voting on poll $pollId, option $optionId")
        try {
            web3Service.vote(pollId, optionId)
            Log.d("PollsRepository", "Vote submitted successfully")
        } catch (e: Exception) {
            Log.e("PollsRepository", "Error voting", e)
            throw e
        }
    }
} 