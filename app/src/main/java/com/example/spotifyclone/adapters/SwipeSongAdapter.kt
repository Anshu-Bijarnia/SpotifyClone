package com.example.spotifyclone.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.databinding.SwipeItemBinding

// To make swiping songs possible
class SwipeSongAdapter : RecyclerView.Adapter<SwipeSongAdapter.SongViewHolder>() {
    class SongViewHolder (val binding: SwipeItemBinding) : RecyclerView.ViewHolder (binding.root)

    private val diffCallback = object : DiffUtil.ItemCallback<Song> (){
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }
    // The actual list differ , which will check the differences in data. All this will happen asynchronously
    private val differ = AsyncListDiffer(this,diffCallback)

    // We will set this from outside, so we update the getter and setter
    var songs : List<Song>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SwipeItemBinding.inflate(layoutInflater,parent,false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.binding.apply {
            val text = "${song.title} - ${song.subtitle}"
            tvPrimary.text = text
        }
// Setting the onclick listener to this item view
        holder.itemView.setOnClickListener {
            onItemClickListener?.let { click ->
                click(song)
            }
        }
    }
    // To be able to set this listener from our fragment, when we click an item we actually want to play that song
    private var onItemClickListener : ((Song) -> Unit)? = null
    fun setOnItemClickListener (listener : (Song) -> Unit) {
        onItemClickListener = listener
    }
    override fun getItemCount(): Int {
        return songs.size
    }
}