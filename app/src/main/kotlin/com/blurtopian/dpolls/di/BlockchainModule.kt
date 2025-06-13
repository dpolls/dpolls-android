package com.blurtopian.dpolls.di

import android.content.Context
import com.blurtopian.dpolls.BuildConfig
import com.blurtopian.dpolls.data.blockchain.PollsContract
import com.blurtopian.dpolls.data.blockchain.Web3Manager
import com.blurtopian.dpolls.data.blockchain.WalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.web3j.protocol.Web3j
import javax.inject.Singleton

/**
 * Dependency injection module for blockchain-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object BlockchainModule {
    
    @Provides
    @Singleton
    fun provideWeb3Manager(
        @ApplicationContext context: Context
    ): Web3Manager {
        return Web3Manager(context)
    }
    
    @Provides
    @Singleton
    fun provideWalletManager(
        @ApplicationContext context: Context
    ): WalletManager {
        return WalletManager(context)
    }
    
    @Provides
    @Singleton
    fun provideWeb3j(
        web3Manager: Web3Manager
    ): Web3j? {
        return web3Manager.getWeb3j()
    }
    
    @Provides
    @Singleton
    fun providePollsContract(
        web3Manager: Web3Manager
    ): PollsContract {
        val web3j = web3Manager.getWeb3j()
            ?: throw IllegalStateException("Web3j not initialized")
        
        return PollsContract(
            web3j = web3j,
            contractAddress = BuildConfig.POLLS_CONTRACT_ADDRESS
        )
    }
    
    @Provides
    @Singleton
    fun provideContractAddress(): String {
        return BuildConfig.POLLS_CONTRACT_ADDRESS
    }
    
    @Provides
    @Singleton
    fun provideRpcUrl(): String {
        return BuildConfig.NERO_RPC_URL
    }
    
    @Provides
    @Singleton
    fun provideChainId(): String {
        return BuildConfig.NERO_CHAIN_ID
    }
}

