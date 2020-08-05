package com.omarshehe.library

class Wards {
    fun getAllWords():LinkedHashMap<String,ArrayList<String>>{
        return LinkedHashMap<String,ArrayList<String>>().apply {
            set("Kati", arrayListOf<String>().apply {
                add("Bambi")
                add("Binguni")
                add("Bungi")
                add("Charawe")
                add("Cheju")
                add("Chwaka")
                add("Dunga Bweni")
                add("Dunga Kiembeni")
                add("Ghana")
                add("Jendele")
                add("Kiboje Mkwajuni")
                add("Kiboje Mwembeshauri")
            })
            set("Mjini", arrayListOf<String>().apply {
                add("Amani")
                add("Chumbuni")
                add("Gulioni")
                add("Jang'ombe")
                add("Karakana")
                add("Kidongo Chekundu")
                add("Kikwajuni Bondeni")
                add("Kikwajuni Juu")
                add("Kilimahewa Bondeni")
                add("Kilimahewa Juu")
                add("Kilimani")
                add("Kiponda")
                add("Kisima Majongoo")
                add("Kisiwandui")
                add("Kwaalimsha")
                add("Kwaalinatu")
                add("Kwahani")
                add("Kwamtipura")
                add("Kwa Wazee")
                add("Magomeni")
                add("Makadara")
                add("Malindi")
                add("Matarumbeta")
                add("Mchangani")
                add("Meya")
                add("Miembeni")
                add("Migombani")
                add("Mikunguni")
                add("Mkele")
                add("Mkunazini")
                add("Mlandege")
                add("Mpendae")
                add("Muungano")
                add("Mwembeladu")
                add("Mwembe Makumbi")
                add("Mwembeshauri")
                add("Mwembetanga")
                add("Nyerere")
                add("Rahaleo")
                add("Sebleni")
                add("Shangani")
                add("Shaurimoyo")
                add("Sogea")
                add("Urusi")
                add("Vikokotoni")
            })
        }
    }


    fun getWardsByDistrict(district:String):ArrayList<String>{
        return getAllWords()[district] ?: arrayListOf()
    }
}