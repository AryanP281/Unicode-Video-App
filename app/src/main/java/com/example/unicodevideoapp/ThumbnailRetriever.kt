package com.example.unicodevideoapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import java.io.InputStream
import java.lang.Exception
import java.net.URL

class ThumbnailRetriever(var imageView: ImageView /*The image view to be used for displaying the downloaded thumbnail*/) : AsyncTask<String, Void, Bitmap>()
{
    override fun doInBackground(vararg thumbnailUrl: String?): Bitmap?
    {
        var thumbnailBmp : Bitmap? = null //Bitmap for the thumbnail
        var inputStream : InputStream? = null //An input stream for the thumbnail data
        try {
            val thumbnailUrl : URL = URL(thumbnailUrl[0]) //The url to the thumbnail image
            inputStream = thumbnailUrl.openStream() //Opening a download stream to the url
            thumbnailBmp = BitmapFactory.decodeStream(inputStream) //Getting the image
        }
        catch(e : Exception)
        {
            Log.e("Thb error", e.message);
        }
        finally {
            inputStream?.close() //Closing the input stream
        }

        return thumbnailBmp
    }

    override fun onPostExecute(result: Bitmap?)
    {
        super.onPostExecute(result)

        if(result != null)
            imageView.setImageBitmap(result) //Displaying the thumbnail
    }
}