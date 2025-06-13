package com.blurtopian.dpolls.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Web3Module {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeb3j(okHttpClient: OkHttpClient): Web3j {
        // Replace with your actual RPC URL
        val rpcUrl = "YOUR_RPC_URL"
        return Web3j.build(HttpService(rpcUrl, okHttpClient))
    }

    @Provides
    @Singleton
    fun provideContractGasProvider(): ContractGasProvider {
        return DefaultGasProvider()
    }
} 