package com.example.spotifyclone.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.adapters.SwipeSongAdapter
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.databinding.ActivityMainBinding
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.other.Status.*
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
// Because we want to bind this viewModel to the lifecycle of our activity we initialise it like this
    private val mainViewModel : MainViewModel by viewModels( )

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter //Recycler view adapter for our viewpager, so we can swipe through our songs

    @Inject
    lateinit var glide: RequestManager

    private var curPlayingSong : Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToObservers()

        binding.vpSong.adapter = swipeSongAdapter
    }
    // Function to switch view pager to the current song -> when a new song plays, our vp automatically swipes to the corresponding song
    private fun switchViewPagerToCurrentSong (song : Song){
        // Check at which index our current song is, return -1 if this song doesn't exist in the list
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1){
            binding.vpSong.currentItem = newItemIndex //index of the current item that is displayed in that vp
            curPlayingSong = song
        }
    }
    // function to subscribe to our observers
    // observe them so that we can fill our vp with these items, and display the right item when we launch our app
    private fun subscribeToObservers (){
        mainViewModel.mediaItems.observe(this){
            it?.let { result ->
                when(result.status){
                    //
                    SUCCESS -> {
                       result.data?.let { songs ->
                           swipeSongAdapter.songs = songs
                           if (songs.isNotEmpty()) {
                               glide.load((curPlayingSong ?: songs[0]).imageUrl).into(binding.ivCurSongImage)
                           }
                           // if curplaying song is null we return out of this observe block, we just display the first song by default
                           switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
                       }
                    }
                    ERROR -> Unit // These media items don't even emit error, because we have already have that in home fragment
                    LOADING -> Unit

                }
            }
            // Every time we get new information about the currently playing song (if the song switches) then this observer here will trigger
            // Here we will update the currently playing song and also switch the viewpager accordingly
            mainViewModel.curPlayingSong.observe(this){
                if (it == null) return@observe
                curPlayingSong = it.toSong()
                glide.load(curPlayingSong?.imageUrl).into(binding.ivCurSongImage)
                switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
            }
        }
    }
}