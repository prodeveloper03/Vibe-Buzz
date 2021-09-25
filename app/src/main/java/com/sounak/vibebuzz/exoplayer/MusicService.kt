package com.sounak.vibebuzz.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.media.session.MediaSession
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.sounak.vibebuzz.data.entities.Song
import com.sounak.vibebuzz.exoplayer.callbacks.MusicPlaybackPreparer
import com.sounak.vibebuzz.exoplayer.callbacks.MusicPlayerEventListner
import com.sounak.vibebuzz.exoplayer.callbacks.MusicPlayerNotificationListner
import com.sounak.vibebuzz.other.Constants.MEDIA_ROOT_ID
import com.sounak.vibebuzz.other.Constants.NETWORK_ERROR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

import javax.inject.Inject


private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactoryFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer : SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob = Job()
    private val serviceScoped = CoroutineScope(Dispatchers.Main+serviceJob)

    private lateinit var musicNotificationManager: MusicNotificationManager


    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var memdiaSessionConnector: MediaSessionConnector

    private lateinit var musicPlayerEventListner: MusicPlayerEventListner


    var isForegroundService = false

    private var currentPlayingSong:MediaMetadataCompat? = null

    private var isPlayerintialized = false


    companion object{
       var currSongDuration = 0L
        private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScoped.launch {
            firebaseMusicSource.fetchMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {

            setSessionActivity(activityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

       musicNotificationManager = MusicNotificationManager(
           this,
           mediaSession.sessionToken,
           MusicPlayerNotificationListner(this)
       ){
            currSongDuration = exoPlayer.duration
       }

        val musiPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            currentPlayingSong = it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }

        memdiaSessionConnector = MediaSessionConnector(mediaSession)
        memdiaSessionConnector.setPlaybackPreparer(musiPlaybackPreparer)
        memdiaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        memdiaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListner = MusicPlayerEventListner(this)
        exoPlayer.addListener(musicPlayerEventListner)
        musicNotificationManager.showNotification(exoPlayer)

        }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession){

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }


    private fun preparePlayer (

        song: List<MediaMetadataCompat>,
        itemToPlay :MediaMetadataCompat?,
        playNow: Boolean
        ){
        val currentSongIndex = if(currentPlayingSong==null) 0 else song.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactoryFactory))
        exoPlayer.seekTo(currentSongIndex,0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScoped.cancel()
        exoPlayer.removeListener(musicPlayerEventListner)
        exoPlayer.release()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID,null)

    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
            when(parentId){
                MEDIA_ROOT_ID-> {
                    val resultsSent = firebaseMusicSource.whenReady {isInitialized->
                        if(isInitialized){
                            result.sendResult(firebaseMusicSource.asMediaItems())
                            if(!isPlayerintialized&& firebaseMusicSource.songs.isNotEmpty()){
                                preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                                isPlayerintialized =true

                            }
                        }else{
                                 mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                                 result.sendResult(null)
                        }

                    }
                    if(!resultsSent){
                        result.detach()
                    }
                }
            }
    }

}