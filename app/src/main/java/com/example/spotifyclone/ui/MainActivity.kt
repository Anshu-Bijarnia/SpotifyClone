package com.example.spotifyclone.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.R
import com.example.spotifyclone.adapters.SwipeSongAdapter
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.databinding.ActivityMainBinding
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.other.Status.*
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    // Because we want to bind this viewModel to the lifecycle of our activity we initialise it like this
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter //Recycler view adapter for our viewpager, so we can swipe through our songs

    @Inject
    lateinit var glide: RequestManager

    private var curPlayingSong: Song? = null

    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToObservers()
        binding.vpSong.adapter = swipeSongAdapter
        // To detect if we have actually swiped the item in our view pager, we add a onPageChangedCallback to our viewpager
        binding.vpSong.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            // this is called every time we swipe in our viewpager or we change the current item in our viewpager
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // In this case we just want to play the position we have swiped to
                if (playbackState?.isPlaying == true) {
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                } else {
                    curPlayingSong = swipeSongAdapter.songs[position]
                    glide.load(curPlayingSong?.imageUrl).into(binding.ivCurSongImage)
                }
            }
        })
        binding.ivPlayPause.setOnClickListener {
            curPlayingSong?.let {
                // Toggle is set to true because we always want to toggle the song when ever we click on the play pause image
                mainViewModel.playOrToggleSong(it, true)
            }
        }
        // When we click on the viewpager in the bottom bar we use the global action to switch to song fragment.
        swipeSongAdapter.setOnItemClickListener {
            Navigation.findNavController(this, R.id.navHostFragment)
                .navigate(R.id.globalActionToSongFragment)
        }
        // We will use the destination id of the nav host fragment to check if we should show the bottom bar or we should hide it.
        Navigation.findNavController(this, R.id.navHostFragment)
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.songFragment -> hideBottomBar()
                    R.id.homeFragment -> showBottomBar()
                    else -> showBottomBar()
                }
            }
    }

    private fun hideBottomBar() {
        binding.apply {
            ivCurSongImage.isVisible = false
            vpSong.isVisible = false
            ivPlayPause.isVisible = false
        }
    }

    private fun showBottomBar() {
        binding.apply {
            ivCurSongImage.isVisible = true
            vpSong.isVisible = true
            ivPlayPause.isVisible = true
        }
    }

    // Function to switch view pager to the current song -> when a new song plays, our vp automatically swipes to the corresponding song
    private fun switchViewPagerToCurrentSong(song: Song) {
        // Check at which index our current song is, return -1 if this song doesn't exist in the list
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1) {
            binding.vpSong.currentItem =
                newItemIndex //index of the current item that is displayed in that vp
            curPlayingSong = song
        }
    }

    // function to subscribe to our observers
    // observe them so that we can fill our vp with these items, and display the right item when we launch our app
    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    //
                    SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
                            if (songs.isNotEmpty()) {
                                glide.load((curPlayingSong ?: songs[0]).imageUrl)
                                    .into(binding.ivCurSongImage)
                            }
                            // if curplaying song is null we return out of this observe block, we just display the first song by default
                            switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
                        }
                    }
                    ERROR -> Unit // These media items don't even emit error, because we have already have that in home fragment
                    LOADING -> Unit

                }
            }
        }
        // Every time we get new information about the currently playing song (if the song switches) then this observer here will trigger
        // Here we will update the currently playing song and also switch the viewpager accordingly
        mainViewModel.curPlayingSong.observe(this) {
            if (it == null) return@observe
            curPlayingSong = it.toSong()
            glide.load(curPlayingSong?.imageUrl).into(binding.ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
        }
        // This will be called everytime the playbackstate is changed -> pause/play the song everytime the player is prepared
        mainViewModel.playbackState.observe(this) {
            playbackState = it
            binding.ivPlayPause.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
        // So that we don't show the snack bar in the error case twice, when rotating screen these observers will fire off again because the viewmodel
        // will be reloaded, but since it is wrapped around the event object this wont happen
        mainViewModel.isConnected.observe(this) {
            // First time this is called it will return boolean, but after that it will return a null and that is handled here.
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    ERROR -> Snackbar.make(
                        binding.root,
                        result.message
                            ?: "An unknown error occurred", // This should never happen because we only emit this error status for is connected once and then we just pass the message
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }

        }
        mainViewModel.networkError.observe(this) {
            // First time this is called it will return boolean, but after that it will return a null and that is handled here.
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    ERROR -> Snackbar.make(
                        binding.root,
                        result.message
                            ?: "An unknown error occurred", // This should never happen because we only emit this error status for is connected once and then we just pass the message
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
}
