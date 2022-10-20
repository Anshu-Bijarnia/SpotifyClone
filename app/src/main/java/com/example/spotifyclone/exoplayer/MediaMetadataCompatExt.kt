package com.example.spotifyclone.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.example.spotifyclone.data.entities.Song

// Now we can call this extension function on every media metadata compat object to just convert it to a song
fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            it.mediaId ?: "",
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString()
        )
    }
}