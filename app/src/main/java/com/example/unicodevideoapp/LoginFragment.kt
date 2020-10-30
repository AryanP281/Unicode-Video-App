package com.example.unicodevideoapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class LoginFragment : Fragment()
{

    companion object
    {
        private val GOOGLE_SIGNIN_RC = 150

        @JvmStatic
        fun newInstance() : LoginFragment
        {
            return LoginFragment()
        }
    }

    private lateinit var firebaseAuth : FirebaseAuth //The firebase auth
    private var showingPassword : Boolean = false //Tells whether the show password button has ben clicked and the password is being shown
    private lateinit var firestoreDb : FirebaseFirestore //The firestore database

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_login, container, false)

        //Initializing the firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //Initializing the firestore database
        firestoreDb = Firebase.firestore

        //Setting the click listener for login button
        fragmentView.findViewById<Button>(R.id.login_btn).setOnClickListener {
            loginUser() //Logging the user in
        }

        //Setting click listener for the show password button
        val showPasswordBtn : ImageButton = fragmentView.findViewById<ImageButton>(R.id.login_show_password_btn)
        showPasswordBtn.setOnClickListener {view : View -> tooglePasswordVisibility(showPasswordBtn) }

        //Setting click listener for reset password button
        fragmentView.findViewById<TextView>(R.id.login_reset_password).setOnClickListener { view : View ->
            resetPassword()
        }

        //Setting click listener to sign in using google
        fragmentView.findViewById<Button>(R.id.login_via_google_btn).setOnClickListener {view : View? ->
            loginViaGoogle()
        }

        //Setting click listener to sign in using phone
        fragmentView.findViewById<Button>(R.id.login_via_phone_btn).setOnClickListener {view : View? ->
            //Starting the activity
            val intent : Intent = Intent(activity, PhoneAuthenticationActivity::class.java)
            startActivity(intent)
        }

        return fragmentView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        //Handling Google SignIn Activity result
        if(requestCode == GOOGLE_SIGNIN_RC)
        {
            //Checking if activity was successful
            if(resultCode == Activity.RESULT_OK)
            {
                val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                try
                {
                    //Getting the google account
                    val account : GoogleSignInAccount = task.getResult(ApiException::class.java)!!
                    firebaseAuthUsingGoogle(account.idToken!!)
                }
                catch (exe : ApiException)
                {
                    Toast.makeText(activity, "Google Sign In Failed - ${exe.message}", Toast.LENGTH_LONG).show()
                }
            }
            else
                Toast.makeText(activity, "Google Sign In Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser()
    {
        /**Logs the user using the entered credentials and firebase authentication system**/

        //Getting the entered credentials
        val email : String = view!!.findViewById<EditText>(R.id.login_email).text.toString().trim() //Getting the email
        val password : String = view!!.findViewById<EditText>(R.id.login_password).text.toString() //Getting the password

        //Checking if entered credentials are valid
        if(validCredentials(email, password))
        {
            //Logging the user
            firebaseAuth.signInWithEmailAndPassword(email, password).let{
                it.addOnSuccessListener{
                    //User was successfully logged in

                    //Showing the home activity
                    val intent : Intent = Intent(activity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }

                it.addOnFailureListener{exe : Exception ->
                    showLoginFailureMessage(exe) //Showing the error message
                }
            }
        }

    }

    private fun validCredentials(email : String, password : String) : Boolean
    {
        if(!checkEmailValidity(email))
            return false

        //Checking if password is blank
        if(password.length == 0)
        {
            Toast.makeText(activity, "Password Field Is Empty", Toast.LENGTH_SHORT).show() //Showing an error message
            return false
        }

        return true
    }

    private fun checkEmailValidity(email : String) : Boolean
    {
        /**Checks if the given email id is valid**/

        //Checking if the entered email is blank
        if(email.length == 0)
        {
            Toast.makeText(activity, "Email Field Is Empty", Toast.LENGTH_SHORT).show() //Showing an error message
            return false
        }

        //Checking if the email matches the email format
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            Toast.makeText(activity, "Email Is Invalid", Toast.LENGTH_SHORT).show() //Showing an error message
            return false;
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
            view!!.findViewById<EditText>(R.id.login_password).let {
                it.inputType = InputType.TYPE_CLASS_TEXT
                it.setSelection(it.text.length) //Moving the cursor to the end
            }

            showPasswordBtn.setImageResource(R.drawable.showing_pass_icon) //Changing the button image
            showingPassword = true
        }
        else
        {
            //Hiding the password
            view!!.findViewById<EditText>(R.id.login_password).let {
                it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                it.setSelection(it.text.length) //Moving the cursor to the end

                showPasswordBtn.setImageResource(R.drawable.hiding_pass_icon) //Changing the button image
                showingPassword = false
            }

        }
    }

    private fun showLoginFailureMessage(exe : Exception)
    {
        /***Shows a message explaining the reason for failure to log in**/

        var errorMsg : String = "Unable to log in. Try again !"

        if(exe is FirebaseAuthInvalidCredentialsException) //Entered password was wrong
            errorMsg = "Wrong Password. Try again or reset password"
        else if (exe is FirebaseAuthInvalidUserException)
            errorMsg = "User does not exist"

        Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
    }

    private fun resetPassword()
    {
        /**Resets the user's password**/

        //Getting the entered email address
        val emailAddr : String = view!!.findViewById<EditText>(R.id.login_email).text.toString()

        if(checkEmailValidity(emailAddr))
        {
            //Resetting the password
            firebaseAuth.sendPasswordResetEmail(emailAddr).let {
                it.addOnSuccessListener {
                    Toast.makeText(activity, "Password reset link sent to '$emailAddr'", Toast.LENGTH_LONG).show() //Showing success message
                }

                it.addOnFailureListener {exe : Exception ->
                    var errorMsg : String = "Unable to send password reset link. Try again"
                    if(exe is FirebaseAuthInvalidUserException)
                        errorMsg = "User does not exist"
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show() //Showing error message
                }
            }
        }

    }

    private fun loginViaGoogle()
    {
        /**Logs the user in using their google account**/

        //Initializing the google sign in options
        val gso : GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).let {
            it.requestIdToken(getString(R.string.default_web_client_id))
            it.requestEmail()
            it.build()
        }

        //Creating a google sign in client
        val googleSignInClient : GoogleSignInClient = GoogleSignIn.getClient(activity as Context, gso)

        //Signing in
        startActivityForResult(googleSignInClient.signInIntent, GOOGLE_SIGNIN_RC)
    }

    private fun firebaseAuthUsingGoogle(accountId : String)
    {
        /**Logs into firebase using the google account id**/

        //Getting the login credential from the google account id
        val credential : AuthCredential = GoogleAuthProvider.getCredential(accountId, null)

        //Logging into firebase
        firebaseAuth.signInWithCredential(credential).let {
            it.addOnCompleteListener { task : Task<AuthResult> ->
                if(task.isSuccessful)
                {
                    //Adding user to database
                    val newUser : User = User(firebaseAuth.currentUser!!.email.toString(), "", -1, -1, -1, -1)
                    firestoreDb.collection("users").document(newUser.handle).set(newUser).addOnCompleteListener {task : Task<Void> ->
                        //Showing error message
                        if(!task.isSuccessful)
                            Toast.makeText(context, "Failed to add user to database", Toast.LENGTH_SHORT).show()

                        //Switching to home activity
                        val intent : Intent = Intent(context, HomeActivity::class.java)
                        startActivity(intent)
                    }
                }
                else
                    Toast.makeText(activity, "Login Failed - ${task.exception!!.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}