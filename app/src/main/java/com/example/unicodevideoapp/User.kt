package com.example.unicodevideoapp

class User(var handle : String, var displayName : String, var bDay : Int, var bMonth : Int, var bYear : Int, var country : Int)
{
    constructor() : this("","",-1,-1,-1,-1)
}