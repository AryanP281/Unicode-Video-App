package com.example.unicodevideoapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface YoutubeApiInterface
{
    @GET("search")
    fun getSearchResults(@QueryMap queryMap : Map<String, String>) : Call<KeywordSearchResult>

    @GET("videos")
    fun getVideo(@QueryMap queryMap: Map<String, String>) : Call<VideoSearchResult>
}