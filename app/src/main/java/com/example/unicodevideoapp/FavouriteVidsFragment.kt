package com.example.unicodevideoapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class FavouriteVidsFragment : Fragment()
{
    companion object
    {
        fun newInstance() : FavouriteVidsFragment
        {
            return FavouriteVidsFragment()
        }
    }

    val videoCardClickListener : View.OnClickListener = object:View.OnClickListener
    {
        override fun onClick(v: View?)
        {
            //Playing the selected video
            val videoTitle : String = v!!.findViewById<RelativeLayout>(R.id.video_card_rel_layout).findViewById<TextView>(R.id.video_card_title).text.toString()
            (activity as HomeActivity).playVideo(v!!.getTag().toString(), videoTitle)
        }
    }

    private inner class RecyclerAdapter(var videos : ArrayList<FavouriteVideoItem>) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>()
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
            //Setting the card title
            holder.cardView.findViewById<TextView>(R.id.video_card_title).text = videos.get(position).snippet.title

            //Displaying the video thumbnail
            val imageView : ImageView = holder.cardView.findViewById<ImageView>(R.id.video_card_thumbnail)
            imageView.setImageResource(R.drawable.thumbnail_failure_icon) //Default icon
            ThumbnailRetriever(imageView).let {
                it.execute(videos.get(position).snippet.thumbnails.high.url)
            }

            //Setting the video id as tag on the card view
            holder.cardView.setTag(videos.get(position).id)

            //Setting click listener for the card
            holder.cardView.setOnClickListener(videoCardClickListener)
        }

        override fun getItemCount(): Int
        {
            return videos.size
        }

        fun update(newVideos : ArrayList<FavouriteVideoItem>)
        {
            /**Updates the videos array used for populating the recycler view**/

            Log.d("vc", newVideos.size.toString())
            videos = newVideos
            notifyDataSetChanged()
        }

        fun append(newVideos : ArrayList<FavouriteVideoItem>)
        {
            /**Adds the new videos to the videos list**/

            videos.addAll(newVideos) //Adds to new videos to the list of videos
            notifyDataSetChanged()
        }
    }

    private lateinit var firestoreDb : FirebaseFirestore //The firestore object
    private lateinit var userHandle : String //The user handle
    private lateinit var recyclerAdapter : RecyclerAdapter //Adapter for the recycler view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initializing firestore
        firestoreDb = Firebase.firestore

        //Getting the user handle
        val auth : FirebaseAuth = FirebaseAuth.getInstance()
        if(auth.currentUser!!.email == null || auth.currentUser!!.email!!.isBlank())
            userHandle = auth.currentUser!!.phoneNumber!!
        else
            userHandle = auth.currentUser!!.email!!

        //Initializing the recycler adapter
        recyclerAdapter = RecyclerAdapter(ArrayList<FavouriteVideoItem>())

        //Getting the list of favourite videos
        getFavVideos()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_favourite_vids, container, false)

        //Initializing the recycler view
        fragmentView.findViewById<RecyclerView>(R.id.fav_vids_list).let {
            it.layoutManager = LinearLayoutManager(context) //Setting the layout manager
            it.adapter = recyclerAdapter //Setting the adapter
        }

        return fragmentView
    }

    private fun getFavVideos()
    {
        //Creating the query to get all the favourite videos of the current user
        val query : Query = firestoreDb.collection("favs").whereEqualTo("user",userHandle)

        //Executing the query
        query.get().addOnCompleteListener { task : Task<QuerySnapshot> ->
            if(task.isSuccessful)
                retrieveFavVideosInfo(task.result!!.documents)
            else
                Toast.makeText(context, "Failed to retrieve favourites list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveFavVideosInfo(docs : List<DocumentSnapshot>)
    {
        /**Retrieves video items for each doc**/

        //Initializing retrofit
        val retrofit : Retrofit = Retrofit.Builder().let {
            it.baseUrl(SearchFragment.YOUTUBE_API_BASE_URL)
            it.addConverterFactory(GsonConverterFactory.create())
            it.build()
        }

        //Creating the YoutubeApiInterface object
        val youtubeApiInterface : YoutubeApiInterface = retrofit.create(YoutubeApiInterface::class.java)

        //Creating the request query map
        val queryMap : HashMap<String, String> = HashMap<String,String>()
        queryMap.put("part","snippet")
        //Adding the video ids to the query
        var videoIds : String = ""
        for(doc in docs)
        {
            videoIds += "${doc.data!!.get("videoId") as String},"
        }
        queryMap.put("id",videoIds)
        queryMap.put("fields","items(snippet,id)")
        queryMap.put("key",SearchFragment.YOUTUBE_API_KEY)

        //Performing api request
        youtubeApiInterface.getVideo(queryMap).enqueue(object:Callback<VideoSearchResult>
        {
            override fun onResponse(call: Call<VideoSearchResult>, response: Response<VideoSearchResult>)
            {
                //Checking if the response was successful
                if(response.isSuccessful)
                {
                    val videoItems : ArrayList<FavouriteVideoItem> = response.body()!!.items as ArrayList<FavouriteVideoItem> //Getting the video items list

                    //Updating the recycler adapter
                    recyclerAdapter.update(videoItems)
                }
                else
                    Log.e("FAVS_ERROR_CODE", response.code().toString())
            }

            override fun onFailure(call: Call<VideoSearchResult>, t: Throwable)
            {
                Log.e("FAVS_ERROR", t.message, t)
            }

        })

    }

}