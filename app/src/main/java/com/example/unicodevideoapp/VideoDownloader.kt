package com.example.unicodevideoapp

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import kotlin.math.max

class VideoDownloader(val context : Context, val videoTitle : String, val userHandle : String) : AsyncTask<String,Void,Void?>()
{
    private var downloadSuccessful : Boolean = true //Tells whether the download was successful

    override fun doInBackground(vararg params: String?): Void?
    {
        var input : InputStream? = null
        var output : OutputStream? = null
        var httpConn : HttpURLConnection? = null
        var videoFile : File? = null


        try {
            val url : URL = URL(params[0])
            httpConn = url.openConnection() as HttpURLConnection
            httpConn.connect()

            if(httpConn.responseCode != HttpURLConnection.HTTP_OK)
            {
                Log.e("DOWNLOAD ERROR", httpConn.responseCode.toString() + " " + httpConn.responseMessage)
                return null
            }

            val fileLength : Long = httpConn.contentLength.toLong()

            input = httpConn.inputStream
            videoFile = File(context.getExternalFilesDir(userHandle), "$videoTitle.mp4")
            output = FileOutputStream(videoFile)

            val data : ByteArray = ByteArray(4096)
            var count : Int  = input.read(data)
            var downloadedSize : Long = 0
            while(count != -1)
            {
                output.write(data, 0, count)
                downloadedSize += count
                Log.e("Progress", (downloadedSize * 100 / fileLength).toString())
                count = input.read(data)
            }
        }
        catch(e : Exception)
        {
            Log.e("DOWNLOAD ERROR", e.message)
            downloadSuccessful = false
        }
        finally {
            try {
                output?.close()
                input?.close()
                httpConn?.disconnect()
            }
            catch(e : Exception){}
        }

        return null
    }

    override fun onPostExecute(result: Void?)
    {
        super.onPostExecute(result)

        //Showing success message
        if(downloadSuccessful)
            Toast.makeText(context, "Download successfully completed", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() //Showing failure message
    }

}