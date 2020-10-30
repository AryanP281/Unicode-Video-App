package com.example.unicodevideoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : AppCompatActivity()
{

    class ViewPageAdapter(fm : FragmentManager) : FragmentPagerAdapter(fm)
    {

        override fun getCount() : Int
        {
            return 2;
        }

        override fun getItem(position: Int): Fragment
        {
            if(position == 0)
                return LoginFragment()
            return RegistrationFragment()
        }
    }

    private lateinit var pagerAdapter : ViewPageAdapter //The adapter used for the view pager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        //Switching to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        //Initializing the View Pager
        pagerAdapter = ViewPageAdapter(supportFragmentManager)
        findViewById<ViewPager>(R.id.auth_viewpager).adapter = pagerAdapter
    }

    override fun onStart()
    {
        super.onStart()

        //Checking if a user is already logged in
        val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null)
        {
            //Switching to home activity
            val intent : Intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
    }
}