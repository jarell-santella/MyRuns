package com.example.myruns

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfilePictureViewModel : ViewModel() {
    val userImage = MutableLiveData<Bitmap>()
}