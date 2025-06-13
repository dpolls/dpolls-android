package com.blurtopian.dpolls.domain.model

import java.math.BigInteger

/**
 * Domain model for a poll
 */
data class Poll(
    val id: BigInteger,
    val title: String,
    val description: String,
    val creator: String,
    val startTime: BigInteger,
    val endTime: BigInteger,
    val isActive: Boolean,
    val totalVotes: BigInteger,
    val options: List<PollOption>,
    val userVote: UserVote? = null
) {
    /**
     * Check if poll is currently active and within time bounds
     */
    fun isCurrentlyActive(): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        return isActive && 
               currentTime >= startTime.toLong() && 
               currentTime <= endTime.toLong()
    }
    
    /**
     * Get time remaining in seconds
     */
    fun getTimeRemainingSeconds(): Long {
        val currentTime = System.currentTimeMillis() / 1000
        val endTimeSeconds = endTime.toLong()
        return if (endTimeSeconds > currentTime) {
            endTimeSeconds - currentTime
        } else {
            0L
        }
    }
    
    /**
     * Check if user has voted
     */
    fun hasUserVoted(): Boolean = userVote?.hasVoted == true
    
    /**
     * Get winning option (option with most votes)
     */
    fun getWinningOption(): PollOption? {
        return options.maxByOrNull { it.voteCount }
    }
    
    /**
     * Get poll duration in hours
     */
    fun getDurationHours(): Long {
        return (endTime.toLong() - startTime.toLong()) / 3600
    }
}

/**
 * Domain model for a poll option
 */
data class PollOption(
    val index: Int,
    val text: String,
    val voteCount: BigInteger
) {
    /**
     * Calculate percentage of total votes
     */
    fun getPercentage(totalVotes: BigInteger): Double {
        return if (totalVotes > BigInteger.ZERO) {
            (voteCount.toDouble() / totalVotes.toDouble()) * 100.0
        } else {
            0.0
        }
    }
}

/**
 * Domain model for user's vote information
 */
data class UserVote(
    val hasVoted: Boolean,
    val voteTimestamp: BigInteger,
    val optionIndex: BigInteger,
    val weight: BigInteger
) {
    /**
     * Get vote date as timestamp
     */
    fun getVoteDate(): Long = voteTimestamp.toLong() * 1000
}

/**
 * Domain model for creating a new poll
 */
data class CreatePollRequest(
    val title: String,
    val description: String,
    val options: List<String>,
    val durationInHours: Int,
    val allowMultipleVotes: Boolean = false
) {
    /**
     * Validate poll creation request
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (title.isBlank()) {
            errors.add("Title cannot be empty")
        }
        
        if (title.length > 200) {
            errors.add("Title cannot exceed 200 characters")
        }
        
        if (description.isBlank()) {
            errors.add("Description cannot be empty")
        }
        
        if (description.length > 1000) {
            errors.add("Description cannot exceed 1000 characters")
        }
        
        if (options.size < 2) {
            errors.add("Poll must have at least 2 options")
        }
        
        if (options.size > 20) {
            errors.add("Poll cannot have more than 20 options")
        }
        
        options.forEachIndexed { index, option ->
            if (option.isBlank()) {
                errors.add("Option ${index + 1} cannot be empty")
            }
            if (option.length > 100) {
                errors.add("Option ${index + 1} cannot exceed 100 characters")
            }
        }
        
        if (durationInHours < 1) {
            errors.add("Duration must be at least 1 hour")
        }
        
        if (durationInHours > 8760) { // 1 year
            errors.add("Duration cannot exceed 1 year")
        }
        
        return errors
    }
    
    /**
     * Check if request is valid
     */
    fun isValid(): Boolean = validate().isEmpty()
}

/**
 * Domain model for voting on a poll
 */
data class VoteRequest(
    val pollId: BigInteger,
    val optionIndex: Int
)

/**
 * Domain model for poll results
 */
data class PollResults(
    val poll: Poll,
    val optionResults: List<OptionResult>
) {
    /**
     * Get total votes across all options
     */
    fun getTotalVotes(): BigInteger = poll.totalVotes
    
    /**
     * Get winning option result
     */
    fun getWinningResult(): OptionResult? {
        return optionResults.maxByOrNull { it.voteCount }
    }
}

/**
 * Domain model for individual option results
 */
data class OptionResult(
    val option: PollOption,
    val voteCount: BigInteger,
    val percentage: Double
)

/**
 * Domain model for wallet information
 */
data class WalletInfo(
    val address: String,
    val balance: BigInteger,
    val isConnected: Boolean,
    val providerType: String
) {
    /**
     * Get short address for display
     */
    fun getShortAddress(): String {
        return if (address.length >= 10) {
            "${address.substring(0, 6)}...${address.substring(address.length - 4)}"
        } else {
            address
        }
    }
    
    /**
     * Get balance in NERO (converted from Wei)
     */
    fun getBalanceInNero(): Double {
        return balance.toDouble() / 1_000_000_000_000_000_000.0
    }
    
    /**
     * Get formatted balance string
     */
    fun getFormattedBalance(): String {
        val neroBalance = getBalanceInNero()
        return String.format("%.4f NERO", neroBalance)
    }
}

/**
 * Domain model for transaction information
 */
data class TransactionInfo(
    val hash: String,
    val status: TransactionStatus,
    val gasUsed: BigInteger?,
    val gasPrice: BigInteger?,
    val blockNumber: BigInteger?,
    val timestamp: Long?
) {
    /**
     * Get transaction cost in Wei
     */
    fun getTransactionCost(): BigInteger? {
        return if (gasUsed != null && gasPrice != null) {
            gasUsed * gasPrice
        } else null
    }
    
    /**
     * Get transaction cost in NERO
     */
    fun getTransactionCostInNero(): Double? {
        val costWei = getTransactionCost()
        return costWei?.let { it.toDouble() / 1_000_000_000_000_000_000.0 }
    }
}

/**
 * Enum for transaction status
 */
enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED
}

/**
 * Domain model for blockchain network information
 */
data class NetworkInfo(
    val chainId: String,
    val networkName: String,
    val rpcUrl: String,
    val blockExplorerUrl: String,
    val isTestnet: Boolean
)

/**
 * Domain model for gas estimation
 */
data class GasEstimate(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    val estimatedCost: BigInteger
) {
    /**
     * Get estimated cost in NERO
     */
    fun getEstimatedCostInNero(): Double {
        return estimatedCost.toDouble() / 1_000_000_000_000_000_000.0
    }
    
    /**
     * Get formatted cost string
     */
    fun getFormattedCost(): String {
        val neroCost = getEstimatedCostInNero()
        return String.format("%.6f NERO", neroCost)
    }
}

