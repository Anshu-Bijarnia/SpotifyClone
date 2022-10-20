package com.example.spotifyclone.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.R
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.databinding.FragmentSongBinding
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.other.Status
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import com.example.spotifyclone.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {
    lateinit var binding: FragmentSongBinding

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var curPlayingSong: Song? = null

    private var playbackState: PlaybackStateCompat? = null

    // Boolean check to see if we should update the seekbar
    private var shouldUpdateSeekbar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSongBinding.bind(view)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()
        binding.apply {
            // Setting up the functionality of play pause icon
            ivPlayPauseDetail.setOnClickListener {
                curPlayingSong?.let {
                    mainViewModel.playOrToggleSong(it, true)
                }
            }
            // This will be called when the seekbar is touched and dragged
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    // As long as the user is dragging we update the current time of the song and set it to the text view
                    if (fromUser) {
                        setCurPlayingTimeToTextView(progress.toLong())
                    }
                }

                // When we start touching the seekbar -> we should stop updating the seekbar
                override fun onStartTrackingTouch(p0: SeekBar?) {
                    shouldUpdateSeekbar = false
                }

                // After we leave the seekbar we should seek the song to that position, and start updating the seekbar
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    seekBar?.let {
                        mainViewModel.seekTo(it.progress.toLong())
                        shouldUpdateSeekbar = true
                    }
                }
            })
            // Implementation of back icon on the song fragment
            ivSkipPrevious.setOnClickListener {
                mainViewModel.skipToPreviousSong()
            }
            // Implementation of next icon on the song fragment
            ivSkip.setOnClickListener {
                mainViewModel.skipToNextSong()
            }

        }
    }

    // This function will be used to update the title and image of the song
    private fun updateTitleAndSongImage(song: Song) {
        val title = "${song.title} - ${song.subtitle}"
        binding.apply {
            tvSongName.text = title
            glide.load(song.imageUrl).into(ivSongImage)
        }
    }

    private fun subscribeToObservers() {
        // we observe the mediaItems and update the data in case of success case
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            //When we just launch the app and the song list is not empty, then we set the first song details to the song fragment
                            if (curPlayingSong == null && songs.isNotEmpty()) {
                                curPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
        // When the currently playing song changes we want to update its details
        mainViewModel.curPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            curPlayingSong = it.toSong()
            updateTitleAndSongImage(curPlayingSong!!)
        }
        // To check if the playback state changed -> paused or played from the notification
        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            binding.ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            // update the seekbar when the playback state changes -> set it to position, if progress is null set it to 0
            binding.seekBar.progress = it?.position?.toInt() ?: 0
        }
        // When the user seek by using the seekbar, during the drag we shouldn't update the seekbar
        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            // Only update the seekbar if we are allowed to, also we want to update our current position text view
            if (shouldUpdateSeekbar) {
                binding.seekBar.progress = it.toInt()
                setCurPlayingTimeToTextView(it)
            }
        }
        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            binding.seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            binding.tvSongDuration.text = dateFormat.format(it)
        }
    }

    private fun setCurPlayingTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.tvCurTime.text =
            dateFormat.format(ms) // The time will be formatted in the pattern given above -> mm:ss
    }
}