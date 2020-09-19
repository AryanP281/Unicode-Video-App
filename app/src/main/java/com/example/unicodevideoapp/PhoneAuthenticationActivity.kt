package com.example.unicodevideoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PhoneAuthenticationActivity : AppCompatActivity() {

    private val BUNDLE_VER_REQ_KEY: String = "VER_REQ"

    private lateinit var firebaseAuth : FirebaseAuth

    private var verificationRequested : Boolean = false //Tells whether the phone number was entered and verification was requested
    private lateinit var phoneVerId : String //The verification id received after the verification code has been sent

    private var phoneNumField : EditText? = null //The Edit Text used for entering the phone number
    private var verCodeField : EditText? = null //The Edit Text used for entering the verification code
    private var submitBtn : Button? = null //The submit button

    val phoneAuthCallback : PhoneAuthProvider.OnVerificationStateChangedCallbacks = object:PhoneAuthProvider.OnVerificationStateChangedCallbacks()
    {
        override fun onVerificationCompleted(credential: PhoneAuthCredential?)
        {
            signInWithPhone(credential!!)
        }

        override fun onVerificationFailed(exe: FirebaseException?)
        {
            if(exe is FirebaseAuthInvalidCredentialsException)
                Toast.makeText(this@PhoneAuthenticationActivity, exe.message, Toast.LENGTH_LONG).show()
            else
                Toast.makeText(this@PhoneAuthenticationActivity, "Phone Number Verification failed. Try again", Toast.LENGTH_LONG).show()
            verificationRequested = false
        }

        override fun onCodeSent(verificationId: String?, resendToken: PhoneAuthProvider.ForceResendingToken?)
        {
            phoneVerId = verificationId!! //Setting the verification id

            //Updating the UI
            updateFields()
        }
    } //The callback for the phone number verification

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_authentication)

        //Initializing the firebase auth object
        firebaseAuth = FirebaseAuth.getInstance()

        //Setting the verification request flag
        if(savedInstanceState != null)
            verificationRequested = savedInstanceState.getBoolean(BUNDLE_VER_REQ_KEY)

        //Initializing the fields
        updateFields()

        //Setting click listener for the submit button
        submitBtn!!.setOnClickListener {view : View? ->
            if(!verificationRequested)
                sendVerificationCode()
            else
                verifyUsingEnteredCode()
        }

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle)
    {
        super.onSaveInstanceState(outState, outPersistentState)

        outState.putBoolean(BUNDLE_VER_REQ_KEY, verificationRequested)
    }

    private fun updateFields()
    {
        /**Sets the editexts' state according to the state of the verification request**/

        if(phoneNumField == null) phoneNumField = findViewById(R.id.phoneauth_phone_num) as EditText
        if(verCodeField == null) verCodeField = findViewById(R.id.phoneauth_verification_code) as EditText
        if(submitBtn == null) submitBtn = findViewById(R.id.phonwauth_submit_btn) as Button

        if(verificationRequested)
        {
            phoneNumField!!.isEnabled = false
            verCodeField!!.isEnabled = true
            verCodeField!!.visibility = View.VISIBLE
            submitBtn!!.text = getString(R.string.phoneauth_submit_btn_txt2)
        }
        else
        {
            phoneNumField!!.isEnabled = true
            verCodeField!!.isEnabled = false
            verCodeField!!.visibility = View.INVISIBLE
            submitBtn!!.text = getString(R.string.phoneauth_submit_btn_txt1)
        }
    }

    private fun sendVerificationCode()
    {
        /**Verifies the enetered phone number and sends the verification code**/

        //Getting the phone num
        val phoneNum : String = phoneNumField!!.text.toString().trim()

        //Checking if phone number is empty
        if(phoneNum.length == 0)
        {
            Toast.makeText(this, "Phone Field is empty. Enter your phone number", Toast.LENGTH_SHORT).show()
            return
        }

        //Verifying phone number
        verificationRequested = true
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNum, 60, TimeUnit.SECONDS, this, phoneAuthCallback)
    }

    private fun signInWithPhone(credential: PhoneAuthCredential)
    {
        /**Logs user in firebase using the provided credential**/

        firebaseAuth.signInWithCredential(credential).let {
            it.addOnSuccessListener {res : AuthResult ->
                //Switching to home activity
                val intent : Intent = Intent(this, HomeActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            it.addOnFailureListener {exe : Exception ->
                //Showing error message
                Toast.makeText(this, "Unable to sign in - ${exe.message}", Toast.LENGTH_LONG).show()

                verificationRequested = false

                //Updating the ui
                updateFields()
            }
        }
    }

    private fun verifyUsingEnteredCode()
    {
        /**Performs phone auth using the entered verification code**/

        //Getting entered verification code
        val verCode : String = verCodeField!!.text.toString()

        //Creating the auth credential
        val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(phoneVerId, verCode)

        signInWithPhone(credential)
    }
}