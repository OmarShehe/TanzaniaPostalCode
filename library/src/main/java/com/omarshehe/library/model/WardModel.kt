package com.omarshehe.library.model

data class WardModel (
    var name:String="",
    var postCode:Int=0,
    var streets :ArrayList<StreetModel> = ArrayList()
)
