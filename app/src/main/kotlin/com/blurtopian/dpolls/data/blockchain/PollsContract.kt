package com.blurtopian.dpolls.data.blockchain

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web3j wrapper for interacting with the PollsManager smart contract
 */
@Singleton
class PollsContract @Inject constructor(
    private val web3j: Web3j,
    private val contractAddress: String
) {
    
    companion object {
        // Function signatures
        private const val CREATE_POLL_FUNCTION = "createPoll"
        private const val VOTE_FUNCTION = "vote"
        private const val GET_POLL_FUNCTION = "getPoll"
        private const val GET_POLL_OPTIONS_FUNCTION = "getPollOptions"
        private const val GET_ACTIVE_POLLS_FUNCTION = "getActivePolls"
        private const val GET_POLL_RESULTS_FUNCTION = "getPollResults"
        private const val GET_VOTER_INFO_FUNCTION = "getVoterInfo"
        private const val END_POLL_FUNCTION = "endPoll"
        private const val GET_TOTAL_POLLS_COUNT_FUNCTION = "getTotalPollsCount"
        private const val IS_POLL_ENDED_FUNCTION = "isPollEnded"
        
        // Event signatures
        private const val POLL_CREATED_EVENT = "PollCreated(uint256,address,string,uint256,uint256)"
        private const val VOTE_CAST_EVENT = "VoteCast(uint256,address,uint256,uint256)"
        private const val POLL_ENDED_EVENT = "PollEnded(uint256,uint256,uint256)"
    }
    
    /**
     * Create a new poll
     */
    suspend fun createPoll(
        credentials: Credentials,
        title: String,
        description: String,
        options: List<String>,
        durationInHours: BigInteger,
        allowMultipleVotes: Boolean
    ): TransactionReceipt {
        val function = org.web3j.abi.datatypes.Function(
            CREATE_POLL_FUNCTION,
            listOf(
                Utf8String(title),
                Utf8String(description),
                DynamicArray(Utf8String::class.java, options.map { Utf8String(it) }),
                Uint256(durationInHours),
                Bool(allowMultipleVotes)
            ),
            emptyList()
        )
        
        return executeTransaction(credentials, function)
    }
    
    /**
     * Cast a vote on a poll
     */
    suspend fun vote(
        credentials: Credentials,
        pollId: BigInteger,
        optionIndex: BigInteger
    ): TransactionReceipt {
        val function = org.web3j.abi.datatypes.Function(
            VOTE_FUNCTION,
            listOf(
                Uint256(pollId),
                Uint256(optionIndex)
            ),
            emptyList()
        )
        
        return executeTransaction(credentials, function)
    }
    
    /**
     * Get poll details
     */
    suspend fun getPoll(pollId: BigInteger): PollDetails? {
        val function = org.web3j.abi.datatypes.Function(
            GET_POLL_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(
                TypeReference.create(Uint256::class.java),  // id
                TypeReference.create(Utf8String::class.java), // title
                TypeReference.create(Utf8String::class.java), // description
                TypeReference.create(Address::class.java),   // creator
                TypeReference.create(Uint256::class.java),   // startTime
                TypeReference.create(Uint256::class.java),   // endTime
                TypeReference.create(Bool::class.java),      // isActive
                TypeReference.create(Uint256::class.java),   // totalVotes
                TypeReference.create(Uint256::class.java)    // optionsCount
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            PollDetails(
                id = (result[0] as Uint256).value,
                title = (result[1] as Utf8String).value,
                description = (result[2] as Utf8String).value,
                creator = (result[3] as Address).value,
                startTime = (result[4] as Uint256).value,
                endTime = (result[5] as Uint256).value,
                isActive = (result[6] as Bool).value,
                totalVotes = (result[7] as Uint256).value,
                optionsCount = (result[8] as Uint256).value
            )
        } else null
    }
    
    /**
     * Get poll options
     */
    suspend fun getPollOptions(pollId: BigInteger): PollOptions? {
        val function = org.web3j.abi.datatypes.Function(
            GET_POLL_OPTIONS_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(
                TypeReference.create(DynamicArray::class.java, Utf8String::class.java), // texts
                TypeReference.create(DynamicArray::class.java, Uint256::class.java)     // voteCounts
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            val texts = (result[0] as DynamicArray<Utf8String>).value.map { it.value }
            val voteCounts = (result[1] as DynamicArray<Uint256>).value.map { it.value }
            
            PollOptions(
                texts = texts,
                voteCounts = voteCounts
            )
        } else null
    }
    
    /**
     * Get active polls with pagination
     */
    suspend fun getActivePolls(offset: BigInteger, limit: BigInteger): List<BigInteger> {
        val function = org.web3j.abi.datatypes.Function(
            GET_ACTIVE_POLLS_FUNCTION,
            listOf(
                Uint256(offset),
                Uint256(limit)
            ),
            listOf(
                TypeReference.create(DynamicArray::class.java, Uint256::class.java)
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            (result[0] as DynamicArray<Uint256>).value.map { it.value }
        } else emptyList()
    }
    
    /**
     * Get poll results
     */
    suspend fun getPollResults(pollId: BigInteger): PollResults? {
        val function = org.web3j.abi.datatypes.Function(
            GET_POLL_RESULTS_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(
                TypeReference.create(Utf8String::class.java), // title
                TypeReference.create(Uint256::class.java),    // totalVotes
                TypeReference.create(DynamicArray::class.java, Utf8String::class.java), // optionTexts
                TypeReference.create(DynamicArray::class.java, Uint256::class.java),    // optionVotes
                TypeReference.create(DynamicArray::class.java, Uint256::class.java)     // optionPercentages
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            val title = (result[0] as Utf8String).value
            val totalVotes = (result[1] as Uint256).value
            val optionTexts = (result[2] as DynamicArray<Utf8String>).value.map { it.value }
            val optionVotes = (result[3] as DynamicArray<Uint256>).value.map { it.value }
            val optionPercentages = (result[4] as DynamicArray<Uint256>).value.map { it.value }
            
            PollResults(
                title = title,
                totalVotes = totalVotes,
                optionTexts = optionTexts,
                optionVotes = optionVotes,
                optionPercentages = optionPercentages
            )
        } else null
    }
    
    /**
     * Get voter information
     */
    suspend fun getVoterInfo(pollId: BigInteger, voterAddress: String): VoterInfo? {
        val function = org.web3j.abi.datatypes.Function(
            GET_VOTER_INFO_FUNCTION,
            listOf(
                Uint256(pollId),
                Address(voterAddress)
            ),
            listOf(
                TypeReference.create(Bool::class.java),      // hasVoted
                TypeReference.create(Uint256::class.java),   // voteTimestamp
                TypeReference.create(Uint256::class.java),   // optionIndex
                TypeReference.create(Uint256::class.java)    // weight
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            VoterInfo(
                hasVoted = (result[0] as Bool).value,
                voteTimestamp = (result[1] as Uint256).value,
                optionIndex = (result[2] as Uint256).value,
                weight = (result[3] as Uint256).value
            )
        } else null
    }
    
    /**
     * End a poll (only creator)
     */
    suspend fun endPoll(
        credentials: Credentials,
        pollId: BigInteger
    ): TransactionReceipt {
        val function = org.web3j.abi.datatypes.Function(
            END_POLL_FUNCTION,
            listOf(Uint256(pollId)),
            emptyList()
        )
        
        return executeTransaction(credentials, function)
    }
    
    /**
     * Get total polls count
     */
    suspend fun getTotalPollsCount(): BigInteger {
        val function = org.web3j.abi.datatypes.Function(
            GET_TOTAL_POLLS_COUNT_FUNCTION,
            emptyList(),
            listOf(TypeReference.create(Uint256::class.java))
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            (result[0] as Uint256).value
        } else BigInteger.ZERO
    }
    
    /**
     * Check if poll has ended
     */
    suspend fun isPollEnded(pollId: BigInteger): Boolean {
        val function = org.web3j.abi.datatypes.Function(
            IS_POLL_ENDED_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(TypeReference.create(Bool::class.java))
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            (result[0] as Bool).value
        } else true
    }
    
    /**
     * Execute a transaction
     */
    private suspend fun executeTransaction(
        credentials: Credentials,
        function: org.web3j.abi.datatypes.Function
    ): TransactionReceipt {
        val encodedFunction = FunctionEncoder.encode(function)
        val gasPrice = web3j.ethGasPrice().send().gasPrice
        val gasLimit = DefaultGasProvider.GAS_LIMIT
        
        val transaction = Transaction.createFunctionCallTransaction(
            credentials.address,
            null,
            gasPrice,
            gasLimit,
            contractAddress,
            encodedFunction
        )
        
        val transactionResponse = web3j.ethSendTransaction(transaction).send()
        
        if (transactionResponse.hasError()) {
            throw Exception("Transaction failed: ${transactionResponse.error.message}")
        }
        
        val transactionHash = transactionResponse.transactionHash
        
        // Wait for transaction receipt
        var receipt: TransactionReceipt? = null
        var attempts = 0
        val maxAttempts = 40 // 2 minutes with 3-second intervals
        
        while (receipt == null && attempts < maxAttempts) {
            Thread.sleep(3000) // Wait 3 seconds
            val receiptResponse = web3j.ethGetTransactionReceipt(transactionHash).send()
            receipt = receiptResponse.transactionReceipt.orElse(null)
            attempts++
        }
        
        return receipt ?: throw Exception("Transaction receipt not found after waiting")
    }
    
    /**
     * Execute a call (read-only)
     */
    private suspend fun executeCall(function: org.web3j.abi.datatypes.Function): List<Type<*>> {
        val encodedFunction = FunctionEncoder.encode(function)
        
        val ethCall: EthCall = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send()
        
        if (ethCall.hasError()) {
            throw Exception("Call failed: ${ethCall.error.message}")
        }
        
        return FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
    }
}

// Data classes for contract responses
data class PollDetails(
    val id: BigInteger,
    val title: String,
    val description: String,
    val creator: String,
    val startTime: BigInteger,
    val endTime: BigInteger,
    val isActive: Boolean,
    val totalVotes: BigInteger,
    val optionsCount: BigInteger
)

data class PollOptions(
    val texts: List<String>,
    val voteCounts: List<BigInteger>
)

data class PollResults(
    val title: String,
    val totalVotes: BigInteger,
    val optionTexts: List<String>,
    val optionVotes: List<BigInteger>,
    val optionPercentages: List<BigInteger>
)

data class VoterInfo(
    val hasVoted: Boolean,
    val voteTimestamp: BigInteger,
    val optionIndex: BigInteger,
    val weight: BigInteger
)

