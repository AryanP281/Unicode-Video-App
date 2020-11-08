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
import android.opengl.Visibility
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
import androidx.core.view.isVisible
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


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

    private lateinit var exoplayer : SimpleExoPlayer //The exoplayer
    private lateinit var playerView : PlayerView //The player view
    val dataSourceFactory : DataSource.Factory by lazy {
        DefaultDataSourceFactory(context!!, "sample")
    }
    private var playbackPos : Long = 0 //The current position of the playback
    private var decodedUrl : String? = null //The decoded dash url

    private var userHandle : String? = null //The user handle
    private lateinit var firestoreDb : FirebaseFirestore //The firestore db object
    private var isFavourite : Boolean = false //Tells whether the current video has already been marked favourite
    private var favBtn : ImageButton? = null //The button used to mark video as favourite
    private lateinit var dbRecordId : String //The id of the video's record in the favourites database

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_video, container, false)

        //Initializing the firestore object
        firestoreDb = Firebase.firestore

        //Getting the user handle
        getUserHandle()

        //Setting the video title
        if(!(activity as HomeActivity).playDownloadedVideo)
            fragmentView.findViewById<TextView>(R.id.video_title)?.text = (activity as HomeActivity).videoTitle
        else
        {
            fragmentView.findViewById<TextView>(R.id.video_title)?.text = (activity as HomeActivity).downloadedVideo!!.title

            //Disabling the download button
            val downloadBtn : Button? = fragmentView.findViewById<Button>(R.id.download_video_btn)
            downloadBtn?.visibility = View.GONE
        }

        //Getting the player view
        playerView = fragmentView.findViewById<PlayerView>(R.id.video_player)

        //Setting fullscreen button click listener
        playerView.findViewById<ImageView>(R.id.exoplayer_fullscreen_icon).setOnClickListener { v : View ->
            if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else
                activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        //Setting click listener for download button
        fragmentView.findViewById<ImageButton>(R.id.download_video_btn)?.setOnClickListener { v : View ->
            if(!(activity as HomeActivity).playDownloadedVideo)
                manageStoragePermissions()
        }

        //Getting state
        if(savedInstanceState != null) {
            //Setting playback position
            playbackPos = savedInstanceState.getLong("PLAYBACK_POS")

            //Getting the decoded url
            decodedUrl = savedInstanceState.getString("DECODED_URL")

            //Getting if video has already been marked favourite
            isFavourite = savedInstanceState.getBoolean("FAV")
            updateFavBtnUI(fragmentView) //Updating the button icon
        }
        else
        {
            //Checking if the video has been marked favourite
            checkIfFavourite(fragmentView)
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

        //Saving the fav status
        outState.putBoolean("FAV", isFavourite)
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

        //Determining the playback mode
        if((activity as HomeActivity).playDownloadedVideo)
            playDownloadedVideo()
        else
            playYoutubeVideo()
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

    private fun playYoutubeVideo()
    {
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

    private fun playDownloadedVideo()
    {
        //Initializing the media source
        val mediaSource : MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource((activity as HomeActivity).downloadedVideo!!.uri)
        exoplayer.prepare(mediaSource)
        exoplayer.seekTo(playbackPos)
    }

    private fun downloadVideo()
    {

        //Getting the user handle
        var userHandle : String = ""
        val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser!!.email!!.isBlank())
            userHandle = firebaseAuth.currentUser!!.phoneNumber!!
        else
            userHandle = firebaseAuth.currentUser!!.email!!

        val videoDownloader : VideoDownloader = VideoDownloader(context!!, (activity as HomeActivity).videoTitle, userHandle) //The downloader to be used for downloading the video

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

        //Showing download start message
        Toast.makeText(context, "Downloaded started", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFavourite()
    {
        //Getting the user handle
        if(userHandle == null) getUserHandle()

        if(!isFavourite)
            markAsFavourite()
        else
            removeFromFavourites()
    }

    private fun markAsFavourite()
    {
        /**Adds the current video to the database**/

        //Initializing the data to be added
        val data: HashMap<String, String?> =
            hashMapOf("user" to userHandle, "videoId" to (activity as HomeActivity).videoId)

        //Adding video to db
        firestoreDb.collection("favs").add(data)
            .addOnCompleteListener { task: Task<DocumentReference> ->
                if (task.isSuccessful)
                {
                    //Setting the flag
                    isFavourite = true

                    //Saving the document id
                    dbRecordId = task.result!!.id

                    //Updating the ui
                    updateFavBtnUI(view!!)

                } else
                    Toast.makeText(context, "Failed to mark favourite", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFromFavourites()
    {
        /**Removes the current video from the database**/

        firestoreDb.collection("favs").document(dbRecordId).delete().addOnCompleteListener { task : Task<Void> ->
            if(task.isSuccessful)
            {
                //Setting the flag
                isFavourite = false

                //Updating the ui
                updateFavBtnUI(view!!)
            }
            else
                Toast.makeText(context, "Failed to remove from favourite", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserHandle()
    {
        val auth : FirebaseAuth = FirebaseAuth.getInstance()
        if(auth.currentUser!!.email == null || auth.currentUser!!.email!!.isBlank())
            userHandle = auth.currentUser!!.phoneNumber
        else
            userHandle = auth.currentUser!!.email
    }

    private fun checkIfFavourite(fragmentView : View)
    {
        /**Checks if the current video has already been marked as favourite**/

        //Creating the query
        val query : Query = firestoreDb.collection("favs").whereEqualTo("user",userHandle).whereEqualTo("videoId", (activity as HomeActivity).videoId)

        query.get().addOnCompleteListener { task :Task<QuerySnapshot> ->
                if(task.isSuccessful)
                {

                    //Checking if doc exists i.e video is already marked favourite
                    if (!task.result!!.isEmpty)
                    {
                        isFavourite = true //Setting the flag

                        //Saving the document id
                        dbRecordId = task.result!!.documents.get(0).id
                    }

                    //Setting the click listener
                    updateFavBtnUI(fragmentView)
                    favBtn!!.setOnClickListener { v : View ->
                        toggleFavourite()
                    }
                }
        }
    }

    private fun updateFavBtnUI(view : View)
    {
        /**Updates the icon for the favourites button**/

        //Initializing the button
        if(favBtn == null)
            favBtn = view.findViewById(R.id.favourite_video_btn)

        if(favBtn == null) return

        if(isFavourite)
            favBtn!!.setImageResource(R.drawable.fav_icon)
        else
            favBtn!!.setImageResource(R.drawable.not_fav_icon)
    }
}