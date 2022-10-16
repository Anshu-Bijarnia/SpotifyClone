package com.example.spotifyclone.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import com.example.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import com.example.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.example.spotifyclone.other.Constants.NETWORK_ERROR
import com.example.spotifyclone.other.Constants.SERVICE_TAG
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    @Inject
    lateinit var dataSourceFactory : DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer : ExoPlayer

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var mediaSessionConnector : MediaSessionConnector

    var isForegroundService = false

    // this refers to the currently playing song
    private var curPlayingSong : MediaMetadataCompat? = null

    private var isPlayerInitialized = false
    private lateinit var musicPlayerEventListener : MusicPlayerEventListener

    companion object {
        // This is a private set -> only change the value from within the service but we could read it outside of this service
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Loading the firebase music source -> this is done immediately after the service has been created.
        // We do this using coroutine scope (asynchronously)
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }
        mediaSession = MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){
            curSongDuration = exoPlayer.duration
        }
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong = it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true // In case we clicked on it, we want to directly play it
                )
        }
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)
        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator (mediaSession){
        // This will be called once when our service needs a new description from our media item (even when the song changes)
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }
    // We prepare the exoplayer here, it doesnt return any value
    private fun preparePlayer (
        songs: List<MediaMetadataCompat>,
        itemToPlay : MediaMetadataCompat?,
        playNow : Boolean
    ){
        // We find the current song index that we want to play, if it is null then we prepare the player for the first song
        // -> 0 else we will find the index of that song.
        val curSongIndex = if (curPlayingSong == null ) 0 else songs.indexOf(itemToPlay)
        exoPlayer.setMediaSource(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.prepare()
        // To make sure every song plays from the beginning, and seek our song to the curSongIndex
        exoPlayer.seekTo(curSongIndex,0L)
        exoPlayer.playWhenReady = playNow
    }
    // When the task of this service is removed(when the intent is removed), we stop the exoplayer from playing
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }
    // Here we will clean up the resources
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(musicPlayerEventListener) // To prevent memory leak
        exoPlayer.release() // Release the resources of exoplayer
    }
    // Our app is configured in such a way that its acts as browsable media objects (playlist,albums,recommended section)
    // We can also include the functionality of denying the request of certain clients, not implemented here
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID,null)
    }
    // Here we can check if the parentId is subscribed by the client and according to that we have the result list
    // If we have subscribed to a parentId then we need to return all the media inside that id in form of result
    override fun onLoadChildren(
        parentId: String, // We can check if this id is subscribed by the client (play)
        result: Result<MutableList<MediaBrowserCompat.MediaItem>> // This can be a playable song or a browsable (playlist,album etc)
    ) {
        when (parentId){ //  For extendability incase we add more ids.
            MEDIA_ROOT_ID -> {
                // Useful because this onLoad function might be called very early and we need to check if the music source is ready
                // and if it is ready then we know we have already sent the results, else we will tell that the result is not sent and the music source is not yet ready.
                val resultSent = firebaseMusicSource.whenReady { initialized ->
                    if (initialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                            isPlayerInitialized = true
                        }
                    }else {
                        // We caught a network error when we send a null result
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        result.sendResult(null)
                    }
                }
                // If the result is not sent, the detach will make this action happen later
                if (!resultSent){
                    result.detach()
                }
            }
        }
    }
}