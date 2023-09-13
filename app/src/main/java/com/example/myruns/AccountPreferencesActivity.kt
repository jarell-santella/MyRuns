package com.example.myruns

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import java.io.File

class AccountPreferencesActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var nameEditView: EditText
    private lateinit var emailEditView: EditText
    private lateinit var phoneEditView: EditText
    private lateinit var femaleRadioView: RadioButton
    private lateinit var maleRadioView: RadioButton
    private lateinit var classEditView: EditText
    private lateinit var majorEditView: EditText

    private lateinit var tempImgUri: Uri
    private lateinit var permImgUri: Uri
    private lateinit var cameraResult: ActivityResultLauncher<Intent>
    private lateinit var galleryResult: ActivityResultLauncher<Intent>

    private lateinit var profilePictureViewModel: ProfilePictureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_preferences)

        imageView = findViewById(R.id.profilePicture)
        nameEditView = findViewById(R.id.nameInput)
        emailEditView = findViewById(R.id.emailInput)
        phoneEditView = findViewById(R.id.phoneInput)
        femaleRadioView = findViewById(R.id.femaleSelect)
        maleRadioView = findViewById(R.id.maleSelect)
        classEditView = findViewById(R.id.classInput)
        majorEditView = findViewById(R.id.majorInput)

        profilePictureViewModel = ViewModelProvider(this).get(ProfilePictureViewModel::class.java)
        profilePictureViewModel.userImage.observe(this) { it ->
            imageView.setImageBitmap(it)
        }

        val tempImgFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getString(R.string.temp_img))
        tempImgUri = FileProvider.getUriForFile(this, "com.example.myruns", tempImgFile)

        cameraResult = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = Util.getBitmap(this, tempImgUri)
                profilePictureViewModel.userImage.value = bitmap
            }
        }

        galleryResult = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Util.writeBitmap(this, getString(R.string.temp_img), result.data?.data!!)
                val bitmap = Util.getBitmap(this, tempImgUri)
                profilePictureViewModel.userImage.value = bitmap
            }
        }

        loadLastSaved()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    private fun loadLastSaved() {
        val permImgFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getString(R.string.perm_img))
        permImgUri = FileProvider.getUriForFile(this, "com.example.myruns", permImgFile)
        if (permImgFile.exists()) {
            val bitmap = Util.getBitmap(this, permImgUri)
            imageView.setImageBitmap(bitmap)
        }
        else {
            imageView.setImageBitmap(null)
        }

        // Adapted from https://developer.android.com/reference/android/content/SharedPreferences
        val sharedPreferences = this.getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE)
        nameEditView.setText(sharedPreferences.getString(getString(R.string.name_key), ""))
        emailEditView.setText(sharedPreferences.getString(getString(R.string.email_key), ""))
        phoneEditView.setText(sharedPreferences.getString(getString(R.string.phone_key), ""))
        var radioValue: Int = sharedPreferences.getInt(getString(R.string.gender_key), -1)
        if (radioValue == 0) {
            femaleRadioView.setChecked(true)
            maleRadioView.setChecked(false)
        }
        else if (radioValue == 1) {
            maleRadioView.setChecked(true)
            femaleRadioView.setChecked(false)
        }
        else {
            maleRadioView.setChecked(false)
            femaleRadioView.setChecked(false)
        }
        var classValue = sharedPreferences.getInt(getString(R.string.class_key), 0)
        if (classValue == 0) {
            classEditView.setText("")
        }
        else {
            classEditView.setText(classValue.toString())
        }
        majorEditView.setText(sharedPreferences.getString(getString(R.string.major_key), ""))
    }

    fun onClickChangePhoto(view: View) {
        val options = arrayOf("Take from camera", "Select from gallery")
        val dialogBuilder = AlertDialog.Builder(this)
        var intent: Intent
        dialogBuilder.setTitle(R.string.change_photo_dialog)
        dialogBuilder.setItems(options) { _, index ->
            when (options[index]) {
                "Take from camera" -> {
                    intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImgUri)
                    cameraResult.launch(intent)
                }
                "Select from gallery" -> {
                    intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryResult.launch(intent)
                }
            }
        }

        dialogBuilder.show()
    }

    fun onClickCancel(view: View) {
        loadLastSaved()
        finish()
    }

    fun onClickSave(view: View) {
        val tempImgFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getString(R.string.temp_img))
        val permImgFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getString(R.string.perm_img))
        if (tempImgFile.exists()) {
            tempImgFile.copyTo(permImgFile, true)
            tempImgFile.delete()
        }

        // Adapted from https://developer.android.com/reference/android/content/SharedPreferences
        val sharedPreferences = this.getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE)
        with (sharedPreferences.edit()) {
            putString(getString(R.string.name_key), nameEditView.text.toString())
            putString(getString(R.string.email_key), emailEditView.text.toString())
            putString(getString(R.string.phone_key), phoneEditView.text.toString())
            when {
                femaleRadioView.isChecked -> putInt(getString(R.string.gender_key), 0)
                maleRadioView.isChecked -> putInt(getString(R.string.gender_key), 1)
                else -> putInt(getString(R.string.gender_key), -1)
            }
            if (classEditView.text.toString() == "") {
                putInt(getString(R.string.class_key), 0)
            }
            else {
                putInt(getString(R.string.class_key), classEditView.text.toString().toInt())
            }
            putString(getString(R.string.major_key), majorEditView.text.toString())
            apply()
        }

        finish()
    }
}