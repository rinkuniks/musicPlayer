package com.nikhil.musicplayer.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nikhil.musicplayer.R
import com.nikhil.musicplayer.adapters.SwipeSongAdapter
import com.nikhil.musicplayer.data.entities.Song
import com.nikhil.musicplayer.other.Status
import com.nikhil.musicplayer.exoplayer.isPlaying
import com.nikhil.musicplayer.exoplayer.toSong
import com.nikhil.musicplayer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide : RequestManager

    private var currPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObservers()

        vpSong.adapter = swipeSongAdapter

        vpSong.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (playbackState?.isPlaying == true){
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                }else{
                    currPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        ivPlayPause.setOnClickListener {
            currPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        swipeSongAdapter.setItemClickListener {
            navHostFragment.findNavController().navigate(
                R.id.globalActionToSongFragment
            )
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id){
                R.id.homeFragment -> showBottomBar()
                R.id.songFragment -> hideBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun hideBottomBar(){
        ivPlayPause.isVisible = false
        vpSong.isVisible = false
        ivCurSongImage.isVisible = false
    }

    private fun showBottomBar(){
        ivPlayPause.isVisible = true
        vpSong.isVisible = true
        ivCurSongImage.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song:Song){
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1){
            vpSong.currentItem = newItemIndex
            currPlayingSong = song
        }
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(this){
            it?.let { result->
                when(result.status){
                    Status.SUCCESS-> {
                        result.data?.let { songs->
                            swipeSongAdapter.songs = songs
                            if (songs.isNotEmpty()){
                                glide.load((currPlayingSong ?: songs[0].imageUrl)).into(ivCurSongImage)
                            }
                            switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
                        }
                    }
                    Status.ERROR-> Unit
                    Status.LOADING-> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(this){
            if (it == null) return@observe
            currPlayingSong = it.toSong()
            glide.load(currPlayingSong?.imageUrl).into(ivCurSongImage)
            switchViewPagerToCurrentSong(currPlayingSong ?: return@observe)
        }

        mainViewModel.playbackState.observe(this){
            playbackState = it
            ivPlayPause.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        mainViewModel.isConnected.observe(this){
            it?.getContentIfNotHandled()?.let { result->
                when(result.status){
                    Status.ERROR-> {
                        Snackbar.make(rootLayout, result.message ?: "Error occured",
                        Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this){
            it?.getContentIfNotHandled()?.let { result->
                when(result.status){
                    Status.ERROR-> {
                        Snackbar.make(rootLayout, result.message ?: "Error occured",
                            Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
    }
}
