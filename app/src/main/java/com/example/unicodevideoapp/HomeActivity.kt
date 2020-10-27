package com.example.unicodevideoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.marginLeft
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeActivity : AppCompatActivity()
{
    private val VIDEO_ID_KEY : String = "V_I"

    val bottomNavClickListener : BottomNavigationView.OnNavigationItemSelectedListener = object:BottomNavigationView.OnNavigationItemSelectedListener
    {
        override fun onNavigationItemSelected(item: MenuItem): Boolean
        {
            when(item.itemId)
            {
                R.id.h_bnav_search -> displayFragment(SearchFragment(), false)
                R.id.h_bnav_watch -> displayFragment(VideoFragment(), false)
                R.id.h_bnav_profile -> displayFragment(UserProfileFragment(), false)
            }
            return true
        }

    }

    var videoId : String = "0" //The id of the video being
    var videoTitle : String = "" //The title of the video being played
    var fragId : Int = 0 //The currently displayed fragment
    private var bottomNavBar : BottomNavigationView? = null //The bottom navigation bar

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Initializing the bottom nav bar
        bottomNavBar = findViewById(R.id.home_bottom_navbar)
        if(bottomNavBar != null) {
            bottomNavBar!!.inflateMenu(R.menu.home_bottom_nav_menu) //Setting the nav bar menu
            bottomNavBar!!.setOnNavigationItemSelectedListener(bottomNavClickListener) //Setting the item selected listener
        }

        //Checking for saved state
        if(savedInstanceState != null)
        {
            //Getting the video id
            videoId = savedInstanceState.getString(VIDEO_ID_KEY)!!

            //Getting the video title
            videoTitle = savedInstanceState.getString("VIDEO_TITLE")!!

            //Getting the fragment id
            fragId = savedInstanceState.getInt("FRAG_ID")
        }
        else {
            //Displaying the default fragment
            bottomNavBar?.selectedItemId = R.id.h_bnav_search
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        //Saving the currently played video id
        outState.putString(VIDEO_ID_KEY, videoId)

        //Saving the video title
        outState.putString("VIDEO_TITLE", videoTitle)

        //Saving the current fragment
        outState.putInt("FRAG_ID", fragId)
    }

    private fun displayFragment(frag : Fragment, addToBackstack : Boolean)
    {
        //Updating the fragment id
        if(frag is SearchFragment) fragId = R.id.h_bnav_search
        else if (frag is VideoFragment) fragId = R.id.h_bnav_watch
        else fragId = R.id.h_bnav_profile

        //Starting the fragment change transaction
        val fragTrans : FragmentTransaction = supportFragmentManager.beginTransaction()
        fragTrans.replace(R.id.home_content_frame, frag)
        fragTrans.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        if(addToBackstack) fragTrans.addToBackStack(null)
        fragTrans.commit()
    }

    fun playVideo(videoId : String, videoTitle : String)
    {
        /**Plays the video with the given id, in video fragment*/

        //Setting the video id
        this.videoId = videoId
        this.videoTitle = videoTitle

        //Changing to video fragment
        bottomNavBar?.selectedItemId = R.id.h_bnav_watch
    }
}