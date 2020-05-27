package com.omarshehe.library.model

data class RegionModel(
    var name:String="",
    var postalCode:Int=0,
    var district:ArrayList<DistrictModel> = ArrayList()

)