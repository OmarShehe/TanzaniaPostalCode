# Import report

**Result: PASSED**

- Dataset version: 3
- Source edition: 2016-04-22 (Gazette Notice 240); Zanzibar 2012-07-30
- Generated at: 2026-10-04T00:00:00Z
- tz-address.json was written

## Totals

- Regions: 31
- Districts: 168
- Wards: 4058
- Mtaa/village (incl. Zanzibar shehia): 17039
- Kitongoji: 64262
- Source lines parsed: 68915
- Anomalies: 207 (ratio 0.0030)

## Per region

| Code | Region | Districts | Wards | Mtaa | Kitongoji |
|---|---|---|---|---|---|
| 11000 | Dar es Salaam | 6 | 102 | 565 | 0 |
| 21000 | Tanga | 9 | 245 | 1044 | 4610 |
| 23000 | Arusha | 7 | 158 | 569 | 1480 |
| 25000 | Kilimanjaro | 7 | 168 | 589 | 2259 |
| 27000 | Manyara | 6 | 142 | 547 | 1974 |
| 30000 | Geita | 5 | 121 | 546 | 2260 |
| 31000 | Mara | 6 | 178 | 703 | 2630 |
| 33000 | Mwanza | 7 | 191 | 914 | 3422 |
| 35000 | Kagera | 8 | 195 | 758 | 3685 |
| 37000 | Shinyanga | 4 | 130 | 598 | 2751 |
| 39000 | Simiyu | 5 | 133 | 574 | 2705 |
| 41000 | Dodoma | 8 | 209 | 763 | 3446 |
| 43000 | Singida | 6 | 136 | 499 | 2311 |
| 45000 | Tabora | 7 | 206 | 869 | 3718 |
| 47000 | Kigoma | 7 | 103 | 372 | 1592 |
| 50000 | Katavi | 3 | 58 | 220 | 932 |
| 51000 | Iringa | 4 | 106 | 584 | 1855 |
| 53000 | Mbeya | 6 | 178 | 777 | 2941 |
| 54100 | Songwe | 4 | 93 | 384 | 1484 |
| 55000 | Rukwa | 4 | 97 | 519 | 1854 |
| 57000 | Ruvuma | 6 | 172 | 655 | 3769 |
| 59000 | Njombe | 5 | 107 | 463 | 1828 |
| 61000 | Pwani | 8 | 133 | 522 | 1576 |
| 63000 | Mtwara | 6 | 191 | 996 | 3339 |
| 65000 | Lindi | 6 | 153 | 671 | 2429 |
| 67000 | Morogoro | 8 | 213 | 1007 | 3412 |
| 71000 | Mjini Magharibi | 2 | 34 | 84 | 0 |
| 72000 | Kusini Unguja | 2 | 22 | 61 | 0 |
| 73000 | Kaskazini Unguja | 2 | 22 | 65 | 0 |
| 74000 | Kusini Pemba | 2 | 35 | 62 | 0 |
| 75000 | Kaskazini Pemba | 2 | 27 | 59 | 0 |

## Validation

No violations.

### Notes
- District 501 printed as 'MPANDA -CBD', read as 'Mpanda CBD'
- District 593 printed as 'WANGING'O MBE', read as 'Wanging'ombe'
- Ward 'Njisi' (postcode '73733', Kyela, Mbeya) left out: does not start with its district code 537
- Ward 'Lituta' (postcode '57731', Songea, Ruvuma) left out: does not start with its district code 572
- Ward 'Kilosampepo' (postcode '678010', Malinyi, Morogoro) left out: postcode is not 5 digits

### Warnings (1)
- DUPLICATE_OLD_POSTCODE: Old postcode 53829 is given by wards 54112, 54228

## Anomalies

First 50 of 207 (all rows are in `import-anomalies.csv`):

- p38 l27 KITONGOJI_WITHOUT_MTAA: Mombo 21620 Majengo
- p47 l15 KITONGOJI_WITHOUT_MTAA: LUSHOTO 217 LUSHOTO 21701 Chakechake
- p84 l21 KITONGOJI_WITHOUT_MTAA: Magamba 21745 Magamba
- p5 l24 KITONGOJI_WITHOUT_MTAA: ARUSHA 232 Olturumet 23201 Ekenywa
- p8 l9 KITONGOJI_WITHOUT_MTAA: Kiranyi 23209 Kiranyi
- p8 l13 KITONGOJI_WITHOUT_MTAA: Olorien 23210 Olorien
- p9 l17 KITONGOJI_WITHOUT_MTAA: Olmotonyi 23214 Ngaramtoni
- p10 l11 KITONGOJI_WITHOUT_MTAA: Moivo 23217 Moivo
- p13 l16 KITONGOJI_WITHOUT_MTAA: Tarakwa 23226 Tarakwa
- p13 l18 KITONGOJI_WITHOUT_MTAA: Ilboru 23227 Oltulelei
- p13 l20 KITONGOJI_WITHOUT_MTAA: Arumeru 233 Usa River 23301 Ngaresero
- p24 l7 KITONGOJI_WITHOUT_MTAA: Monduli 234 Monduli Mjini 23401 Monduli Magharibi
- p36 l19 KITONGOJI_WITHOUT_MTAA: Karatu 236 Karatu 23601 Kati
- p36 l30 KITONGOJI_WITHOUT_MTAA: Ganako 23602 Ayalabe Magharibi
- p44 l21 KITONGOJI_WITHOUT_MTAA: Ngorongoro 237 Orgosorok 23701 Loliondo Mashariki
- p15 l10 KITONGOJI_WITHOUT_MTAA: Makuyuni (Himo) 25223 Mieresini B
- p20 l19 KITONGOJI_WITHOUT_MTAA: Njia Panda 25232 Njia Panda Mashariki
- p26 l14 KITONGOJI_WITHOUT_MTAA: Bomang'ombe 25312 Kibaoni
- p27 l32 KITONGOJI_WITHOUT_MTAA: Muungano 25316 Kambi ya Raha
- p27 l38 KITONGOJI_WITHOUT_MTAA: Bondeni 25317 Kingereka A
- p35 l36 KITONGOJI_WITHOUT_MTAA: Mwanga 25511 Mabomani
- p39 l30 KITONGOJI_WITHOUT_MTAA: SAME 256 Same 25601 Ujamaa A
- p39 l36 KITONGOJI_WITHOUT_MTAA: Stesheni 25602 Stesheni
- p40 l3 KITONGOJI_WITHOUT_MTAA: Kisima 25603 Kisima
- p55 l2 KITONGOJI_WITHOUT_MTAA: Kelamfua/Mokala 25711 Mhembeni
- p13 l38 KITONGOJI_WITHOUT_MTAA: Hanang' 273 Katesh 27301 Katesh Stendi
- p14 l5 KITONGOJI_WITHOUT_MTAA: Ganana 27302 Katesh ‘A’
- p24 l21 KITONGOJI_WITHOUT_MTAA: Dumbeta 27332 Dumbeta
- p24 l30 KITONGOJI_WITHOUT_MTAA: Jordon 27333 Udameschek
- p39 l27 KITONGOJI_WITHOUT_MTAA: KITETO 275 Kibaya 27501 Bomani
- p39 l32 KITONGOJI_WITHOUT_MTAA: Bwagamoyo 27502 Jangwani
- p41 l29 KITONGOJI_WITHOUT_MTAA: Matui 27507 Soweto
- p46 l30 KITONGOJI_WITHOUT_MTAA: Kaloleni 27522 Kaloleni
- p46 l35 KITONGOJI_WITHOUT_MTAA: Bwawani 27523 Bwawani
- p46 l39 KITONGOJI_WITHOUT_MTAA: SIMANJIRO 276 Orkesumet 27601 Orkesumet
- p50 l2 KITONGOJI_WITHOUT_MTAA: Mererani 27609 Tunduru
- p53 l15 KITONGOJI_WITHOUT_MTAA: Endiamutu 27616 Kaza Moyo Juu
- p5 l32 KITONGOJI_WITHOUT_MTAA: Katoro 30113 Mlimani
- p11 l6 KITONGOJI_WITHOUT_MTAA: Nyamigota 30127 Elimu
- p18 l21 KITONGOJI_WITHOUT_MTAA: Ludete 30149 Ibondo
- p54 l17 KITONGOJI_WITHOUT_MTAA: Ng’anzo 30510 Segwe
- p54 l40 KITONGOJI_WITHOUT_MTAA: Igulwa 30511 Igulwa
- p55 l9 KITONGOJI_WITHOUT_MTAA: Ushirombo 30512 Ifungamawazo
- p55 l25 KITONGOJI_WITHOUT_MTAA: Butizya 30513 Ngita
- p56 l2 KITONGOJI_WITHOUT_MTAA: Katente 30514 Majengo
- p56 l12 KITONGOJI_WITHOUT_MTAA: Bulangwa 30515 Mission
- p56 l20 KITONGOJI_WITHOUT_MTAA: Katome 30516 Bugama
- p56 l40 KITONGOJI_WITHOUT_MTAA: Bulega 30517 Bulega senta
- p1 l1 KITONGOJI_WITHOUT_MTAA: MARA 31 MUSOMA CBD 311 Mukendo 31101 Mukendo
- p1 l4 KITONGOJI_WITHOUT_MTAA: Mwigobero 31102 Mwigobero A
