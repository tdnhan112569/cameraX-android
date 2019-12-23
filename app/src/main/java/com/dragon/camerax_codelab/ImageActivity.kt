package com.dragon.camerax_codelab

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File


class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val extras = intent.getExtras();
        val string = extras?.get("url").toString() ?: ""
        if(string.isNotEmpty()) {
            val imgFile = File(string)
            if(imgFile.exists()) {
                val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath);
                cropImageView.setImageBitmap(myBitmap)
            }
        }
    }
}
