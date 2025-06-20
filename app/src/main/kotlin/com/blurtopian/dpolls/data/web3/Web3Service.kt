package com.blurtopian.dpolls.data.web3

import android.util.Log
import com.blurtopian.dpolls.BuildConfig
import com.blurtopian.dpolls.data.blockchain.PollsContract
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.domain.model.Poll
import com.blurtopian.dpolls.domain.model.PollOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private var pollsContractKeth: PollsContract? = null
    private val contractAddress = BuildConfig.POLLS_CONTRACT_ADDRESS

    init {
        initializeContract()
    }

    private fun initializeContract() {
        try {
            Log.d("Web3Service", "Initializing contract with address: $contractAddress")
            Log.d("Web3Service", "Network configuration - RPC URL: ${BuildConfig.NERO_RPC_URL}")
            // Initialize contract even without credentials for read operations
            pollsContract = PollsContract(web3j, contractAddress)
            pollsContractKeth = PollsContract(web3j, contractAddress)
            Log.d("Web3Service", "Contract initialized successfully")
        } catch (e: Exception) {
            Log.e("Web3Service", "Failed to initialize contract", e)
            e.printStackTrace()
        }
    }

    suspend fun getPoll(pollId: String): Poll? = withContext(Dispatchers.IO) {
        return@withContext try {
            val contract = pollsContract ?: return@withContext null
            
            // First try to get basic poll info without the problematic dynamic array
            val basicPoll = contract.getPollWithFallback(BigInteger(pollId)) ?: return@withContext null
            
            // Then get options separately
            val options = contract.getPollOptions(BigInteger(pollId)) ?: return@withContext null
            
            Poll(
                id = pollId,
                title = basicPoll.title,
                description = basicPoll.description,
                creator = basicPoll.creator,
                startTime = basicPoll.startTime.toLong(),
                endTime = basicPoll.endTime.toLong(),
                options = options.texts.zip(options.voteCounts).mapIndexed { index, (text, voteCount) ->
                    PollOption(
                        id = index.toString(),
                        text = text,
                        voteCount = voteCount
                    )
                },
                isActive = basicPoll.isActive,
            )
        } catch (e: Exception) {
            Log.e("Web3Service", "Error getting poll $pollId", e)
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

    /**
     * Check if Web3j connection is working
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("Web3Service", "Checking Web3j connection...")
            val blockNumber = web3j.ethBlockNumber().send()
            if (blockNumber.hasError()) {
                Log.e("Web3Service", "Connection check failed: ${blockNumber.error.message}")
                false
            } else {
                Log.d("Web3Service", "Connection successful, latest block: ${blockNumber.blockNumber}")
                true
            }
        } catch (e: Exception) {
            Log.e("Web3Service", "Connection check failed with exception", e)
            false
        }
    }

    /**
     * Test if the contract exists and is accessible
     */
    suspend fun testContractAccess(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("Web3Service", "Testing contract access...")
            val contract = pollsContract ?: return@withContext false
            
            // First test network connection
            val networkTest = contract.testNetworkConnection()
            if (!networkTest) {
                Log.e("Web3Service", "Network connection test failed - not connected to NERO chain")
                return@withContext false
            }
            
            // Get network information
            val networkInfo = contract.getNetworkInfo()
            Log.d("Web3Service", "Network Info: $networkInfo")
            
            // First test if contract exists
            val exists = contract.testContractExists()
            if (!exists) {
                Log.e("Web3Service", "Contract does not exist at address: $contractAddress")
                return@withContext false
            }
            
            // Analyze what functions are actually available in the contract
            val availableFunctions = contract.analyzeContractFunctions()
            Log.d("Web3Service", "Available functions in contract: $availableFunctions")
            
            // Test if contract has the expected function selectors
            val bytecodeTest = contract.testContractBytecode()
            if (!bytecodeTest) {
                Log.e("Web3Service", "Contract bytecode test failed - contract may not have expected functions")
                return@withContext false
            }
            
            // Then test if we can call a simple function
            val connectionTest = contract.testConnection()
            if (!connectionTest) {
                Log.e("Web3Service", "Contract exists but function calls are failing")
                return@withContext false
            }
            
            // Test getPoll function signature specifically
            val getPollTest = contract.testGetPollSignature()
            if (!getPollTest) {
                Log.e("Web3Service", "getPoll function signature test failed")
                // Don't return false here, just log the warning
                Log.w("Web3Service", "Continuing despite getPoll signature test failure")
            }
            
            Log.d("Web3Service", "Contract access test successful")
            true
        } catch (e: Exception) {
            Log.e("Web3Service", "Contract access test failed", e)
            false
        }
    }

    suspend fun getPolls(): List<Poll> = withContext(Dispatchers.IO) {
        Log.d("Web3Service", "Starting getPolls()")
        return@withContext try {
            val contract = pollsContract
            if (contract == null) {
                Log.e("Web3Service", "Contract is null, cannot fetch polls")
                return@withContext emptyList()
            }
            
            // Test contract access first
            val contractAccessible = testContractAccess()
            if (!contractAccessible) {
                Log.e("Web3Service", "Contract is not accessible, cannot fetch polls")
                return@withContext emptyList()
            }
            
            Log.d("Web3Service", "Calling contract.getAllPollIds()")
            val pollIds = contract.getAllPollIds()
            Log.d("Web3Service", "Total polls count: ${pollIds.size}")
            
            if (pollIds.isEmpty()) {
                Log.d("Web3Service", "No polls found, returning empty list")
                return@withContext emptyList()
            }
            
            val polls = mutableListOf<Poll>()
            
            pollIds.forEach { pollId ->
                try {
                    Log.d("Web3Service", "Fetching poll with ID: $pollId")
                    
                    val poll = contract.getPoll(pollId)
                    if (poll == null) {
                        Log.w("Web3Service", "Poll $pollId returned null, skipping")
                        return@forEach
                    }
                    
                    Log.d("Web3Service", "Poll $pollId details: title=${poll.title}, creator=${poll.creator}")
                    
                    val options = contract.getPollOptions(pollId)
                    if (options == null) {
                        Log.w("Web3Service", "Poll $pollId options returned null, skipping")
                        return@forEach
                    }
                    
                    Log.d("Web3Service", "Poll $pollId has ${options.texts.size} options")
                    
                    val pollModel = Poll(
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
                    
                    polls.add(pollModel)
                    Log.d("Web3Service", "Successfully added poll $pollId to list")
                    
                } catch (e: Exception) {
                    Log.e("Web3Service", "Error fetching poll $pollId", e)
                }
            }
            
            Log.d("Web3Service", "Successfully retrieved ${polls.size} polls")
            polls
            
        } catch (e: Exception) {
            Log.e("Web3Service", "Error in getPolls()", e)
            e.printStackTrace()
            emptyList()
        }
    }
}