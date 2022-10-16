package com.example.spotifyclone.exoplayer.callbacks

import android.app.Service
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.spotifyclone.exoplayer.MusicService
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener

class MusicPlayerEventListener (
    private val musicService: MusicService
    ): Player.Listener{
    var PlayWhenReady = false;
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        PlayWhenReady = playWhenReady
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        // If the player is ready but we shouldn't play it automatically, we want to stop our foreground service
        if (playbackState == Player.STATE_READY && !PlayWhenReady){
            musicService.stopForeground(Service.STOP_FOREGROUND_DETACH)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService,"An unknown error occured",Toast.LENGTH_LONG).show()
    }
}