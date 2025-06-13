package com.blurtopian.dpolls.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blurtopian.dpolls.data.model.Poll
import com.blurtopian.dpolls.databinding.ItemPollBinding

class PollsAdapter(
    private val onPollClick: (Poll) -> Unit
) : ListAdapter<Poll, PollsAdapter.PollViewHolder>(PollDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val binding = ItemPollBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PollViewHolder(
        private val binding: ItemPollBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPollClick(getItem(position))
                }
            }
        }

        fun bind(poll: Poll) {
            binding.apply {
                pollTitle.text = poll.title
                pollDescription.text = poll.description
                totalVotes.text = "${poll.totalVotes} votes"
                // Add more binding as needed
            }
        }
    }

    private class PollDiffCallback : DiffUtil.ItemCallback<Poll>() {
        override fun areItemsTheSame(oldItem: Poll, newItem: Poll): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Poll, newItem: Poll): Boolean {
            return oldItem == newItem
        }
    }
} 