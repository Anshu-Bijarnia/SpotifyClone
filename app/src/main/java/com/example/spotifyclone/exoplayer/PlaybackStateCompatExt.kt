package com.example.spotifyclone.exoplayer

import android.support.v4.media.session.PlaybackStateCompat

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