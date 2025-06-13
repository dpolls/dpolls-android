package com.blurtopian.dpolls.data.blockchain

import android.content.Context
import android.util.Log
import com.blurtopian.dpolls.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.websocket.WebSocketService
import java.math.BigInteger
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Web3 blockchain connections and operations
 */
@Singleton
class Web3Manager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "Web3Manager"
        private const val CONNECTION_TIMEOUT = 30000L // 30 seconds
        private const val READ_TIMEOUT = 60000L // 60 seconds
    }
    
    private var web3j: Web3j? = null
    private var isConnected = false
    private var currentNetworkId: String? = null
    
    /**
     * Initialize Web3j connection
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = BuildConfig.NERO_RPC_URL
            Log.d(TAG, "Initializing Web3j with RPC URL: $rpcUrl")
            
            // Create HTTP service with timeouts
            val httpService = HttpService(rpcUrl).apply {
                setConnectionTimeout(CONNECTION_TIMEOUT)
                setReadTimeout(READ_TIMEOUT)
            }
            
            web3j = Web3j.build(httpService)
            
            // Test connection
            val clientVersion = web3j?.web3ClientVersion()?.send()
            if (clientVersion?.hasError() == true) {
                Log.e(TAG, "Web3 connection error: ${clientVersion.error.message}")
                return@withContext false
            }
            
            // Verify network
            val networkId = web3j?.netVersion()?.send()?.netVersion
            currentNetworkId = networkId
            
            Log.d(TAG, "Connected to network: $networkId")
            Log.d(TAG, "Client version: ${clientVersion?.web3ClientVersion}")
            
            isConnected = true
            return@withContext true
            
        } catch (e: ConnectException) {
            Log.e(TAG, "Failed to connect to NERO network", e)
            isConnected = false
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Web3 initialization", e)
            isConnected = false
            return@withContext false
        }
    }
    
    /**
     * Get Web3j instance
     */
    fun getWeb3j(): Web3j? {
        return if (isConnected) web3j else null
    }
    
    /**
     * Check if connected to blockchain
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Get current network ID
     */
    fun getCurrentNetworkId(): String? = currentNetworkId
    
    /**
     * Get expected chain ID for current build
     */
    fun getExpectedChainId(): String = BuildConfig.NERO_CHAIN_ID
    
    /**
     * Verify we're connected to the correct network
     */
    suspend fun verifyNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            val web3 = getWeb3j() ?: return@withContext false
            val chainId = web3.ethChainId().send().chainId
            val expectedChainId = BigInteger(getExpectedChainId())
            
            val isCorrectNetwork = chainId == expectedChainId
            if (!isCorrectNetwork) {
                Log.w(TAG, "Wrong network! Expected: $expectedChainId, Got: $chainId")
            }
            
            return@withContext isCorrectNetwork
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying network", e)
            return@withContext false
        }
    }
    
    /**
     * Get current gas price
     */
    suspend fun getCurrentGasPrice(): BigInteger? = withContext(Dispatchers.IO) {
        try {
            return@withContext getWeb3j()?.ethGasPrice()?.send()?.gasPrice
        } catch (e: Exception) {
            Log.e(TAG, "Error getting gas price", e)
            return@withContext null
        }
    }
    
    /**
     * Get account balance
     */
    suspend fun getBalance(address: String): BigInteger? = withContext(Dispatchers.IO) {
        try {
            val web3 = getWeb3j() ?: return@withContext null
            val balance = web3.ethGetBalance(address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send()
                .balance
            
            Log.d(TAG, "Balance for $address: $balance wei")
            return@withContext balance
        } catch (e: Exception) {
            Log.e(TAG, "Error getting balance for $address", e)
            return@withContext null
        }
    }
    
    /**
     * Get current block number
     */
    suspend fun getCurrentBlockNumber(): BigInteger? = withContext(Dispatchers.IO) {
        try {
            return@withContext getWeb3j()?.ethBlockNumber()?.send()?.blockNumber
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current block number", e)
            return@withContext null
        }
    }
    
    /**
     * Estimate gas for a transaction
     */
    suspend fun estimateGas(
        from: String,
        to: String,
        data: String
    ): BigInteger? = withContext(Dispatchers.IO) {
        try {
            val web3 = getWeb3j() ?: return@withContext null
            
            val transaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                from, null, null, null, to, data
            )
            
            return@withContext web3.ethEstimateGas(transaction).send().amountUsed
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating gas", e)
            return@withContext null
        }
    }
    
    /**
     * Check if address is valid
     */
    fun isValidAddress(address: String): Boolean {
        return try {
            org.web3j.utils.Numeric.cleanHexPrefix(address).length == 40 &&
                    address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Convert Wei to Ether
     */
    fun weiToEther(wei: BigInteger): Double {
        return wei.toDouble() / 1_000_000_000_000_000_000.0
    }
    
    /**
     * Convert Ether to Wei
     */
    fun etherToWei(ether: Double): BigInteger {
        return BigInteger.valueOf((ether * 1_000_000_000_000_000_000.0).toLong())
    }
    
    /**
     * Disconnect from Web3
     */
    fun disconnect() {
        try {
            web3j?.shutdown()
            web3j = null
            isConnected = false
            currentNetworkId = null
            Log.d(TAG, "Web3 connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting Web3", e)
        }
    }
    
    /**
     * Reconnect to Web3
     */
    suspend fun reconnect(): Boolean {
        disconnect()
        return initialize()
    }
    
    /**
     * Get network name for display
     */
    fun getNetworkName(): String {
        return when (currentNetworkId) {
            "1329" -> "NERO Mainnet"
            "1328" -> "NERO Testnet"
            else -> "Unknown Network ($currentNetworkId)"
        }
    }
    
    /**
     * Check if we're on testnet
     */
    fun isTestnet(): Boolean {
        return currentNetworkId == "1328"
    }
    
    /**
     * Get block explorer URL for transaction
     */
    fun getTransactionUrl(txHash: String): String {
        return if (isTestnet()) {
            "https://testnet-explorer.nerochain.io/tx/$txHash"
        } else {
            "https://explorer.nerochain.io/tx/$txHash"
        }
    }
    
    /**
     * Get block explorer URL for address
     */
    fun getAddressUrl(address: String): String {
        return if (isTestnet()) {
            "https://testnet-explorer.nerochain.io/address/$address"
        } else {
            "https://explorer.nerochain.io/address/$address"
        }
    }
}

