package com.example.unicodevideoapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.io.File


class DownloadedVidsFragment : Fragment()
{
    private inner class RecyclerAdapter(var videos : ArrayList<DownloadedVideo>) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>()
    {
        inner class RecyclerViewHolder(val cardView : CardView) : RecyclerView.ViewHolder(cardView) //The view holder for the recycler view

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder
        {
            //Creating a new view
            val cardView : CardView = LayoutInflater.from(parent.context).inflate(R.layout.video_card, parent, false) as CardView

            return RecyclerViewHolder(cardView) //Adding the card view to the view holder and returning it
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int)
        {
            //Setting the title
            holder.cardView.findViewById<TextView>(R.id.video_card_title).text = videos.get(position).title

            //Setting the thumbnail
            val bmp : Bitmap = ThumbnailUtils.createVideoThumbnail(videos.get(position).uri.toFile() ,Size(dpToPx(200.0f).toInt(), dpToPx(200.0f).toInt()), null)
            holder.cardView.findViewById<ImageView>(R.id.video_card_thumbnail).setImageBitmap(bmp)

            //Setting the click listener
            holder.cardView.setOnClickListener { v:View ->
                (activity as HomeActivity).playDownloadedVideo(videos.get(position))
            }
        }

        override fun getItemCount(): Int
        {
            return videos.size
        }

        fun update(newVideos : ArrayList<DownloadedVideo>)
        {
            /**Updates the videos array used for populating the recycler view**/

            Log.d("vc", newVideos.size.toString())
            videos = newVideos
            notifyDataSetChanged()
        }

        fun append(newVideos : ArrayList<DownloadedVideo>)
        {
            /**Adds the new videos to the videos list**/

            videos.addAll(newVideos) //Adds to new videos to the list of videos
            notifyDataSetChanged()
        }
    }

    private lateinit var recyclerAdapter : RecyclerAdapter //The adapter for the videos recycler view

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        //Initializing the adapter
        recyclerAdapter = RecyclerAdapter(ArrayList<DownloadedVideo>())
        manageStoragePermission()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_downloaded_vids, container, false)

        //Initializing the recycler view
        val recyclerView : RecyclerView = fragmentView.findViewById<RecyclerView>(R.id.downloaded_vids_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = recyclerAdapter

        return fragmentView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == UserProfileFragment.EXT_STORAGE_READ_PERM)
        {
            if(grantResults.get(0) == PackageManager.PERMISSION_GRANTED)
                getDownloadedVideos()
        }
    }

    private fun getDownloadedVideos()
    {
        /**Reads the list of downloaded videos and populates the recycler adapter**/

        var userHandle : String = ""
        val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser!!.email == null || firebaseAuth.currentUser!!.email!!.isBlank())
            userHandle = firebaseAuth.currentUser!!.phoneNumber!!
        else
            userHandle = firebaseAuth.currentUser!!.email!!

        val downloadsFolder : File? = context!!.getExternalFilesDir(userHandle) //Getting the downloads folder
        if(downloadsFolder != null)
        {
            val files : Array<File> = downloadsFolder.listFiles() //Getting the files in the downloads folder
            val downloadedVideos : ArrayList<DownloadedVideo> = ArrayList()
            for(file in files)
            {
                downloadedVideos.add(DownloadedVideo(file.name, Uri.fromFile(file)))
            }

            //Initializing the recycler adapter
            recyclerAdapter.update(downloadedVideos)
        }

    }

    private fun manageStoragePermission()
    {
        /**Checks if app has permission to read external storage. If not requests for permission**/

        if(ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                AlertDialog.Builder(context!!).let {
                    it.setTitle(R.string.storage_read_perm_rat_title)
                    it.setMessage(R.string.downloaded_vids_perm_rat_msg)
                    it.setPositiveButton(
                        R.string.accept_perm_btn_msg,
                        DialogInterface.OnClickListener { dialog, which ->
                            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                                UserProfileFragment.EXT_STORAGE_READ_PERM
                            ) })
                }
            }
            else
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    UserProfileFragment.EXT_STORAGE_READ_PERM
                )
        }
        else
            getDownloadedVideos()
    }

    private fun pxToDp(px : Float) : Float
    {
        return px / context!!.resources.displayMetrics.density
    }

    private fun dpToPx(dp : Float) : Float
    {
        return dp * context!!.resources.displayMetrics.density
    }

}