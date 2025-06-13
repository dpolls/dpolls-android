package com.blurtopian.dpolls.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.math.BigInteger

/**
 * Domain model for a poll
 */
@Parcelize
data class Poll(
    val id: String,
    val title: String,
    val description: String,
    val creator: String,
    val startTime: Long,
    val endTime: Long,
    val options: List<PollOption>,
    val isActive: Boolean = true,
    val totalVotes: BigInteger = BigInteger.ZERO,
) : Parcelable {
    /**
     * Check if poll is currently active and within time bounds
     */
    fun isCurrentlyActive(): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        return isActive && currentTime >= startTime && currentTime <= endTime
    }
    
    /**
     * Get time remaining in seconds
     */
    fun getTimeRemainingSeconds(): Long {
        val currentTime = System.currentTimeMillis() / 1000
        return if (endTime > currentTime) {
            endTime - currentTime
        } else 0
    }
    
    /**
     * Get poll duration in hours
     */
    fun getDurationHours(): Long {
        return (endTime - startTime) / 3600
    }
}

/**
 * Domain model for a poll option
 */
@Parcelize
data class PollOption(
    val id: String,
    val text: String,
    val voteCount: @RawValue BigInteger = BigInteger.ZERO
) : Parcelable {
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
@Parcelize
data class UserVote(
    val pollId: String,
    val hasVoted: Boolean,
    val optionIndex: @RawValue BigInteger,
    val weight: @RawValue BigInteger,
    val voteTimestamp: @RawValue BigInteger
) : Parcelable {
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
    val options: List<PollOption>,
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
            if (option.text.isBlank()) {
                errors.add("Option ${index + 1} cannot be empty")
            }
            if (option.text.length > 100) {
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
    val pollId: String,
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
    fun getTotalVotes(): BigInteger = poll.options.sumOf { it.voteCount }
    
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

