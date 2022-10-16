package com.example.spotifyclone.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.exoplayer.MusicServiceConnection
import com.example.spotifyclone.exoplayer.isPlayEnabled
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.isPrepared
import com.example.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.example.spotifyclone.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// When we want to inject dependencies inside viewmodel using dagger hilt, we need to annotate it in a different way
// because viewmodel requires a constructor etc. Hilt will handle the viewmodel factory and all that
@HiltViewModel
class MainViewModel @Inject constructor(
private val musicServiceConnection: MusicServiceConnection // We will only implement one live data here, remaining we will get from
// music service connection, we will reference them here so that we can access them from our activity
) : ViewModel(){
    // Songs that will be later on displayed in recycler view
    private val _mediaItems = MutableLiveData<Resource<List<Song>>> ()
    val mediaItems : LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    // When the app loads, and the veiw model is created. we want to subscribe to our root id so that we can observe
    // on the media items that will return
    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items = children.map {
                    Song (
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )
                }
                _mediaItems.postValue(Resource.success(items))
            }
        })
    }
    // Functions to control our currently playing song, play/pause,skip,seek etc.
    fun skipToNextSong (){
        musicServiceConnection.transportControls.skipToNext()
    }
    fun skipToPreviousSong (){
        musicServiceConnection.transportControls.skipToPrevious()
    }
    fun seekTo(pos : Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }
    // This function will be called either when we want to play a song or toggle the current playing state
    // toggle is set to false -> by default we will not toggle the state, so that when we click on a new song we dont toggle the state and just play that song
    fun playOrToggleSong (mediaItem : Song, toggle : Boolean = false){
        val isPrepared = playbackState.value?.isPrepared ?: false // is null then we assume that the player is not prepared
        if (isPrepared && mediaItem.mediaId ==
            curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID))
        // the second part of the condition is how we get the mediaId of the currently playing song
        {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause() // if toggle is true we toggle the state from play to pause
                    // we pause the song and then play the same song again
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        }else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId,null)
        }
    }
    // For implementing unsubscribe to be called when our mainview model is destroyed
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,object : MediaBrowserCompat.SubscriptionCallback(){})
    }
}