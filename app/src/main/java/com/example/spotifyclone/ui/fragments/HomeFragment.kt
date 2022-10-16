package com.example.spotifyclone.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spotifyclone.R
import com.example.spotifyclone.adapters.SongAdapter
import com.example.spotifyclone.databinding.FragmentHomeBinding
import com.example.spotifyclone.other.Status
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment (R.layout.fragment_home) {
    lateinit var mainViewModel: MainViewModel // We don't initialise it here because we bind the main view model to the life cycle
    // of our activity and not to our fragment, we do it the other way
    lateinit var binding : FragmentHomeBinding
    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel :: class.java)
        setupRecyclerView()
        subscribeToObservers()
        songAdapter.setOnItemClickListener {
            mainViewModel.playOrToggleSong(it) // this will only play the song (if toggle is true and the same song which is playing is clicked that will pause the song)
        }
    }
    private fun setupRecyclerView () = binding.rvAllSongs.apply {
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
    // for subscribing to our observers from our view model, so that we are notified when these mediaItems are loaded
    private fun subscribeToObservers (){
        mainViewModel.mediaItems.observe(viewLifecycleOwner) { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    binding.allSongsProgressBar.isVisible = false
                    // Now set the songs list of our recycler view adapter to the data that is attached to this result object
                    result.data?.let { songs ->
                        songAdapter.songs = songs // this will trigger the songs list setter in songAdapter
                    }
                }
                Status.ERROR -> Unit //This can never happen
                Status.LOADING -> binding.allSongsProgressBar.isVisible = true
            }
        }
    }
}