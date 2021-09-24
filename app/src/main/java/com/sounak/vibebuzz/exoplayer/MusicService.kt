package com.sounak.vibebuzz.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.sounak.vibebuzz.exoplayer.callbacks.MusicPlayerNotificationListner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

import javax.inject.Inject


private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactoryFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer : SimpleExoPlayer

    private val serviceJob = Job()
    private val serviceScoped = CoroutineScope(Dispatchers.Main+serviceJob)

    private lateinit var musicNotificationManager: MusicNotificationManager


    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var memdiaSessionConnector: MediaSessionConnector


    var isForegroundService = false

    override fun onCreate() {
        super.onCreate()

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

       }

        memdiaSessionConnector = MediaSessionConnector(mediaSession)
        memdiaSessionConnector.setPlayer(exoPlayer)

        }

    override fun onDestroy() {
        super.onDestroy()
        serviceScoped.cancel()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

    }

}