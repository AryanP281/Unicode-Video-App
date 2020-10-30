package com.example.unicodevideoapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class RegistrationFragment : Fragment()
{

    companion object
    {
        @JvmStatic
        fun newInstance() : RegistrationFragment
        {
            return RegistrationFragment()
        }
    }

    private lateinit var firebaseAuth : FirebaseAuth //Used for user authorization with firebase
    private var showingPassword : Boolean = false //Tells whether the show password button has ben clicked and the password is being shown
    private lateinit var firestoreDb : FirebaseFirestore //The firebase firestore db object

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_registration, container, false)

        //Initializing the firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //Setting click listener for the register button
        fragmentView.findViewById<Button>(R.id.registration_register_btn).setOnClickListener { view : View ->
            registerUser() //Registering the user profile
        }

        //Setting click listener for the show password button
        val showPasswordBtn : ImageButton = fragmentView.findViewById<ImageButton>(R.id.registration_show_password_btn)
        showPasswordBtn.setOnClickListener {view : View -> tooglePasswordVisibility(showPasswordBtn) }

        //Initializing the database reference
        firestoreDb = Firebase.firestore

        return fragmentView
    }

    private fun registerUser()
    {
        /**Registers the user in firebase using the entered credentials**/

        //Getting the entered user info
        val email : String = view!!.findViewById<EditText>(R.id.registration_email).text.toString().trim() //Getting the entered email
        val password : String = view!!.findViewById<EditText>(R.id.registration_password).text.toString() //Getting the entered password
        val username : String = view!!.findViewById<EditText>(R.id.registration_username).text.toString() //Getting the entered username

        //Checking if the entered user credentials are valid
        if(validCredentials(email, password, username))
        {
            //Registering the user in firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password).let {
                it.addOnCompleteListener{task : Task<AuthResult> ->
                    //Checking if the user was successfully registered
                    if(task.isSuccessful)
                    {
                        //Adding user to database
                        val newUser : User = User(email, username,-1,-1,-1,-1) //Creating the new user

                        firestoreDb.collection("users").document(email).set(newUser).addOnCompleteListener { task : Task<Void> ->
                            //Checking if database write was successful
                            if(!task.isSuccessful)
                                Toast.makeText(activity, "Unable to save user details in database", Toast.LENGTH_SHORT).show()

                            //Switching to home activity
                            val intent : Intent = Intent(context, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }
                    else
                        Toast.makeText(activity, "Registration failed - ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validCredentials(email : String, password : String, username : String) : Boolean
    {
        //Checking if entered email is empty
        if (email.length == 0) {
            Toast.makeText(activity, "Email field cannot be empty", Toast.LENGTH_SHORT)
                .show() //Showing user the error message
            return false
        }

        //Checking if entered email is of valid form
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(activity, "Invalid Email", Toast.LENGTH_SHORT).show() //Showing user the error message
            return false
        }

        //Checking if the password is of the required strength
        val minPasswordSize : Int = resources.getInteger(R.integer.min_password_size)
        if (password.length < minPasswordSize)
        {
            Toast.makeText(activity, "Password size has to atleast be ${minPasswordSize}", Toast.LENGTH_SHORT).show() //Showing user the error message
            return false
        }

        //Checking if username is empty
        if(username.length == 0)
        {
            Toast.makeText(activity, "Username field cannot be empty", Toast.LENGTH_SHORT).show() //Showing error message
        }

        return true
    }

    private fun tooglePasswordVisibility(showPasswordBtn : ImageButton)
    {
        /**Toggles the password field visibility**/

        //Checking if the password is hidden
        if(!showingPassword)
        {
            //Displaying the password
            view!!.findViewById<EditText>(R.id.registration_password).let {
                it.inputType = InputType.TYPE_CLASS_TEXT
                it.setSelection(it.text.length) //Moving the cursor to the end
            }

            showPasswordBtn.setImageResource(R.drawable.showing_pass_icon) //Changing the button image
            showingPassword = true
        }
        else
        {
            //Hiding the password
            view!!.findViewById<EditText>(R.id.registration_password).let {
                it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                it.setSelection(it.text.length) //Moving the cursor to the end

                showPasswordBtn.setImageResource(R.drawable.hiding_pass_icon) //Changing the button image
                showingPassword = false
            }

        }
    }

}