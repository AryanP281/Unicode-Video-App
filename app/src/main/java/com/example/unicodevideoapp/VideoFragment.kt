package com.example.unicodevideoapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import androidx.appcompat.widget.Toolbar;


class VideoFragment : Fragment()
{
    companion object
    {
        private val EXT_STORAGE_PERM_CODE : Int = 1234

        fun newInstance() : VideoFragment
        {
            return VideoFragment()
        }
    }

    val toolbarClickListener : Toolbar.OnMenuItemClickListener = object:Toolbar.OnMenuItemClickListener
    {
        override fun onMenuItemClick(item: MenuItem?): Boolean
        {
            when(item?.itemId)
            {
                R.id.download_video -> manageStoragePermissions()
            }

            return true
        }
    } //The click listener for the utility toolbar

    private lateinit var exoplayer : SimpleExoPlayer //The exoplayer
    private lateinit var playerView : PlayerView //The player view
    val dataSourceFactory : DataSource.Factory by lazy {
        DefaultDataSourceFactory(context!!, "sample")
    }
    private var playbackPos : Long = 0 //The current position of the playback
    private var decodedUrl : String? = null //The decoded dash url

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_video, container, false)

        //Setting the video title
        fragmentView.findViewById<TextView>(R.id.video_title)?.text = (activity as HomeActivity).videoTitle

        //Getting the player view
        playerView = fragmentView.findViewById<PlayerView>(R.id.video_player)

        //Setting fullscreen button click listener
        playerView.findViewById<ImageView>(R.id.exoplayer_fullscreen_icon).setOnClickListener { v : View ->
            if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else
                activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        //Initializing the toolbar
        val toolbar : Toolbar? = fragmentView.findViewById<Toolbar>(R.id.video_toolbar) //Getting the toolbar
        toolbar?.inflateMenu(R.menu.video_toolbar_menu) //Inflating the toolbar menu
        toolbar?.setOnMenuItemClickListener(toolbarClickListener) //Setting the click listener

        //Getting state
        if(savedInstanceState != null) {
            //Setting playback position
            playbackPos = savedInstanceState.getLong("PLAYBACK_POS")

            //Getting the decoded url
            decodedUrl = savedInstanceState.getString("DECODED_URL")
        }

        return fragmentView
    }
    
    override fun onStart() {
        super.onStart()

        //Initializing exoplayer
        initializePlayer()
    }
    override fun onStop()
    {
        super.onStop();

        //Releasing the player
        if(this::exoplayer.isInitialized) exoplayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        //Saving the playback position
        outState.putLong("PLAYBACK_POS", exoplayer.currentPosition)

        //Saving the decoded url
        outState.putString("DECODED_URL", decodedUrl)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == EXT_STORAGE_PERM_CODE)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                downloadVideo()
            }
        }
    }

    fun initializePlayer()
    {
        /**Prepares the exoplayer to play the youtube video with the given id**/

        //Getting the player
        if(!this::exoplayer.isInitialized)
        {
            exoplayer = SimpleExoPlayer.Builder(activity as Context).build()
        }
        if(playerView.player == null) playerView.player = exoplayer

        exoplayer.playWhenReady = true

        if(decodedUrl == null)
        {
            //Extracting youtube url
            val youtubeExtractor = @SuppressLint("StaticFieldLeak")
            object:YouTubeExtractor(context!!)
            {
                    override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?)
                    {
                        if(ytFiles != null)
                        {
                            var iTag : Int = 0
                            for(i in 0..ytFiles.size())
                            {
                                iTag = ytFiles.keyAt(i)
                                if(ytFiles.get(iTag) != null)
                                {
                                    decodedUrl = ytFiles.get(iTag).url
                                    break
                                }
                            }
                            exoplayer.prepare(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(decodedUrl))) //Creating and setting the media source
                            exoplayer.seekTo(playbackPos)
                        }
                    }

                }

            //Building the data source
            val videoUrl : String = "https://www.youtube.com/watch?v=${(activity as HomeActivity).videoId}" //Creating the youtube url
            youtubeExtractor.extract(videoUrl,true,true) //Extracting usable url and preparing to play video
        }
        else
        {
            exoplayer.prepare(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(decodedUrl))) //Creating and setting the media source
            exoplayer.seekTo(playbackPos)
        }

    }

    private fun manageStoragePermissions()
    {
        if(ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                AlertDialog.Builder(context!!).let {
                    it.setTitle(R.string.storage_perm_rat_title)
                    it.setMessage(R.string.storage_perm_rat_msg)
                    it.setPositiveButton(R.string.accept_perm_btn_msg, DialogInterface.OnClickListener { dialog, which ->
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXT_STORAGE_PERM_CODE)
                    })
                }
            }
            else
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXT_STORAGE_PERM_CODE)
        }
        else
            downloadVideo()
    }

    private fun downloadVideo()
    {
        val videoDownloader : VideoDownloader = VideoDownloader(context!!, (activity as HomeActivity).videoTitle) //The downloader to be used for downloading the video

        //Initializing the url extractor
        if(decodedUrl == null)
        {
            val videoUrl : String = "https://www.youtube.com/watch?v=${(activity as HomeActivity).videoId}" //Creating the youtube url
            object : YouTubeExtractor(context!!) {
                override fun onExtractionComplete(
                    ytFiles: SparseArray<YtFile>?,
                    videoMeta: VideoMeta?
                ) {
                    if (ytFiles != null) {
                        var iTag: Int = 0
                        for (i in 0..ytFiles.size()) {
                            iTag = ytFiles.keyAt(i)
                            if (ytFiles.get(iTag) != null) {
                                videoDownloader.execute(ytFiles.get(iTag).url)
                                break
                            }
                        }
                    }
                }

            }.extract(videoUrl, true, true)
        }
        else
            videoDownloader.execute(decodedUrl)

    }

}