package com.blurtopian.dpolls.data.blockchain

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.WalletUtils
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * Manager for wallet operations and Web3Auth integration
 */
@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WalletManager"
        private const val PREFS_NAME = "wallet_prefs"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_ADDRESS = "address"
        private const val KEY_IS_CONNECTED = "is_connected"
        private const val KEY_PROVIDER_TYPE = "provider_type"
        
        // Web3Auth configuration
        private const val WEB3AUTH_CLIENT_ID = "YOUR_WEB3AUTH_CLIENT_ID"
        private const val WEB3AUTH_REDIRECT_URL = "com.blurtopian.dpolls://web3auth"
    }
    
    private var web3Auth: Web3Auth? = null
    private var encryptedPrefs: EncryptedSharedPreferences? = null
    private var currentCredentials: Credentials? = null
    
    enum class ProviderType {
        WEB3AUTH,
        PRIVATE_KEY,
        NONE
    }
    
    init {
        initializeEncryptedPrefs()
        initializeWeb3Auth()
    }
    
    /**
     * Initialize wallet manager
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            // Initialize encrypted shared preferences
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            encryptedPrefs?.let {
                // Initialize Web3Auth
                initializeWeb3Auth()
                
                // Restore previous session if exists
                restoreSession()
            }
            
            Log.d(TAG, "Wallet manager initialized successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize wallet manager", e)
            return@withContext false
        }
    }
    
    /**
     * Initialize Web3Auth SDK
     */
    private fun initializeWeb3Auth() {
        try {
            val web3AuthOptions = Web3AuthOptions(
                clientId = WEB3AUTH_CLIENT_ID,
                network = Network.TESTNET,
                redirectUrl = WEB3AUTH_REDIRECT_URL.toUri(),
                whiteLabel = WhiteLabelData(
                    appName = "Polls dApp",
                    logoLight = "https://your-logo-url.com/logo-light.png",
                    logoDark = "https://your-logo-url.com/logo-dark.png",
                    defaultLanguage = Language.EN,
                    mode = ThemeModes.DARK,
                    theme = hashMapOf(
                        "primary" to "#1976D2"
                    )
                )
            )
            
            web3Auth = Web3Auth(web3AuthOptions, context)
            
            // Set result callback
            web3Auth?.setResultUrl(Uri.parse(WEB3AUTH_REDIRECT_URL))
            
            Log.d(TAG, "Web3Auth initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Web3Auth", e)
        }
    }
    
    /**
     * Connect wallet using Web3Auth
     */
    suspend fun connectWithWeb3Auth(
        loginProvider: Provider = Provider.GOOGLE
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            val web3AuthInstance = web3Auth ?: return@withContext false
            
            val loginParams = LoginParams(
                loginProvider = loginProvider,
                extraLoginOptions = ExtraLoginOptions(
                    display = Display.POPUP,
                    prompt = Prompt.LOGIN
                )
            )

            val web3AuthResponseFuture = web3AuthInstance.login(loginParams);
            web3AuthResponseFuture.whenComplete { web3AuthResponse, throwable ->
                if (web3AuthResponse.error != null) {
                    Log.e(TAG, "Web3Auth login error: ${web3AuthResponse.error}")
                    return@whenComplete
                }

                val privateKey = web3AuthResponse.privKey
                if (privateKey.isNullOrEmpty()) {
                    Log.e(TAG, "No private key received from Web3Auth")
                    return@whenComplete
                }
                // Create credentials from private key
                val credentials = Credentials.create(privateKey)
                currentCredentials = credentials

                // Save to encrypted preferences
                saveWalletInfo(privateKey, credentials.address, ProviderType.WEB3AUTH)

                Log.d(TAG, "Successfully connected with Web3Auth")
                Log.d(TAG, "Address: ${credentials.address}")
            }

            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting with Web3Auth", e)
            return@withContext false
        }
    }
    
    /**
     * Connect wallet using private key
     */
    suspend fun connectWithPrivateKey(privateKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate private key format
            val cleanPrivateKey = if (privateKey.startsWith("0x")) {
                privateKey.substring(2)
            } else {
                privateKey
            }
            
            if (cleanPrivateKey.length != 64) {
                Log.e(TAG, "Invalid private key length")
                return@withContext false
            }
            
            // Create credentials
            val credentials = Credentials.create(cleanPrivateKey)
            currentCredentials = credentials
            
            // Save to encrypted preferences
            saveWalletInfo(cleanPrivateKey, credentials.address, ProviderType.PRIVATE_KEY)
            
            Log.d(TAG, "Successfully connected with private key")
            Log.d(TAG, "Address: ${credentials.address}")
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting with private key", e)
            return@withContext false
        }
    }
    
    /**
     * Generate new wallet
     */
    suspend fun generateNewWallet(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val ecKeyPair = Keys.createEcKeyPair()
            val privateKey = ecKeyPair.privateKey.toString(16)
            val credentials = Credentials.create(ecKeyPair)
            
            Log.d(TAG, "Generated new wallet")
            Log.d(TAG, "Address: ${credentials.address}")
            
            return@withContext Pair(privateKey, credentials.address)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating new wallet", e)
            return@withContext null
        }
    }
    
    /**
     * Get current wallet credentials
     */
    fun getCurrentCredentials(): Credentials? = currentCredentials
    
    /**
     * Get current wallet address
     */
    fun getCurrentAddress(): String? = currentCredentials?.address
    
    /**
     * Check if wallet is connected
     */
    fun isConnected(): Boolean {
        return currentCredentials != null && 
               encryptedPrefs?.getBoolean(KEY_IS_CONNECTED, false) == true
    }
    
    /**
     * Get provider type
     */
    fun getProviderType(): ProviderType {
        val providerName = encryptedPrefs?.getString(KEY_PROVIDER_TYPE, null)
        return try {
            ProviderType.valueOf(providerName ?: "NONE")
        } catch (e: Exception) {
            ProviderType.NONE
        }
    }
    
    /**
     * Sign message
     */
    suspend fun signMessage(message: String): String? = withContext(Dispatchers.IO) {
        try {
            val credentials = currentCredentials ?: return@withContext null
            
            val messageBytes = message.toByteArray()
            val signature = Sign.signPrefixedMessage(messageBytes, credentials.ecKeyPair)
            
            // Combine r, s, v into signature string
            val r = signature.r
            val s = signature.s
            val v = signature.v
            
            val signatureBytes = ByteArray(65)
            System.arraycopy(r, 0, signatureBytes, 0, 32)
            System.arraycopy(s, 0, signatureBytes, 32, 32)
            signatureBytes[64] = v[0]
            
            return@withContext "0x" + signatureBytes.joinToString("") { "%02x".format(it) }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error signing message", e)
            return@withContext null
        }
    }
    
    /**
     * Disconnect wallet
     */
    suspend fun disconnect(): Boolean = withContext(Dispatchers.Main) {
        try {
            // Logout from Web3Auth if connected via Web3Auth
            if (getProviderType() == ProviderType.WEB3AUTH) {
                web3Auth?.logout()
            }
            
            // Clear credentials
            currentCredentials = null
            
            // Clear encrypted preferences
            clearWalletInfo()
            
            Log.d(TAG, "Wallet disconnected")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting wallet", e)
            return@withContext false
        }
    }
    
    /**
     * Save wallet information to encrypted preferences
     */
    private fun saveWalletInfo(privateKey: String, address: String, providerType: ProviderType) {
        encryptedPrefs?.edit()?.apply {
            putString(KEY_PRIVATE_KEY, privateKey)
            putString(KEY_ADDRESS, address)
            putBoolean(KEY_IS_CONNECTED, true)
            putString(KEY_PROVIDER_TYPE, providerType.name)
            apply()
        }
    }
    
    /**
     * Restore previous session
     */
    private suspend fun restoreSession() = withContext(Dispatchers.IO) {
        try {
            val isConnected = encryptedPrefs?.getBoolean(KEY_IS_CONNECTED, false) ?: false
            if (!isConnected) return@withContext
            
            val privateKey = encryptedPrefs?.getString(KEY_PRIVATE_KEY, null)
            if (privateKey.isNullOrEmpty()) return@withContext
            
            val credentials = Credentials.create(privateKey)
            currentCredentials = credentials
            
            Log.d(TAG, "Session restored for address: ${credentials.address}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session", e)
            // Clear invalid session data
            disconnect()
        }
    }
    
    /**
     * Export private key (for backup purposes)
     */
    fun exportPrivateKey(): String? {
        return if (isConnected()) {
            encryptedPrefs?.getString(KEY_PRIVATE_KEY, null)
        } else null
    }
    
    /**
     * Validate Ethereum address
     */
    fun isValidAddress(address: String): Boolean {
        return try {
            WalletUtils.isValidAddress(address)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get short address for display (0x1234...5678)
     */
    fun getShortAddress(address: String? = getCurrentAddress()): String {
        return if (address != null && address.length >= 10) {
            "${address.substring(0, 6)}...${address.substring(address.length - 4)}"
        } else {
            "Unknown"
        }
    }

    private fun initializeEncryptedPrefs() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences

            Log.d(TAG, "Encrypted preferences initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted preferences", e)
        }
    }

    private fun clearWalletInfo() {
        encryptedPrefs?.edit()?.apply {
            remove(KEY_PRIVATE_KEY)
            remove(KEY_ADDRESS)
            remove(KEY_PROVIDER_TYPE)
            putBoolean(KEY_IS_CONNECTED, false)
            apply()
        }
    }
}

