package com.omarshehe.library.model

data class DistrictModel (
    var name:String="",
    var postCode:Int=0,
    var ward : ArrayList<WardModel> = ArrayList()

)
