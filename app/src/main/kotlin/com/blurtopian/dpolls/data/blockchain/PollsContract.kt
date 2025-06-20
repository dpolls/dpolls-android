package com.blurtopian.dpolls.data.blockchain

import com.blurtopian.dpolls.domain.model.PollOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import android.util.Log

// Data classes for contract responses
data class NetworkInfo(
    val chainId: BigInteger,
    val blockNumber: BigInteger,
    val gasPrice: BigInteger,
    val isNeroNetwork: Boolean,
    val networkName: String
) {
    override fun toString(): String {
        return "NetworkInfo(chainId=$chainId, blockNumber=$blockNumber, gasPrice=$gasPrice, isNeroNetwork=$isNeroNetwork, networkName='$networkName')"
    }
}

data class Poll(
    val creator: String,
    val subject: String,
    val description: String,
    val category: String,
    val status: String,
    val viewType: String,
    val options: List<String>,
    val rewardPerResponse: BigInteger,
    val maxResponses: BigInteger,
    val durationDays: BigInteger,
    val minContribution: BigInteger,
    val fundingType: String,
    val targetFund: BigInteger,
    val endTime: BigInteger,
    val isOpen: Boolean,
    val totalResponses: BigInteger,
    val funds: BigInteger,
    val rewardToken: String,
    val rewardDistribution: String
)


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
        private const val GET_OPTIONS_FUNCTION = "getOptions"
        private const val GET_ALL_POLL_IDS_FUNCTION = "getAllPollIds"
        private const val GET_POLL_STATUS_FUNCTION = "getPollStatus"
        private const val GET_POLL_SUBJECT_FUNCTION = "getPollSubject"
        private const val END_POLL_FUNCTION = "closePoll"
        private const val GET_ACTIVE_POLLS_FUNCTION = "getActivePolls"
        
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
        options: List<PollOption>,
        durationInHours: BigInteger,
        allowMultipleVotes: Boolean
    ): TransactionReceipt {
        val function = org.web3j.abi.datatypes.Function(
            CREATE_POLL_FUNCTION,
            listOf(
                Utf8String(title),
                Utf8String(description),
                DynamicArray(Utf8String::class.java, options.map { Utf8String(it.text) }),
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
     * Get basic poll details without dynamic arrays
     */
    suspend fun getPollBasic(pollId: BigInteger): PollDetails? = withContext(Dispatchers.IO) {
        Log.d("PollsContract", "Calling getPollBasic($pollId)")
        try {
             val function = org.web3j.abi.datatypes.Function(
                 GET_POLL_FUNCTION,
                 listOf(Uint256(pollId)),
                 listOf(
                     TypeReference.create(Address::class.java),      // creator
                     TypeReference.create(Utf8String::class.java),   // subject
                     TypeReference.create(Utf8String::class.java),   // description
                     TypeReference.create(Utf8String::class.java),   // category
                     TypeReference.create(Utf8String::class.java),   // status
                     TypeReference.create(Utf8String::class.java),   // viewType
                     TypeReference.create(Uint256::class.java),      // rewardPerResponse
                     TypeReference.create(Uint256::class.java),      // maxResponses
                     TypeReference.create(Uint256::class.java),      // durationDays
                     TypeReference.create(Uint256::class.java),      // minContribution
                     TypeReference.create(Utf8String::class.java),   // fundingType
                     TypeReference.create(Uint256::class.java),      // targetFund
                     TypeReference.create(Uint256::class.java),      // endTime
                     TypeReference.create(Bool::class.java),         // isOpen
                     TypeReference.create(Uint256::class.java),      // totalResponses
                     TypeReference.create(Uint256::class.java),      // funds
                     TypeReference.create(Address::class.java),      // rewardToken
                     TypeReference.create(Utf8String::class.java)    // rewardDistribution
                 )
             )

            val result = executeCall(function)
            return@withContext if (result.isNotEmpty()) {
                val pollDetails = PollDetails(
                    id = pollId,
                    title = (result[1] as Utf8String).value,        // subject
                    description = (result[2] as Utf8String).value,  // description
                    creator = (result[0] as Address).value,         // creator
                    startTime = BigInteger.ZERO,                    // Not available in PollView
                    endTime = (result[13] as Uint256).value,        // endTime
                    isActive = (result[14] as Bool).value,          // isOpen
                    totalVotes = (result[15] as Uint256).value,     // totalResponses
                    optionsCount = BigInteger.ZERO                  // Will get from separate call
                )
                Log.d("PollsContract", "Retrieved basic poll $pollId: ${pollDetails.title}")
                pollDetails
            } else {
                Log.w("PollsContract", "Basic poll $pollId returned null")
                null
            }
        } catch (e: Exception) {
            Log.e("PollsContract", "Error getting basic poll $pollId", e)
            return@withContext null
        }
    }
    
    /**
     * Get poll details
     */
    suspend fun getPoll(pollId: BigInteger): PollDetails? = withContext(Dispatchers.IO) {
        Log.d("PollsContract", "Calling getPoll($pollId) with manual decoding")
        try {
            // The function signature is used to encode the call, but we decode the response manually.
            // Output parameters can be empty as they are not used for encoding.
            val function = org.web3j.abi.datatypes.Function(
                GET_POLL_FUNCTION,
                listOf(Uint256(pollId)),
                emptyList()
            )

            val poll = executeCallForGetPoll(function)

            return@withContext poll?.let {
                PollDetails(
                    id = pollId,
                    title = it.subject,
                    description = it.description,
                    creator = it.creator,
                    startTime = BigInteger.ZERO, // Not available in PollView
                    endTime = it.endTime,
                    isActive = it.isOpen,
                    totalVotes = it.totalResponses,
                    optionsCount = it.options.size.toBigInteger()
                )
            }
        } catch (e: Exception) {
            Log.e("PollsContract", "Error getting poll $pollId with manual decoding", e)
            return@withContext null
        }
    }
    
    /**
     * Get poll options
     */
    suspend fun getPollOptions(pollId: BigInteger): PollOptions? {
        val function = org.web3j.abi.datatypes.Function(
            GET_OPTIONS_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(
                object : TypeReference<DynamicArray<Utf8String>>() {}
            )
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            val texts = (result[0] as DynamicArray<Utf8String>).value.map { it.value }
            // Since the contract doesn't return vote counts, we'll use zeros for now
            val voteCounts = List(texts.size) { BigInteger.ZERO }
            
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
                object : TypeReference<DynamicArray<Uint256>>() {}
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
            GET_POLL_STATUS_FUNCTION,
            listOf(Uint256(pollId)),
            listOf(
                TypeReference.create(Utf8String::class.java),
                TypeReference.create(Uint256::class.java),
                object : TypeReference<DynamicArray<Utf8String>>() {},
                object : TypeReference<DynamicArray<Uint256>>() {},
                object : TypeReference<DynamicArray<Uint256>>() {}
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
            GET_POLL_SUBJECT_FUNCTION,
            listOf(
                Uint256(pollId),
                Address(voterAddress)
            ),
            listOf(
                TypeReference.create(Bool::class.java),
                TypeReference.create(Uint256::class.java),
                TypeReference.create(Uint256::class.java),
                TypeReference.create(Uint256::class.java)
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
     * Get all poll IDs
     */
    suspend fun getAllPollIds(): List<BigInteger> = withContext(Dispatchers.IO) {
        Log.d("PollsContract", "Calling getAllPollIds()")
        val function = org.web3j.abi.datatypes.Function(
            GET_ALL_POLL_IDS_FUNCTION,
            emptyList(),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )
        
        val result = executeCall(function)
        return@withContext if (result.isNotEmpty()) {
            val pollIds = (result[0] as DynamicArray<Uint256>).value.map { it.value }
            Log.d("PollsContract", "Retrieved ${pollIds.size} poll IDs: $pollIds")
            pollIds
        } else {
            Log.d("PollsContract", "No poll IDs found")
            emptyList()
        }
    }
    
    /**
     * Get total polls count
     */
    suspend fun getTotalPollsCount(): BigInteger {
        val function = org.web3j.abi.datatypes.Function(
            GET_ALL_POLL_IDS_FUNCTION,
            emptyList(),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )
        
        val result = executeCall(function)
        return if (result.isNotEmpty()) {
            val pollIds = (result[0] as DynamicArray<Uint256>).value
            BigInteger.valueOf(pollIds.size.toLong())
        } else BigInteger.ZERO
    }
    
    /**
     * Check if poll has ended
     */
    suspend fun isPollEnded(pollId: BigInteger): Boolean {
        val function = org.web3j.abi.datatypes.Function(
            GET_POLL_STATUS_FUNCTION,
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
    ): TransactionReceipt = withContext(Dispatchers.IO) {
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
        
        return@withContext receipt ?: throw Exception("Transaction receipt not found after waiting")
    }
    
    /**
     * Execute a call (read-only)
     */
    private suspend fun executeCall(function: org.web3j.abi.datatypes.Function): List<Type<*>> = withContext(Dispatchers.IO) {
        val encodedFunction = FunctionEncoder.encode(function)
        Log.d("PollsContract", "Calling function: ${function.name}")
        Log.d("PollsContract", "Encoded function: $encodedFunction")
        Log.d("PollsContract", "Contract address: $contractAddress")
        Log.d("PollsContract", "Function input parameters: ${function.inputParameters}")
        Log.d("PollsContract", "Function output parameters: ${function.outputParameters}")
        
        val ethCall: EthCall = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send()
        
        if (ethCall.hasError()) {
            Log.e("PollsContract", "Call failed: ${ethCall.error.message}")
            throw Exception("Call failed: ${ethCall.error.message}")
        }
        
        Log.d("PollsContract", "Raw response: ${ethCall.value}")
        Log.d("PollsContract", "Response length: ${ethCall.value?.length ?: 0}")
        
        // Check if response is empty or null
        if (ethCall.value.isNullOrEmpty()) {
            Log.w("PollsContract", "Empty response received")
            return@withContext emptyList()
        }
        
        // Check if response starts with 0x
        if (!ethCall.value.startsWith("0x")) {
            Log.e("PollsContract", "Invalid response format: ${ethCall.value}")
            throw Exception("Invalid response format: ${ethCall.value}")
        }
        
        // Check for common error responses
        if (ethCall.value == "0x" || ethCall.value == "0x0") {
            Log.w("PollsContract", "Contract returned empty response - function may not exist")
            return@withContext emptyList()
        }
        
        // Check if response is too short to be valid
        if (ethCall.value.length < 10) {
            Log.e("PollsContract", "Response too short to be valid: ${ethCall.value}")
            throw Exception("Response too short to be valid: ${ethCall.value}")
        }
        
        // Log the first 100 characters of the response for debugging
        val responsePreview = if (ethCall.value.length > 100) {
            "${ethCall.value.substring(0, 100)}..."
        } else {
            ethCall.value
        }
        Log.d("PollsContract", "Response preview: $responsePreview")
        
        try {
            val decodedResult = FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
            Log.d("PollsContract", "Decoded result size: ${decodedResult.size}")
            return@withContext decodedResult
        } catch (e: Exception) {
            Log.e("PollsContract", "Failed to decode response: ${ethCall.value}", e)
            Log.e("PollsContract", "Response length: ${ethCall.value.length}")
            Log.e("PollsContract", "Function: ${function.name}")
            Log.e("PollsContract", "Contract address: $contractAddress")
            throw e
        }
    }

    /**
     * Test getPoll function with minimal decoding
     */
    suspend fun testGetPollMinimal(pollId: BigInteger): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing getPoll minimal decoding for poll $pollId")
            
            // Try with just the first few parameters
            val function = org.web3j.abi.datatypes.Function(
                "getPoll",
                listOf(Uint256(pollId)),
                listOf(
                    TypeReference.create(Address::class.java),      // creator
                    TypeReference.create(Utf8String::class.java),   // subject
                    TypeReference.create(Utf8String::class.java)    // description
                )
            )
            
            val result = executeCall(function)
            Log.d("PollsContract", "Minimal getPoll test successful, result size: ${result.size}")
            
            if (result.isNotEmpty()) {
                val creator = (result[0] as Address).value
                val subject = (result[1] as Utf8String).value
                val description = (result[2] as Utf8String).value
                Log.d("PollsContract", "Poll $pollId: creator=$creator, subject='$subject', description='$description'")
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e("PollsContract", "Minimal getPoll test failed", e)
            return@withContext false
        }
    }

    /**
     * Test getPoll function signature
     */
    suspend fun testGetPollSignature(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing getPoll function signature...")
            
            // Try to call getPoll with a non-existent poll ID (0) to see if the function exists
            val function = org.web3j.abi.datatypes.Function(
                "getPoll",
                listOf(Uint256(BigInteger.ZERO)),
                listOf(
                    TypeReference.create(Address::class.java),      // creator
                    TypeReference.create(Utf8String::class.java),   // subject
                    TypeReference.create(Utf8String::class.java),   // description
                    TypeReference.create(Utf8String::class.java),   // category
                    TypeReference.create(Utf8String::class.java),   // status
                    TypeReference.create(Utf8String::class.java),   // viewType
                    object : TypeReference<DynamicArray<Utf8String>>() {}, // options
                    TypeReference.create(Uint256::class.java),      // rewardPerResponse
                    TypeReference.create(Uint256::class.java),      // maxResponses
                    TypeReference.create(Uint256::class.java),      // durationDays
                    TypeReference.create(Uint256::class.java),      // minContribution
                    TypeReference.create(Utf8String::class.java),   // fundingType
                    TypeReference.create(Uint256::class.java),      // targetFund
                    TypeReference.create(Uint256::class.java),      // endTime
                    TypeReference.create(Bool::class.java),         // isOpen
                    TypeReference.create(Uint256::class.java),      // totalResponses
                    TypeReference.create(Uint256::class.java),      // funds
                    TypeReference.create(Address::class.java),      // rewardToken
                    TypeReference.create(Utf8String::class.java)    // rewardDistribution
                )
            )
            
            val result = executeCall(function)
            Log.d("PollsContract", "getPoll function signature test successful, result size: ${result.size}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("PollsContract", "getPoll function signature test failed", e)
            return@withContext false
        }
    }

    /**
     * Test if contract exists and has the expected function
     */
    suspend fun testContractExists(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing if contract exists at address: $contractAddress")
            
            // Try to get the contract code
            val codeResponse = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send()
            if (codeResponse.hasError()) {
                Log.e("PollsContract", "Failed to get contract code: ${codeResponse.error.message}")
                return@withContext false
            }
            
            val contractCode = codeResponse.code
            Log.d("PollsContract", "Contract code length: ${contractCode.length}")
            
            if (contractCode == "0x" || contractCode == "0x0") {
                Log.e("PollsContract", "No contract found at address: $contractAddress")
                return@withContext false
            }
            
            Log.d("PollsContract", "Contract exists at address: $contractAddress")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error testing contract existence", e)
            return@withContext false
        }
    }

    /**
     * Test contract connection with a simple call
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing contract connection...")
            val function = org.web3j.abi.datatypes.Function(
                "getAllPollIds",
                emptyList(),
                listOf(object : TypeReference<DynamicArray<Uint256>>() {})
            )
            
            val result = executeCall(function)
            Log.d("PollsContract", "Connection test successful, result size: ${result.size}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("PollsContract", "Connection test failed", e)
            return@withContext false
        }
    }

    /**
     * Analyze contract bytecode to find available function selectors
     */
    suspend fun analyzeContractFunctions(): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Analyzing contract functions...")
            
            // Get the contract bytecode
            val codeResponse = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send()
            if (codeResponse.hasError()) {
                Log.e("PollsContract", "Failed to get contract code: ${codeResponse.error.message}")
                return@withContext emptyList()
            }
            
            val contractCode = codeResponse.code
            Log.d("PollsContract", "Contract code length: ${contractCode.length}")
            
            if (contractCode == "0x" || contractCode == "0x0") {
                Log.e("PollsContract", "No contract found at address: $contractAddress")
                return@withContext emptyList()
            }
            
            // Common function selectors to check - calculated from actual Solidity contract
            val functionSelectors = mapOf(
                "getPoll(uint256)" to "1a8cbcaa",
                "getAllPollIds()" to "ee0ca517",
                "getOptions(uint256)" to "8915b3fb",
                "getPollStatus(uint256)" to "b0c095e3",
                "getPollResponses(uint256)" to "606af7a5",
                "getUserPolls(address)" to "07f790e7",
                "getUserActivePolls(address)" to "09408ec5",
                "getActivePolls()" to "63a9492f",
                "submitResponse(uint256,string)" to "a7dbf936",
                "closePoll(uint256)" to "9534e637",
                "cancelPoll(uint256)" to "d0ec1607",
                "openPoll(uint256)" to "f23ec81d",
                "forClaiming(uint256)" to "900b79e7",
                "forFunding(uint256)" to "77e98ebe",
                "updateTargetFund(uint256,uint256)" to "e5d6ad47",
                "fundPoll(uint256)" to "d6abd2f4",
                "fundPollWithToken(uint256,uint256)" to "fd8dbf3c",
                "claimReward(uint256)" to "ae169a50",
                "donateReward(uint256)" to "578cc8e0",
                "donateRemainingFunds(uint256)" to "ec455663",
                "claimRemainingFunds(uint256)" to "8654f5f5",
                "getCommunityFundBalance(address)" to "e43b3373",
                "getDonorTotalByToken(address,address)" to "79c72464",
                "getDonorHistory(address)" to "3e4cbf42"
            )
            
            val foundFunctions = mutableListOf<String>()
            
            // Check each function selector
            functionSelectors.forEach { (functionName, selector) ->
                if (contractCode.contains(selector)) {
                    foundFunctions.add(functionName)
                    Log.d("PollsContract", "✅ Found function: $functionName (selector: $selector)")
                } else {
                    Log.d("PollsContract", "❌ Missing function: $functionName (selector: $selector)")
                }
            }
            
            // Also search for common patterns in the bytecode
            val commonPatterns = listOf(
                "getPoll", "getAll", "create", "vote", "close", "submit", "fund", "claim"
            )
            
            commonPatterns.forEach { pattern ->
                if (contractCode.contains(pattern, ignoreCase = true)) {
                    Log.d("PollsContract", "🔍 Found pattern in bytecode: $pattern")
                }
            }
            
            Log.d("PollsContract", "Contract analysis complete. Found ${foundFunctions.size} functions: $foundFunctions")
            return@withContext foundFunctions
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error analyzing contract functions", e)
            return@withContext emptyList()
        }
    }

    /**
     * Test if contract has the expected function selectors
     */
    suspend fun testContractBytecode(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing contract bytecode...")
            
            // Get the contract bytecode
            val codeResponse = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send()
            if (codeResponse.hasError()) {
                Log.e("PollsContract", "Failed to get contract code: ${codeResponse.error.message}")
                return@withContext false
            }
            
            val contractCode = codeResponse.code
            Log.d("PollsContract", "Contract code length: ${contractCode.length}")
            
            if (contractCode == "0x" || contractCode == "0x0") {
                Log.e("PollsContract", "No contract found at address: $contractAddress")
                return@withContext false
            }
            
            // Check for common function selectors in the bytecode
            val getPollSelector = "1a8cbcaa" // keccak256("getPoll(uint256)")[:8]
            val getAllPollIdsSelector = "ee0ca517" // keccak256("getAllPollIds()")[:8]
            
            val hasGetPoll = contractCode.contains(getPollSelector)
            val hasGetAllPollIds = contractCode.contains(getAllPollIdsSelector)
            
            Log.d("PollsContract", "Contract contains getPoll selector: $hasGetPoll")
            Log.d("PollsContract", "Contract contains getAllPollIds selector: $hasGetAllPollIds")
            
            if (!hasGetPoll) {
                Log.e("PollsContract", "Contract does not contain getPoll function selector")
                return@withContext false
            }
            
            Log.d("PollsContract", "Contract bytecode test successful")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error testing contract bytecode", e)
            return@withContext false
        }
    }

    /**
     * Test if connected to the correct NERO chain
     */
    suspend fun testNetworkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Testing network connection...")
            
            // Get chain ID
            val chainIdResponse = web3j.ethChainId().send()
            if (chainIdResponse.hasError()) {
                Log.e("PollsContract", "Failed to get chain ID: ${chainIdResponse.error.message}")
                return@withContext false
            }
            
            val chainId = chainIdResponse.chainId
            Log.d("PollsContract", "Connected to chain ID: $chainId")
            
            // NERO chain IDs:
            // - Mainnet: 1689
            // - Testnet: 689
            val expectedChainIds = listOf(BigInteger.valueOf(1689L), BigInteger.valueOf(689L))
            
            if (chainId in expectedChainIds) {
                val networkName = if (chainId == BigInteger.valueOf(1689L)) "NERO Mainnet" else "NERO Testnet"
                Log.d("PollsContract", "✅ Connected to $networkName (Chain ID: $chainId)")
                return@withContext true
            } else {
                Log.e("PollsContract", "❌ Wrong network! Expected NERO chain (689 or 1689), got: $chainId")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error testing network connection", e)
            return@withContext false
        }
    }

    /**
     * Get current network information
     */
    suspend fun getNetworkInfo(): NetworkInfo = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Getting network information...")
            
            // Get chain ID
            val chainIdResponse = web3j.ethChainId().send()
            val chainId = if (chainIdResponse.hasError()) {
                Log.e("PollsContract", "Failed to get chain ID: ${chainIdResponse.error.message}")
                BigInteger.ZERO
            } else {
                chainIdResponse.chainId
            }
            
            // Get latest block number
            val blockNumberResponse = web3j.ethBlockNumber().send()
            val blockNumber = if (blockNumberResponse.hasError()) {
                Log.e("PollsContract", "Failed to get block number: ${blockNumberResponse.error.message}")
                BigInteger.ZERO
            } else {
                blockNumberResponse.blockNumber
            }
            
            // Get gas price
            val gasPriceResponse = web3j.ethGasPrice().send()
            val gasPrice = if (gasPriceResponse.hasError()) {
                Log.e("PollsContract", "Failed to get gas price: ${gasPriceResponse.error.message}")
                BigInteger.ZERO
            } else {
                gasPriceResponse.gasPrice
            }
            
            val networkInfo = NetworkInfo(
                chainId = chainId,
                blockNumber = blockNumber,
                gasPrice = gasPrice,
                isNeroNetwork = chainId in listOf(BigInteger.valueOf(1689L), BigInteger.valueOf(689L)),
                networkName = when (chainId) {
                    BigInteger.valueOf(1689L) -> "NERO Mainnet"
                    BigInteger.valueOf(689L) -> "NERO Testnet"
                    else -> "Unknown Network"
                }
            )
            
            Log.d("PollsContract", "Network Info: $networkInfo")
            return@withContext networkInfo
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error getting network information", e)
            return@withContext NetworkInfo(
                chainId = BigInteger.ZERO,
                blockNumber = BigInteger.ZERO,
                gasPrice = BigInteger.ZERO,
                isNeroNetwork = false,
                networkName = "Error"
            )
        }
    }

    private suspend fun executeCallForGetPoll(function: org.web3j.abi.datatypes.Function): Poll? = withContext(Dispatchers.IO) {
        val encodedFunction = FunctionEncoder.encode(function)
        Log.d("PollsContract", "Calling function for getPoll: ${function.name}")
        Log.d("PollsContract", "Encoded function for getPoll: $encodedFunction")

        val ethCall: EthCall = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send()

        if (ethCall.hasError()) {
            val error = "eth_call for getPoll failed: ${ethCall.error.message}"
            Log.e("PollsContract", error)
            throw Exception(error)
        }

        val rawResponse = ethCall.value
        Log.d("PollsContract", "Raw response for getPoll: $rawResponse")

        if (rawResponse.isNullOrEmpty() || rawResponse == "0x" || rawResponse == "0x0" || rawResponse.length < 10) {
            Log.w("PollsContract", "Contract returned empty or invalid response for getPoll")
            return@withContext null
        }

        return@withContext decodeRaw(rawResponse)
    }

    private fun decodeRaw(rawResponse: String): Poll? {
        try {
            Log.d("PollsContract", "Decoding PollView struct...")
            val hexData = if (rawResponse.startsWith("0x")) rawResponse.substring(2) else rawResponse

            var offset = 0

            // Helper to read next 32 bytes (64 hex chars) and advance offset
            fun readNext32Bytes(): String {
                val data = hexData.substring(offset, offset + 64)
                offset += 64
                return data
            }

            val creator = "0x" + readNext32Bytes().substring(24)
            val subjectOffset = readNext32Bytes().toBigInteger(16)
            val descriptionOffset = readNext32Bytes().toBigInteger(16)
            val categoryOffset = readNext32Bytes().toBigInteger(16)
            val statusOffset = readNext32Bytes().toBigInteger(16)
            val viewTypeOffset = readNext32Bytes().toBigInteger(16)
            val optionsOffset = readNext32Bytes().toBigInteger(16)
            val rewardPerResponse = readNext32Bytes().toBigInteger(16)
            val maxResponses = readNext32Bytes().toBigInteger(16)
            val durationDays = readNext32Bytes().toBigInteger(16)
            val minContribution = readNext32Bytes().toBigInteger(16)
            val fundingTypeOffset = readNext32Bytes().toBigInteger(16)
            val targetFund = readNext32Bytes().toBigInteger(16)
            val endTime = readNext32Bytes().toBigInteger(16)
            val isOpen = readNext32Bytes().toBigInteger(16) != BigInteger.ZERO
            val totalResponses = readNext32Bytes().toBigInteger(16)
            val funds = readNext32Bytes().toBigInteger(16)
            val rewardToken = "0x" + readNext32Bytes().substring(24)
            val rewardDistributionOffset = readNext32Bytes().toBigInteger(16)

            val subject = readStringAtOffset(hexData, subjectOffset)
            val description = readStringAtOffset(hexData, descriptionOffset)
            val category = readStringAtOffset(hexData, categoryOffset)
            val status = readStringAtOffset(hexData, statusOffset)
            val viewType = readStringAtOffset(hexData, viewTypeOffset)
            val fundingType = readStringAtOffset(hexData, fundingTypeOffset)
            val rewardDistribution = readStringAtOffset(hexData, rewardDistributionOffset)
            val options = readStringArrayAtOffset(hexData, optionsOffset)
            
            return Poll(
                creator, subject, description, category, status, viewType, options,
                rewardPerResponse, maxResponses, durationDays, minContribution,
                fundingType, targetFund, endTime, isOpen, totalResponses, funds,
                rewardToken, rewardDistribution
            )
        } catch (e: Exception) {
            Log.e("PollsContract", "Error decoding PollView struct", e)
            return null
        }
    }

    private fun readStringAtOffset(hexData: String, offset: BigInteger): String {
        try {
            val dataOffset = offset.toInt() * 2
            val length = hexData.substring(dataOffset, dataOffset + 64).toBigInteger(16).toInt()
            if (length == 0) return ""
            val stringData = hexData.substring(dataOffset + 64, dataOffset + 64 + (length * 2))
            return String(hexStringToByteArray(stringData))
        } catch (e: Exception) {
            Log.e("PollsContract", "Error reading string at offset $offset", e)
            return ""
        }
    }

    private fun readStringArrayAtOffset(hexData: String, arrayOffset: BigInteger): List<String> {
        try {
            val arrayDataPos = arrayOffset.toInt() * 2
            val length = hexData.substring(arrayDataPos, arrayDataPos + 64).toBigInteger(16).toInt()
            if (length == 0) return emptyList()
            
            val strings = mutableListOf<String>()
            for (i in 0 until length) {
                val stringOffsetPos = arrayDataPos + 64 + (i * 64)
                val stringOffset = hexData.substring(stringOffsetPos, stringOffsetPos + 64).toBigInteger(16)
                val absoluteStringOffset = arrayOffset + stringOffset
                strings.add(readStringAtOffset(hexData, absoluteStringOffset))
            }
            return strings
        } catch (e: Exception) {
            Log.e("PollsContract", "Error reading string array at offset $arrayOffset", e)
            return emptyList()
        }
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
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
