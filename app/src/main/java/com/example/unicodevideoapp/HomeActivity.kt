package com.example.unicodevideoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.marginLeft
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeActivity : AppCompatActivity()
{

    val viewPagerAdapter : FragmentPagerAdapter = object:FragmentPagerAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
    {
        override fun getCount(): Int
        {
            return 3
        }

        override fun getItem(position: Int): Fragment
        {
            var fragment : Fragment? = null
            when(position)
            {
                0 -> fragment = SearchFragment()
                1 -> fragment = VideoFragment()
                2 -> fragment = UserProfileFragment()
            }

            return fragment!!
        }

        override fun getPageTitle(position: Int): CharSequence?
        {
            var title : String? = null
            when(position)
            {
                0 -> title = "Search"
                1 -> title = "Watch"
                2 -> title = FirebaseAuth.getInstance().currentUser!!.displayName
            }

            return title
        }

    } //The fragment adapter for the view pager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Initalizing pager title strip
        val pagerTab : PagerTabStrip = findViewById(R.id.home_pager_tab)

        //Initializing the view pager
        findViewById<ViewPager>(R.id.home_view_pager).adapter = viewPagerAdapter //Setting the view pager adapter
    }

}