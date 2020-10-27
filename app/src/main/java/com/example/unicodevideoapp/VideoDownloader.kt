package com.example.unicodevideoapp

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import kotlin.math.max

class VideoDownloader(val context : Context, val videoTitle : String) : AsyncTask<String,Void,Void?>()
{

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

            val fileLength : Int = httpConn.contentLength

            input = httpConn.inputStream
            videoFile = File(context.getExternalFilesDir("DownloadedVideos"), "$videoTitle.mp4")
            output = FileOutputStream(videoFile)

            val data : ByteArray = ByteArray(4096)
            var count : Int  = input.read(data)
            var downloadedSize : Int = 0
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

}