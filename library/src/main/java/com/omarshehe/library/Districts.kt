package com.omarshehe.library

class Districts {
    fun getAllDistrict():LinkedHashMap<String,ArrayList<String>>{
        return LinkedHashMap<String,ArrayList<String>>().apply {
            set("Arusha", arrayListOf<String>().apply {
                add("Meru")
                add("Arusha City")
                add("Arusha")
                add("Karatu")
                add("Longido")
                add("Monduli")
                add("Ngorongoro")
            })
            set("Dar es Salaam", arrayListOf<String>().apply {
                add("Ilala")
                add("Kinondoni")
                add("Temeke")
                add("Kigamboni")
                add("Ubungo")
            })
            set("Dodoma", arrayListOf<String>().apply {
                add("Bahi")
                add("Chamwino")
                add("Chemba")
                add("Dodoma")
                add("Kondoa")
                add("Kongwa")
                add("Mpwapwa")
            })


            set("Mjini Magharibi (Zanzibar)", arrayListOf<String>().apply {
                add("Magharibi")
                add("Mjini")
            })

            set("Pemba North (Zanzibar)", arrayListOf<String>().apply {
                add("Micheweni")
                add("Wete")
            })
            set("Pemba South (Zanzibar)", arrayListOf<String>().apply {
                add("Chake Chake")
                add("Mkoani")
            })
            set("Pwani", arrayListOf<String>().apply {
                add("Bagamoyo")
                add("Kibaha")
                add("Kisarawe")
                add("Mafia")
                add("Mkuranga")
                add("Rufiji")
            })
            set("Rukwa", arrayListOf<String>().apply {
                add("Kalambo")
                add("Lyamba lya Mfipa")
                add("Nkasi")
                add("Sumbawanga")
            })
            set("Ruvuma", arrayListOf<String>().apply {
                add("Mbinga")
                add("Namtumbo")
                add("Nyasa")
                add("Songea")
                add("Tunduru")
            })
            set("Shinyanga", arrayListOf<String>().apply {
                add("Kahama Rural")
                add("Kahama Urban")
                add("Kishapu")
                add("Shinyanga Rural")
                add("Shinyanga Urban")
            })
            set("Simiyu", arrayListOf<String>().apply {
                add("Bariadi")
                add("Busega")
                add("Itilima")
                add("Maswa")
                add("Meatu")
            })
            set("Singida", arrayListOf<String>().apply {
                add("Iramba")
                add("Ikungi")
                add("Manyoni")
                add("Mkalama")
                add("Singida Rural")
                add("Singida Urban")
            })
            set("Songwe", arrayListOf<String>().apply {
                add("Songwe")
            })
            set("Tabora", arrayListOf<String>().apply {
                add("Igunga")
                add("Kaliua")
                add("Nzega")
                add("Sikonge")
                add("Tabora Municipal")
                add("Urambo")
                add("Uyui")
            })
            set("Tanga", arrayListOf<String>().apply {
                add("Handeni Rural")
                add("Handeni Urban")
                add("Kilindi")
                add("Korogwe Rural")
                add("Korogwe Urban")
                add("Lushoto")
                add("Muheza")
                add("Mkinga")
                add("Pangani")
                add("Tanga")
            })
            set("Unguja North (Zanzibar)", arrayListOf<String>().apply {
                add("Kaskazini A")
                add("Kaskazini B")
            })
            set("Unguja South (Zanzibar)", arrayListOf<String>().apply {
                add("Kati")
                add("Kusini")
            })

        }

    }

    fun getDistrictsByRegion(region:String):ArrayList<String>{
        return getAllDistrict()[region] ?: arrayListOf()
    }
}