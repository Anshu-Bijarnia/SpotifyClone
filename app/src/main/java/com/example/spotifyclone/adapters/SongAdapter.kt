package com.example.spotifyclone.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.data.entities.Song
import javax.inject.Inject
import com.example.spotifyclone.databinding.ListItemBinding

// To display our list of songs
class SongAdapter @Inject constructor(
    private val glide : RequestManager
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    class SongViewHolder (val binding: ListItemBinding) : RecyclerView.ViewHolder (binding.root)

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

    var songs : List<Song>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemBinding.inflate(layoutInflater,parent,false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.binding.apply {
            tvPrimary.text = song.title
            tvSecondary.text = song.subtitle
            glide.load(song.imageUrl).into(ivItemImage)
        }
        holder.itemView.setOnClickListener {
            onItemClickListener?.let { click ->
                click(song)
            }
        }
    }
    private var onItemClickListener : ((Song) -> Unit)? = null
    fun setOnItemClickListener (listener : (Song) -> Unit) {
        onItemClickListener = listener
    }
    override fun getItemCount(): Int {
        return songs.size
    }
}