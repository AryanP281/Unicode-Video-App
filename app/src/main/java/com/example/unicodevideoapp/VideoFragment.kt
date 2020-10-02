package com.example.unicodevideoapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class VideoFragment : Fragment()
{
    companion object
    {
        fun newInstance() : VideoFragment
        {
            return VideoFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        val fragmentView : View = inflater.inflate(R.layout.fragment_video, container, false)

        return fragmentView
    }

}