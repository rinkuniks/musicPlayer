package com.nikhil.musicplayer.exoplayer.callbacks

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.nikhil.musicplayer.exoplayer.MusicService

class MusicPlayerEventListener(
    private val musicService: MusicService
) :Player.EventListener{

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "Error Occured", Toast.LENGTH_SHORT).show()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        if (playbackState == Player.STATE_READY && !playWhenReady){
            musicService.stopForeground(false)
        }
    }
}