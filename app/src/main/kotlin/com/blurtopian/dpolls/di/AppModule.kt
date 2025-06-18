package com.blurtopian.dpolls.di

import android.content.Context
import com.blurtopian.dpolls.data.blockchain.WalletManager
import com.blurtopian.dpolls.data.web3.Web3Service
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.DefaultGasProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
//    @Provides
//    @Singleton
//    fun provideWeb3j(): Web3j {
//        return Web3j.build(HttpService("https://nero-rpc-url")) // Replace with actual RPC URL
//    }
    
    @Provides
    @Singleton
    fun provideWalletManager(
        @ApplicationContext context: Context
    ): WalletManager {
        return WalletManager(context)
    }
    
    @Provides
    @Singleton
    fun provideGasProvider(): DefaultGasProvider {
        return DefaultGasProvider()
    }
    
    @Provides
    @Singleton
    fun provideWeb3Service(
        web3j: Web3j,
        walletManager: WalletManager,
        gasProvider: DefaultGasProvider
    ): Web3Service {
        return Web3Service(web3j, walletManager, gasProvider)
    }
    
    @Provides
    @Singleton
    fun providePollsRepository(
        web3Service: Web3Service
    ): PollsRepositoryImpl {
        return PollsRepositoryImpl(web3Service)
    }
} 