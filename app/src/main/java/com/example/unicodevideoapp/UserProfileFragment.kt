package com.example.unicodevideoapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_user_profile.*
import java.util.*
import java.util.jar.Manifest


class UserProfileFragment : Fragment()
{
    companion object
    {
        val EXT_STORAGE_READ_PERM : Int = 9654
        val USE_GALARY_REQUEST_CODE : Int = 9655

        fun newInstance() : UserProfileFragment
        {
            return UserProfileFragment()
        }
    }

    private val CALENDAR_START_YEAR : Int = 1800

    private lateinit var firebaseAuth : FirebaseAuth //Firebase auth object
    private lateinit var firestoreDatabase : FirebaseFirestore //Firestore object
    private lateinit var years : Array<Int> //The years to be displayed in the birth date year spinner
    private lateinit var userHandle : String //The handle used by the user to sign in i.e email or phone number

    private var newDp : Boolean = false //Determines whether the dp is to be loaded from shared prefernces or a new dp is to be set from the galary
    private var profilePicUri : Uri? = null //The uri of the dp image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Generating years list
        generateYears()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_user_profile, container, false)

        //Initializing the Firebase auth object
        firebaseAuth = FirebaseAuth.getInstance()

        //Initializing the firestore object
        firestoreDatabase = Firebase.firestore

        //Setting click listener for logout button
        fragmentView.findViewById<Button>(R.id.logout_btn).setOnClickListener {view : View? ->
            firebaseAuth.signOut() //Signing out

            //Returning to logout page
            val intent : Intent = Intent(activity, AuthenticationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        //Setting click listener for save button
        fragmentView.findViewById<Button>(R.id.save_profile_btn).setOnClickListener { v : View ->
            saveUserDetails()
        }

        //Setting click listener for change profile pic btn
        fragmentView.findViewById<Button>(R.id.dp_change_btn).setOnClickListener { v : View ->
            newDp = true
            manageStoragePermission()
        }

        //Displaying the user details
        displayUserDetails(fragmentView)

        return fragmentView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == EXT_STORAGE_READ_PERM)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                setDp()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == USE_GALARY_REQUEST_CODE)
        {
            //Checking if uri was received
            if(resultCode == AppCompatActivity.RESULT_OK && data != null)
            {
                profilePicUri = data.data!!
                setDpFromURI(profilePicUri!!)
                newDp = false
            }
        }
    }

    private fun displayUserDetails(fragmentView : View)
    {
        /**Loads the user details from database and displays them*/

        //Setting the user handle
        if(firebaseAuth.currentUser!!.email == null || firebaseAuth.currentUser!!.email!!.isBlank())
            userHandle = firebaseAuth.currentUser!!.phoneNumber!!
        else
            userHandle = firebaseAuth.currentUser!!.email!!

        //Initializing the birth date selection spinners
        val bdaySpinner : Spinner = fragmentView.findViewById<Spinner>(R.id.user_bday)
        ArrayAdapter.createFromResource(context!!, R.array.days,R.layout.simple_spinner_item).let {
            it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            bdaySpinner.adapter = it
        }
        val bMonthSpinner : Spinner = fragmentView.findViewById<Spinner>(R.id.user_bmonth)
        ArrayAdapter.createFromResource(context!!,R.array.monthsOfYear,R.layout.simple_spinner_item).let {
            it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            bMonthSpinner.adapter = it
        }
        val bYearSpinner : Spinner = fragmentView.findViewById<Spinner>(R.id.user_bYear)
        ArrayAdapter<Int>(context!!, R.layout.simple_spinner_item, years).let {
            it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            bYearSpinner.adapter = it
        }

        //Initializing the country selection spinner
        val countrySpinner : Spinner = fragmentView.findViewById<Spinner>(R.id.user_country)
        ArrayAdapter.createFromResource(context!!, R.array.countries_array, R.layout.simple_spinner_item).let {
            it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            countrySpinner.adapter = it
        }

        //Retriving user info from database
        val docRef : DocumentReference = firestoreDatabase.collection("users").document(userHandle) //Reference to the users document
        docRef.get().addOnCompleteListener { task : Task<DocumentSnapshot> ->
            var user : User = User("","",-1,-1,-1,-1)

            //Checking if data was successfully retrived
            if(task.isSuccessful)
            {
                val userInfo : Map<String,Any?> = task.result!!.data!!

                user.displayName = userInfo.get("displayName").toString()
                user.handle = userInfo.get("handle").toString()
                if(userInfo.containsKey("bday")) user.bDay = userInfo.get("bday").toString().toInt()
                if(userInfo.containsKey("bmonth")) user.bMonth = userInfo.get("bmonth").toString().toInt()
                if(userInfo.containsKey("byear")) user.bYear = userInfo.get("byear").toString().toInt()
                if(userInfo.containsKey("country")) user.country = userInfo.get("country").toString().toInt()
            }

            //Displaying retrieved user info
            fragmentView.findViewById<EditText>(R.id.username_textbox).setText(user.displayName)
            fragmentView.findViewById<TextView>(R.id.user_handle).text = user.handle
            if(user.bDay != -1 && user.bMonth != -1 && user.bYear != -1)
            {
                bdaySpinner.setSelection(user.bDay - 1)
                bMonthSpinner.setSelection(user.bMonth - 1)
                bYearSpinner.setSelection(Calendar.getInstance().get(Calendar.YEAR) - user.bYear)
            }
            if(user.country != -1) countrySpinner.setSelection(user.country)

            //Displaying the profile pic
            setDp()
        }
    }

    private fun saveUserDetails()
    {
        /**Reads the entered user details and saves them to the database**/

        val user : User = User()

        //Getting the username
        user.displayName = view!!.findViewById<EditText>(R.id.username_textbox).text.toString()

        //Setting the email
        user.handle = userHandle

        //Getting the birthdate
        user.bDay = view!!.findViewById<Spinner>(R.id.user_bday).selectedItemPosition + 1
        user.bMonth = view!!.findViewById<Spinner>(R.id.user_bmonth).selectedItemPosition + 1
        user.bYear = Calendar.getInstance().get(Calendar.YEAR) - view!!.findViewById<Spinner>(R.id.user_bYear).selectedItemPosition

        //Getting the country
        user.country = view!!.findViewById<Spinner>(R.id.user_country).selectedItemPosition

        //Updating the database
        firestoreDatabase.collection("users").document(user.handle).set(user).addOnCompleteListener {task : Task<Void> ->
            //Checking if database was successfully updated
            if(task.isSuccessful)
                Toast.makeText(context!!, "Profile saved", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context!!, "Failed to save profile", Toast.LENGTH_SHORT).show()
        }

        //Saving the dp uri
        if(profilePicUri != null)
        {
           activity!!.getPreferences(Context.MODE_PRIVATE).let {
               with(it.edit())
               {
                   this.putString(userHandle, profilePicUri.toString())
                   this.apply()
               }
           }
        }

    }

    private fun generateYears()
    {
        val currentYear : Int = Calendar.getInstance().get(Calendar.YEAR) //Getting the current year

        //Adding years from 1800 to current year
        years = Array<Int>(currentYear - CALENDAR_START_YEAR + 1, {i -> currentYear-i})
    }

    private fun setDp()
    {
        /**Lets the user select a new dp from gallery and sets it as the profile pic**/

        if(newDp)
        {
            //Creating the intent to select image from gallery
            val intent: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            startActivityForResult(
                Intent.createChooser(intent, "Select profile picture"),
                USE_GALARY_REQUEST_CODE
            )
        }
        else
        {
            val uriString : String? = activity!!.getPreferences(Context.MODE_PRIVATE).getString(userHandle, null)
            if(uriString != null)
            {
                val picUri : Uri = Uri.parse(uriString)
                setDpFromURI(picUri)
            }
        }
    }

    private fun manageStoragePermission()
    {
        /**Checks if app has permission to read external storage. If not requests for permission**/

        if(ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                AlertDialog.Builder(context!!).let {
                    it.setTitle(R.string.storage_read_perm_rat_title)
                    it.setMessage(R.string.storage_read_perm_rat_msg)
                    it.setPositiveButton(
                        R.string.accept_perm_btn_msg,
                        DialogInterface.OnClickListener { dialog, which ->
                            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), EXT_STORAGE_READ_PERM) })
                }
            }
            else
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), EXT_STORAGE_READ_PERM)
        }
        else
            setDp()
    }

    private fun setDpFromURI(picUri : Uri)
    {
        //Checking if resource exists
        val cursor : Cursor? = context!!.contentResolver.query(picUri,null,null,null,null)
        if(cursor != null && cursor.moveToFirst())
            view!!.findViewById<ImageView>(R.id.user_dp).setImageURI(picUri)
    }
}