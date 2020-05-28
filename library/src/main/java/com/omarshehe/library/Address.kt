package com.omarshehe.library

import com.omarshehe.library.model.DistrictModel
import com.omarshehe.library.model.RegionModel
import com.omarshehe.library.model.StreetModel
import com.omarshehe.library.model.WardModel

object Address{
    fun getAddressList():ArrayList<RegionModel>{
        return ArrayList<RegionModel>().apply {
            add(RegionModel().apply {
                name="Arusha"
                postalCode=23

                district=ArrayList<DistrictModel>().apply {

                    add(DistrictModel().apply {
                        name="Arusha CBD"
                        postalCode=231
                        ward=ArrayList<WardModel>().apply {
                            add(WardModel().apply {
                                name="Sekei"
                                postalCode=23101
                                streets=ArrayList<StreetModel>().apply {
                                    add(StreetModel().apply {
                                        name="Sanawari"
                                        postCode=23101
                                    })
                                }
                            })

                        }
                    })


                }
            })
        }
    }

}