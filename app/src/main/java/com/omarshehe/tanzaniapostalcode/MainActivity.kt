package com.omarshehe.tanzaniapostalcode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.omarshehe.library.Districts
import com.omarshehe.library.Regions

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Regions.getRegionsList()
        Districts().getAllDistrict()
    }
}