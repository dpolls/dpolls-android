package com.blurtopian.dpolls.data.repository

import com.blurtopian.dpolls.data.model.Poll
import javax.inject.Inject
import javax.inject.Singleton

interface PollsRepository {
    suspend fun getPolls(): List<Poll>
    suspend fun getPoll(id: String): Poll
    suspend fun createPoll(poll: Poll): String
    suspend fun vote(pollId: String, optionId: String)
}

@Singleton
class PollsRepositoryImpl @Inject constructor(
    private val web3Service: Web3Service
) : PollsRepository {
    
    override suspend fun getPolls(): List<Poll> {
        return web3Service.getPolls()
    }
    
    override suspend fun getPoll(id: String): Poll {
        return web3Service.getPoll(id)
    }
    
    override suspend fun createPoll(poll: Poll): String {
        return web3Service.createPoll(poll)
    }
    
    override suspend fun vote(pollId: String, optionId: String) {
        web3Service.vote(pollId, optionId)
    }
} 