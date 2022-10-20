package com.example.spotifyclone.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.spotifyclone.other.Constants.NETWORK_ERROR
import com.example.spotifyclone.other.Event
import com.example.spotifyclone.other.Resource

// We will use resource class(for error handling) and event class(prevention from some events firing multiple times) here
class MusicServiceConnection(
    context: Context
) {
// We will implement bunch of live data objects, those will be the objects that will contain information
// from our service, we will observe them later on to get information and updates for data changes

    // Made private livedata so that only this connection can change the value, A immutable live data will also be there
    // which will be public, which will be exposed to other classes, so that we can observe the changed in other classes
    // but we can't change it.

    // This is a livedata that contains the current state of this connection between activity and music service
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    // To check if there is a network error
    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    // Wether the current player is playing or paused.
    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    // Contains meta information about the currently playing song
    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong: LiveData<MediaMetadataCompat?> = _curPlayingSong

    // It will be used to get access to transport controls -> useful to pause the song,play,skip to next and register some callbacks
    lateinit var mediaController: MediaControllerCompat // We cannot yet instantiate this here, access to session token is needed
    // of our service and we only have access to that in a callback that we will define here in this service connection

    // This is mainly used for getting access to a session token and to subscribe and unsubscribe to our service (to our media Id)
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context, MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { // Done so that it actually connects, onConnected is triggered
        connect()
    }

    // That's why getter is set for transportControls, so that only when we try to access them the mediaController access is needed.
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    // These functions are used to subscribe and unsubscribe to a media id.
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    // This callback is for media browser
    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        // Once the music service connection is active this will be called
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            } // Once the connection is active, we have access to the session token
            // So we can initialise the media controller here.
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            _isConnected.postValue(Event(Resource.error("The connection was suspended", false)))
        }

        override fun onConnectionFailed() {
            _isConnected.postValue(
                Event(
                    Resource.error(
                        "Couldn't connect to media browser",
                        false
                    )
                )
            )
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        // Whenever our playback state is changed this function will be called and we post the new state
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        // Whenever our current song metadata changes, we want to post a new value to this currently playing song live data
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _curPlayingSong.postValue(metadata)
        }

        // It is used to send custom events from our service to this connection callback, used to notify network error
        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your internet connection.",
                            null
                        )
                    )
                )
            }

        }

        // Handling the error if our session is destroyed
        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}