package com.blurtopian.dpolls.di

import com.blurtopian.dpolls.data.repository.PollsRepository
import com.blurtopian.dpolls.data.repository.PollsRepositoryImpl
import com.blurtopian.dpolls.data.web3.Web3Service
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindPollsRepository(
        pollsRepositoryImpl: PollsRepositoryImpl
    ): PollsRepository
} 