package com.example.spotifyclone.exoplayer

import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING

// Some extension variables for PlaybackStateCompat class -> suggested by google

// if any of these states are true then we know that the player is prepared
inline val PlaybackStateCompat.isPrepared
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING ||
            state == PlaybackStateCompat.STATE_PAUSED

inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING

// Checking if the play option is enabled
inline val PlaybackStateCompat.isPlayEnabled
    get() = actions and PlaybackStateCompat.ACTION_PLAY != 0L ||
            (actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L && // Checking if the play paused option is enabled and we
                    state == PlaybackStateCompat.STATE_PAUSED) // are in the paused state then we know we can now play the song

inline val PlaybackStateCompat.currentPlaybackPosition: Long
    get() = if (state == STATE_PLAYING) {
        // We only get a specific position that was last updated in our player, exoplayer wont continously update the current playback position
        // The old and new values can be used to calculate the exact position of the song.
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else position // if the song is not playing, we return position (it is the value when the player was last updated -> 0 if position is not set)