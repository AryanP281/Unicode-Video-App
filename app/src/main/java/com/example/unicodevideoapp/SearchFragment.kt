package com.example.unicodevideoapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.InputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URL

class SearchFragment : Fragment()
{

    companion object
    {
        private val YOUTUBE_API_BASE_URL : String = "https://www.googleapis.com/youtube/v3/"
        private val YOUTUBE_API_KEY : String = "API-KEY"

        fun newInstance() : SearchFragment
        {
            return SearchFragment()
        }
    }

    private lateinit var youtubeApiInterface: YoutubeApiInterface //The interface to be used with retrofit for performing calls to YoutubeApi

    private inner class RecyclerAdapter(var videos : ArrayList<VideoItem>) : RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder>()
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
            holder.cardView.setTag(videos.get(position).id.videoId)

            //Setting click listener for the card
            holder.cardView.setOnClickListener(videoCardClickListener)
        }

        override fun getItemCount(): Int
        {
            return videos.size
        }

        fun update(newVideos : ArrayList<VideoItem>)
        {
            /**Updates the videos array used for populating the recycler view**/

            Log.d("vc", newVideos.size.toString())
            videos = newVideos
            notifyDataSetChanged()
        }

        fun append(newVideos : ArrayList<VideoItem>)
        {
            /**Adds the new videos to the videos list**/

            videos.addAll(newVideos) //Adds to new videos to the list of videos
            notifyDataSetChanged()
        }
    }

    class ThumbnailRetriever(var imageView: ImageView /*The image view to be used for displaying the downloaded thumbnail*/) : AsyncTask<String, Void, Bitmap>()
    {
        override fun doInBackground(vararg thumbnailUrl: String?): Bitmap?
        {
            var thumbnailBmp : Bitmap? = null //Bitmap for the thumbnail
            var inputStream : InputStream? = null //An input stream for the thumbnail data
            try {
                val thumbnailUrl : URL = URL(thumbnailUrl[0]) //The url to the thumbnail image
                inputStream = thumbnailUrl.openStream() //Opening a download stream to the url
                thumbnailBmp = BitmapFactory.decodeStream(inputStream) //Getting the image
            }
            catch(e : Exception)
            {
                Log.e("Thb error", e.message);
            }
            finally {
                inputStream?.close() //Closing the input stream
            }

            return thumbnailBmp
        }

        override fun onPostExecute(result: Bitmap?)
        {
            super.onPostExecute(result)

            if(result != null)
                imageView.setImageBitmap(result) //Displaying the thumbnail
        }
    }

    val listScrollListener : RecyclerView.OnScrollListener = object:RecyclerView.OnScrollListener()
    {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)
        {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager : LinearLayoutManager? = recyclerView.layoutManager as LinearLayoutManager
            val totalItems : Int = recyclerAdapter.itemCount
            if(totalItems > 0 && layoutManager != null && layoutManager.findLastVisibleItemPosition() > totalItems - 10 && !isFetchingMoreData)
            {
                isFetchingMoreData = true
                searchKeyword()
            }

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

    private lateinit var keyword : String //The entered search query
    private lateinit var recyclerAdapter: RecyclerAdapter
    private var nextPageToken : String? = null //The token for the next search result page
    private var isFetchingMoreData : Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_search, container, false)

        //Creating a retrofit instance
        val retrofit : Retrofit = Retrofit.Builder().let {
            it.baseUrl(YOUTUBE_API_BASE_URL)
            it.addConverterFactory(GsonConverterFactory.create())
            it.build()
        }

        //Initializing the retrofit youtube api interface
        youtubeApiInterface = retrofit.create(YoutubeApiInterface::class.java)

        //Initializing the recycler view
        recyclerAdapter = RecyclerAdapter(ArrayList<VideoItem>())
        val recyclerView : RecyclerView = fragmentView.findViewById<RecyclerView>(R.id.videos_list)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = recyclerAdapter
        recyclerView.addOnScrollListener(listScrollListener)

        //Setting click listener for the search button
        fragmentView.findViewById<ImageButton>(R.id.search_btn).setOnClickListener { view: View ->

            //Clearing the old data
            recyclerAdapter.update(ArrayList<VideoItem>())

            //Getting the entered search query
            nextPageToken = null //Clearing the page token from the last search
            keyword = this.view!!.findViewById<EditText>(R.id.video_search_bar).text.toString().trim()
            searchKeyword()
        }

        return fragmentView
    }

    private fun searchKeyword()
    {
        /**Uses the Youtube Api to search videos related to the keyword entered in the text box**/

        //Creating the request query map
        val queryMap : HashMap<String, String> = HashMap<String,String>()
        queryMap.put("part", "snippet")
        queryMap.put("q", keyword)
        queryMap.put("type", "video")
        queryMap.put("maxResults", "50")
        if(nextPageToken != null) queryMap.put("pageToken", nextPageToken!!)
        queryMap.put("fields", "items(id(videoId),snippet),nextPageToken")
        queryMap.put("key", YOUTUBE_API_KEY)

        //Initializing the api call
        val apiCall : Call<KeywordSearchResult> = youtubeApiInterface.getSearchResults(queryMap)

        //Executing the call in a separate thread
        apiCall.enqueue(object:Callback<KeywordSearchResult>
        {
            override fun onResponse(call: Call<KeywordSearchResult>, response: Response<KeywordSearchResult>)
            {
                //Checking if response was successful
                if(response.isSuccessful)
                {
                    val searchResult : KeywordSearchResult = response.body()!! //Getting the api result
                    nextPageToken = searchResult.nextPageToken //Setting the next page token

                    //Populating the recycler adapter
                    recyclerAdapter.append(searchResult.items as ArrayList<VideoItem>)
                }
                else
                    Log.e("Response code", response.code().toString())

                isFetchingMoreData = false
            }

            override fun onFailure(call: Call<KeywordSearchResult>, t: Throwable)
            {
                Log.e("api request failure", t.message, t)
                isFetchingMoreData = false
            }

        })

    }

}
