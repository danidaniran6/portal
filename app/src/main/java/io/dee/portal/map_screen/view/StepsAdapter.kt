package io.dee.portal.map_screen.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.dee.portal.R
import io.dee.portal.databinding.StepsItemViewBinding
import io.dee.portal.map_screen.data.dto.Step

class StepsAdapter(private val onStepSelected: (position: Int) -> Unit) :
    ListAdapter<Step, StepsAdapter.StepViewHolder>(StepDiffAdapter()) {
    inner class StepViewHolder(private val binding: StepsItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {

                    onStepSelected(adapterPosition)
                }
            }
        }

        fun bind(step: Step) {
            binding.apply {
                tvStepDistance.visibility =
                    if (!step.distance?.text.isNullOrEmpty()) View.VISIBLE else View.GONE
                tvStepDistance.text = step.distance?.text ?: ""
                tvStepInstruction.text = step.instruction ?: ""
                val drawable = ContextCompat.getDrawable(
                    itemView.context,
                    Step.ModifierEnum.getModifier(step.modifier).icon
                )
                ivStepDirection.setImageDrawable(drawable)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = StepsItemViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) =
        holder.bind(currentList[position])
}

class StepDiffAdapter() : DiffUtil.ItemCallback<Step>() {
    override fun areItemsTheSame(oldItem: Step, newItem: Step): Boolean {
        return newItem.name == oldItem.name
    }

    override fun areContentsTheSame(oldItem: Step, newItem: Step): Boolean {
        return newItem == oldItem
    }
}
