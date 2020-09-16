package com.example.unicodevideoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeActivity : AppCompatActivity()
{
    private lateinit var firebaseAuth : FirebaseAuth //The firebase auth object

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Initializing the firebase auth object
        firebaseAuth = FirebaseAuth.getInstance()

        //Displaying logged user info
        displayUserInfo()

        //Adding listener for log out button
        findViewById<Button>(R.id.logout_btn).setOnClickListener{view : View ->
            firebaseAuth.signOut()

            //Switching to login activity
            val intent : Intent = Intent(this, AuthenticationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun displayUserInfo()
    {
        /**Displays the currently logged in user's info**/

        val currentUser : FirebaseUser? = firebaseAuth.currentUser
        if(currentUser != null)
        {
            findViewById<TextView>(R.id.user_email).text = currentUser.email
            findViewById<TextView>(R.id.user_id).text = currentUser.uid
            findViewById<TextView>(R.id.user_display_name).text = currentUser.displayName
        }
        else
        {
            //Switching to login activity
            val intent : Intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
        }

    }
}