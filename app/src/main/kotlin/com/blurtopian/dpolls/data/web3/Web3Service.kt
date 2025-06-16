package com.blurtopian.dpolls.data.web3

import androidx.lifecycle.LiveData
import com.blurtopian.dpolls.data.blockchain.PollsContract
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.domain.model.Poll
import com.blurtopian.dpolls.domain.model.PollOption
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3Service @Inject constructor(
    private val web3j: Web3j,
    private val walletManager: WalletManager,
    private val gasProvider: ContractGasProvider
) {
    private var pollsContract: PollsContract? = null
    private val contractAddress = "0x71322f1Bb13f0857410f2ebd4BC6ad731f2De6E5"

    init {
        initializeContract()
    }

    private fun initializeContract() {
        try {
            val credentials = walletManager.getCurrentCredentials() ?: return
            pollsContract = PollsContract(web3j, contractAddress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getPoll(pollId: String): Poll? {
        return try {
            val contract = pollsContract ?: return null
            val poll = contract.getPoll(BigInteger(pollId)) ?: return null
            val options = contract.getPollOptions(BigInteger(pollId)) ?: return null
            
            Poll(
                id = pollId,
                title = poll.title,
                description = poll.description,
                creator = poll.creator,
                startTime = poll.startTime.toLong(),
                endTime = poll.endTime.toLong(),
                options = options.texts.zip(options.voteCounts).mapIndexed { index, (text, voteCount) ->
                    PollOption(
                        id = index.toString(),
                        text = text,
                        voteCount = voteCount
                    )
                },
                isActive = poll.isActive,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createPoll(
        poll: Poll
    ): TransactionReceipt? {
        val title: String = poll.title
        val description: String = poll.description
        val options: List<PollOption> = poll.options;
        val durationHours: Long = poll.getDurationHours();
        return try {
            val contract = pollsContract ?: return null
            val credentials = walletManager.getCurrentCredentials() ?: return null
            
            contract.createPoll(
                credentials = credentials,
                title = title,
                description = description,
                options = options,
                durationInHours = BigInteger.valueOf(durationHours),
                allowMultipleVotes = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun vote(pollId: String, optionIndex: String): TransactionReceipt? {
        return try {
            val contract = pollsContract ?: return null
            val credentials = walletManager.getCurrentCredentials() ?: return null
            
            contract.vote(
                credentials = credentials,
                pollId = BigInteger(pollId),
                optionIndex = BigInteger.valueOf(optionIndex.toLong())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updatePoll(
        pollId: String,
        title: String,
        description: String
    ): TransactionReceipt? {
        return try {
            val contract = pollsContract ?: return null
            val credentials = walletManager.getCurrentCredentials() ?: return null
            
            contract.endPoll(
                credentials = credentials,
                pollId = BigInteger(pollId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deletePoll(pollId: String): TransactionReceipt? {
        return try {
            val contract = pollsContract ?: return null
            val credentials = walletManager.getCurrentCredentials() ?: return null
            
            contract.endPoll(
                credentials = credentials,
                pollId = BigInteger(pollId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getPolls(): List<Poll> {
        return try {
            val contract = pollsContract ?: return emptyList()
            val pollCount = contract.getTotalPollsCount().toInt()
            
            (0 until pollCount).mapNotNull { index ->
                val pollId = BigInteger.valueOf(index.toLong())
                val poll = contract.getPoll(pollId) ?: return@mapNotNull null
                val options = contract.getPollOptions(pollId) ?: return@mapNotNull null
                
                Poll(
                    id = pollId.toString(),
                    title = poll.title,
                    description = poll.description,
                    creator = poll.creator,
                    startTime = poll.startTime.toLong(),
                    endTime = poll.endTime.toLong(),
                    options = options.texts.zip(options.voteCounts).mapIndexed { optIndex, (text, voteCount) ->
                        PollOption(
                            id = optIndex.toString(),
                            text = text,
                            voteCount = voteCount
                        )
                    },
                    isActive = poll.isActive
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
} 