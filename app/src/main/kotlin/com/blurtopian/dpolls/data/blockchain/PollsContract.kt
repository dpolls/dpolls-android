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

        return@withContext decodeRawImproved(rawResponse)
    }

    /**
     * Improved manual decoding with better error handling
     */
    private fun decodeRawImproved(rawResponse: String): Poll? {
        try {
            Log.d("PollsContract", "Starting improved manual decoding...")
            Log.d("PollsContract", "Raw response length: ${rawResponse.length}")
            
            val hexData = if (rawResponse.startsWith("0x")) rawResponse.substring(2) else rawResponse
            
            // Validate minimum length for a complete PollView struct
            val minExpectedLength = 20 * 64 // 20 fields * 32 bytes each
            if (hexData.length < minExpectedLength) {
                Log.e("PollsContract", "Response too short. Expected at least $minExpectedLength chars, got ${hexData.length}")
                return null
            }
            
            var offset = 0
            
            // Helper to read next 32 bytes with bounds checking
            fun readNext32Bytes(): String {
                if (offset + 64 > hexData.length) {
                    throw Exception("Attempted to read beyond response bounds at offset $offset")
                }
                val data = hexData.substring(offset, offset + 64)
                offset += 64
                return data
            }
            
            // Helper to read address (20 bytes, padded to 32)
            fun readAddress(): String {
                val data = readNext32Bytes()
                return "0x" + data.substring(24) // Remove padding
            }
            
            // Helper to read uint256
            fun readUint256(): BigInteger {
                return readNext32Bytes().toBigInteger(16)
            }
            
            // Helper to read bool
            fun readBool(): Boolean {
                return readUint256() != BigInteger.ZERO
            }
            
            // Read all static fields first
            val creator = readAddress()
            val subjectOffset = readUint256()
            val descriptionOffset = readUint256()
            val categoryOffset = readUint256()
            val statusOffset = readUint256()
            val viewTypeOffset = readUint256()
            val optionsOffset = readUint256()
            val rewardPerResponse = readUint256()
            val maxResponses = readUint256()
            val durationDays = readUint256()
            val minContribution = readUint256()
            val fundingTypeOffset = readUint256()
            val targetFund = readUint256()
            val endTime = readUint256()
            val isOpen = readBool()
            val totalResponses = readUint256()
            val funds = readUint256()
            val rewardToken = readAddress()
            val rewardDistributionOffset = readUint256()
            
            Log.d("PollsContract", "Static fields decoded successfully")
            Log.d("PollsContract", "Creator: $creator")
            Log.d("PollsContract", "Subject offset: $subjectOffset")
            Log.d("PollsContract", "Options offset: $optionsOffset")
            
            // Read dynamic strings
            val subject = readStringAtOffsetImproved(hexData, subjectOffset, "subject")
            val description = readStringAtOffsetImproved(hexData, descriptionOffset, "description")
            val category = readStringAtOffsetImproved(hexData, categoryOffset, "category")
            val status = readStringAtOffsetImproved(hexData, statusOffset, "status")
            val viewType = readStringAtOffsetImproved(hexData, viewTypeOffset, "viewType")
            val fundingType = readStringAtOffsetImproved(hexData, fundingTypeOffset, "fundingType")
            val rewardDistribution = readStringAtOffsetImproved(hexData, rewardDistributionOffset, "rewardDistribution")
            val options = readStringArrayAtOffsetImproved(hexData, optionsOffset)
            
            Log.d("PollsContract", "Dynamic fields decoded successfully")
            Log.d("PollsContract", "Subject: '$subject'")
            Log.d("PollsContract", "Options count: ${options.size}")
            
            return Poll(
                creator = creator,
                subject = subject,
                description = description,
                category = category,
                status = status,
                viewType = viewType,
                options = options,
                rewardPerResponse = rewardPerResponse,
                maxResponses = maxResponses,
                durationDays = durationDays,
                minContribution = minContribution,
                fundingType = fundingType,
                targetFund = targetFund,
                endTime = endTime,
                isOpen = isOpen,
                totalResponses = totalResponses,
                funds = funds,
                rewardToken = rewardToken,
                rewardDistribution = rewardDistribution
            )
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error in improved manual decoding", e)
            Log.e("PollsContract", "Raw response: $rawResponse")
            return null
        }
    }
    
    private fun readStringAtOffsetImproved(hexData: String, offset: BigInteger, fieldName: String): String {
        try {
            val dataOffset = offset.toInt() * 2
            if (dataOffset >= hexData.length) {
                Log.w("PollsContract", "String offset $offset for $fieldName is beyond data bounds")
                return ""
            }
            
            if (dataOffset + 64 > hexData.length) {
                Log.w("PollsContract", "Cannot read length for $fieldName at offset $offset")
                return ""
            }
            
            val length = hexData.substring(dataOffset, dataOffset + 64).toBigInteger(16).toInt()
            if (length == 0) return ""
            
            val stringStart = dataOffset + 64
            val stringEnd = stringStart + (length * 2)
            
            if (stringEnd > hexData.length) {
                Log.w("PollsContract", "String data for $fieldName extends beyond response bounds")
                return ""
            }
            
            val stringData = hexData.substring(stringStart, stringEnd)
            val result = String(hexStringToByteArray(stringData))
            Log.d("PollsContract", "Decoded $fieldName: '$result'")
            return result
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error reading string for $fieldName at offset $offset", e)
            return ""
        }
    }
    
    private fun readStringArrayAtOffsetImproved(hexData: String, arrayOffset: BigInteger): List<String> {
        try {
            val arrayDataPos = arrayOffset.toInt() * 2
            if (arrayDataPos >= hexData.length) {
                Log.w("PollsContract", "Array offset $arrayOffset is beyond data bounds")
                return emptyList()
            }
            
            if (arrayDataPos + 64 > hexData.length) {
                Log.w("PollsContract", "Cannot read array length at offset $arrayOffset")
                return emptyList()
            }
            
            val length = hexData.substring(arrayDataPos, arrayDataPos + 64).toBigInteger(16).toInt()
            if (length == 0) return emptyList()
            
            Log.d("PollsContract", "Reading string array with $length elements")
            
            val strings = mutableListOf<String>()
            for (i in 0 until length) {
                val stringOffsetPos = arrayDataPos + 64 + (i * 64)
                if (stringOffsetPos + 64 > hexData.length) {
                    Log.w("PollsContract", "Cannot read string offset $i in array")
                    break
                }
                
                val stringOffset = hexData.substring(stringOffsetPos, stringOffsetPos + 64).toBigInteger(16)
                val absoluteStringOffset = arrayOffset + stringOffset
                val string = readStringAtOffsetImproved(hexData, absoluteStringOffset, "option[$i]")
                strings.add(string)
            }
            
            Log.d("PollsContract", "Successfully decoded ${strings.size} options")
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

    /**
     * Alternative: Raw call with custom decoder for complex structs
     */
    suspend fun getPollWithRawCall(pollId: BigInteger): Poll? = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Calling getPoll with raw call for poll $pollId")
            
            // Create function with minimal output parameters to avoid Web3j decoding issues
            val function = org.web3j.abi.datatypes.Function(
                GET_POLL_FUNCTION,
                listOf(Uint256(pollId)),
                emptyList() // Let us handle decoding manually
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            Log.d("PollsContract", "Raw call encoded function: $encodedFunction")
            
            val ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()
            
            if (ethCall.hasError()) {
                Log.e("PollsContract", "Raw call failed: ${ethCall.error.message}")
                return@withContext null
            }
            
            val rawResponse = ethCall.value
            Log.d("PollsContract", "Raw call response: $rawResponse")
            
            if (rawResponse.isNullOrEmpty() || rawResponse == "0x" || rawResponse == "0x0") {
                Log.w("PollsContract", "Raw call returned empty response")
                return@withContext null
            }
            
            // Use the improved manual decoder
            return@withContext decodeRawImproved(rawResponse)
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error in raw call getPoll", e)
            return@withContext null
        }
    }

    /**
     * Alternative: Direct HTTP RPC call to bypass Web3j decoding
     */
    suspend fun getPollWithDirectRPC(pollId: BigInteger): Poll? = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Calling getPoll with direct RPC for poll $pollId")
            
            // Create the RPC request manually
            val functionSelector = "1a8cbcaa" // keccak256("getPoll(uint256)")[:8]
            val pollIdHex = pollId.toString(16).padStart(64, '0')
            val data = "0x$functionSelector$pollIdHex"
            
            val rpcRequest = """
                {
                    "jsonrpc": "2.0",
                    "method": "eth_call",
                    "params": [
                        {
                            "to": "$contractAddress",
                            "data": "$data"
                        },
                        "latest"
                    ],
                    "id": 1
                }
            """.trimIndent()
            
            Log.d("PollsContract", "RPC request: $rpcRequest")
            
            // You would need to implement HTTP client here
            // For now, we'll use Web3j's underlying HTTP client
            val ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, data),
                DefaultBlockParameterName.LATEST
            ).send()
            
            if (ethCall.hasError()) {
                Log.e("PollsContract", "Direct RPC call failed: ${ethCall.error.message}")
                return@withContext null
            }
            
            val rawResponse = ethCall.value
            Log.d("PollsContract", "Direct RPC response: $rawResponse")
            
            if (rawResponse.isNullOrEmpty() || rawResponse == "0x" || rawResponse == "0x0") {
                Log.w("PollsContract", "Direct RPC returned empty response")
                return@withContext null
            }
            
            // Use the improved manual decoder
            return@withContext decodeRawImproved(rawResponse)
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error in direct RPC getPoll", e)
            return@withContext null
        }
    }

    /**
     * Alternative: Web3j contract wrapper with exact struct matching
     */
    suspend fun getPollWithExactStruct(pollId: BigInteger): Poll? = withContext(Dispatchers.IO) {
        try {
            Log.d("PollsContract", "Calling getPoll with exact struct matching for poll $pollId")
            
            // Create function with exact output parameters matching the Solidity struct
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
            
            return@withContext if (result.size >= 20) {
                val poll = Poll(
                    creator = (result[0] as Address).value,
                    subject = (result[1] as Utf8String).value,
                    description = (result[2] as Utf8String).value,
                    category = (result[3] as Utf8String).value,
                    status = (result[4] as Utf8String).value,
                    viewType = (result[5] as Utf8String).value,
                    options = (result[6] as DynamicArray<Utf8String>).value.map { it.value },
                    rewardPerResponse = (result[7] as Uint256).value,
                    maxResponses = (result[8] as Uint256).value,
                    durationDays = (result[9] as Uint256).value,
                    minContribution = (result[10] as Uint256).value,
                    fundingType = (result[11] as Utf8String).value,
                    targetFund = (result[12] as Uint256).value,
                    endTime = (result[13] as Uint256).value,
                    isOpen = (result[14] as Bool).value,
                    totalResponses = (result[15] as Uint256).value,
                    funds = (result[16] as Uint256).value,
                    rewardToken = (result[17] as Address).value,
                    rewardDistribution = (result[18] as Utf8String).value
                )
                
                Log.d("PollsContract", "Successfully decoded poll with exact struct: ${poll.subject}")
                poll
            } else {
                Log.w("PollsContract", "Exact struct decoding returned insufficient results: ${result.size}")
                null
            }
            
        } catch (e: Exception) {
            Log.e("PollsContract", "Error in exact struct getPoll", e)
            return@withContext null
        }
    }

    /**
     * Comprehensive fallback strategy that tries multiple decoding approaches
     */
    suspend fun getPollWithFallback(pollId: BigInteger): Poll? = withContext(Dispatchers.IO) {
        Log.d("PollsContract", "Starting fallback strategy for poll $pollId")
        
        // Strategy 1: Try exact struct matching first
        try {
            Log.d("PollsContract", "Fallback Strategy 1: Exact struct matching")
            val result = getPollWithExactStruct(pollId)
            if (result != null) {
                Log.d("PollsContract", "✅ Strategy 1 succeeded")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w("PollsContract", "Strategy 1 failed", e)
        }
        
        // Strategy 2: Try raw call with manual decoding
        try {
            Log.d("PollsContract", "Fallback Strategy 2: Raw call with manual decoding")
            val result = getPollWithRawCall(pollId)
            if (result != null) {
                Log.d("PollsContract", "✅ Strategy 2 succeeded")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w("PollsContract", "Strategy 2 failed", e)
        }
        
        // Strategy 3: Try basic poll without dynamic arrays
        try {
            Log.d("PollsContract", "Fallback Strategy 3: Basic poll + separate options")
            val basicPoll = getPollBasic(pollId)
            val options = getPollOptions(pollId)
            
            if (basicPoll != null && options != null) {
                val poll = Poll(
                    creator = basicPoll.creator,
                    subject = basicPoll.title,
                    description = basicPoll.description,
                    category = "", // Not available in basic poll
                    status = "", // Not available in basic poll
                    viewType = "", // Not available in basic poll
                    options = options.texts,
                    rewardPerResponse = BigInteger.ZERO, // Not available in basic poll
                    maxResponses = BigInteger.ZERO, // Not available in basic poll
                    durationDays = BigInteger.ZERO, // Not available in basic poll
                    minContribution = BigInteger.ZERO, // Not available in basic poll
                    fundingType = "", // Not available in basic poll
                    targetFund = BigInteger.ZERO, // Not available in basic poll
                    endTime = basicPoll.endTime,
                    isOpen = basicPoll.isActive,
                    totalResponses = basicPoll.totalVotes,
                    funds = BigInteger.ZERO, // Not available in basic poll
                    rewardToken = "", // Not available in basic poll
                    rewardDistribution = "" // Not available in basic poll
                )
                Log.d("PollsContract", "✅ Strategy 3 succeeded")
                return@withContext poll
            }
        } catch (e: Exception) {
            Log.w("PollsContract", "Strategy 3 failed", e)
        }
        
        // Strategy 4: Try direct RPC call
        try {
            Log.d("PollsContract", "Fallback Strategy 4: Direct RPC call")
            val result = getPollWithDirectRPC(pollId)
            if (result != null) {
                Log.d("PollsContract", "✅ Strategy 4 succeeded")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w("PollsContract", "Strategy 4 failed", e)
        }
        
        Log.e("PollsContract", "❌ All fallback strategies failed for poll $pollId")
        return@withContext null
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
