package io.dee.portal.search_screen.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.dee.portal.data.local.Location
import io.dee.portal.databinding.SearchListItemViewBinding

class SearchedListAdapter(
    private val onClickListener: (Location) -> Unit
) : ListAdapter<Location, SearchedListAdapter.ViewHolder>(SearchedListModel()) {
    inner class ViewHolder(private val binding: SearchListItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onClickListener.invoke(currentList[adapterPosition])
            }
        }

        fun bind(item: Location) {
            binding.apply {
                tvLocationTitle.text = item.title
                tvLocationAddress.text = item.address
                isFromSearch = item.from == Location.Type.Search
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            SearchListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(this.currentList[position])

}

class SearchedListModel : DiffUtil.ItemCallback<Location>() {
    override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
        return oldItem.latitude == newItem.latitude && oldItem.longitude == newItem.longitude
    }

    override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
        return newItem == oldItem
    }
}