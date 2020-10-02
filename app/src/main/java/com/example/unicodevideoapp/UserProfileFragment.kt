package com.example.unicodevideoapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth


class UserProfileFragment : Fragment()
{
    companion object
    {
        fun newInstance() : UserProfileFragment
        {
            return UserProfileFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_user_profile, container, false)

        //Setting click listener for logout button
        fragmentView.findViewById<Button>(R.id.logout_btn).setOnClickListener {view : View? ->
            FirebaseAuth.getInstance().signOut()

            //Returning to logout page
            val intent : Intent = Intent(activity, AuthenticationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return fragmentView
    }
}