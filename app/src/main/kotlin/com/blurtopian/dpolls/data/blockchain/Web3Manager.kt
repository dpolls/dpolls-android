package com.blurtopian.dpolls.data.blockchain

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.net.ConnectException
import java.util.concurrent.TimeUnit
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
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Initialize Web3j connection
     */
    private suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = ""
            Log.d(TAG, "Initializing Web3j with RPC URL: $rpcUrl")
            
            web3j = Web3j.build(
                HttpService(rpcUrl, okHttpClient)
            )
            
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

}

