# Import report

**Result: PASSED**

- Dataset version: 1
- Source edition: 2012-07-30
- Generated at: 2026-09-29T00:00:00Z
- tz-address.json was written

## Totals

- Regions: 30
- Districts: 163
- Wards: 3416
- Mtaa/village (incl. Zanzibar shehia): 15820
- Kitongoji: 16883
- Source lines parsed: 29525
- Anomalies: 140 (ratio 0.0047)

## Per region

| Code | Region | Districts | Wards | Mtaa | Kitongoji |
|---|---|---|---|---|---|
| 11000 | Dar es Salaam | 4 | 90 | 462 | 0 |
| 21000 | Tanga | 9 | 206 | 1015 | 413 |
| 23000 | Arusha | 7 | 123 | 472 | 40 |
| 25000 | Kilimanjaro | 7 | 153 | 576 | 0 |
| 27000 | Manyara | 6 | 124 | 440 | 100 |
| 30000 | Geita | 5 | 98 | 1545 | 284 |
| 31000 | Mara | 6 | 155 | 484 | 836 |
| 33000 | Mwanza | 7 | 152 | 692 | 555 |
| 35000 | Kagera | 8 | 181 | 726 | 1378 |
| 37000 | Shinyanga | 4 | 93 | 401 | 644 |
| 39000 | Simiyu | 5 | 110 | 469 | 1581 |
| 41000 | Dodoma | 8 | 182 | 644 | 1053 |
| 43000 | Singida | 6 | 124 | 492 | 1820 |
| 45000 | Tabora | 7 | 163 | 725 | 688 |
| 47000 | Kigoma | 7 | 103 | 372 | 1592 |
| 50000 | Katavi | 3 | 41 | 131 | 541 |
| 51000 | Iringa | 4 | 93 | 524 | 44 |
| 53000 | Mbeya | 9 | 218 | 1030 | 1079 |
| 55000 | Rukwa | 4 | 62 | 442 | 776 |
| 57000 | Ruvuma | 6 | 140 | 604 | 1377 |
| 59000 | Njombe | 5 | 94 | 377 | 848 |
| 61000 | Pwani | 7 | 111 | 538 | 84 |
| 63000 | Mtwara | 6 | 150 | 899 | 184 |
| 65000 | Lindi | 6 | 142 | 573 | 24 |
| 67000 | Morogoro | 7 | 168 | 856 | 942 |
| 71000 | Mjini Magharibi | 2 | 34 | 84 | 0 |
| 72000 | Kusini Unguja | 2 | 22 | 61 | 0 |
| 73000 | Kaskazini Unguja | 2 | 22 | 65 | 0 |
| 74000 | Kusini Pemba | 2 | 35 | 62 | 0 |
| 75000 | Kaskazini Pemba | 2 | 27 | 59 | 0 |

## Validation

No violations.

### Notes
- Songwe region (created in 2016) is not in this edition; later administrative changes are not reflected.

### Warnings (3)
- WARD_WITHOUT_MTAAS: Ward 39318 'Binza' has no mtaa/village
- WARD_WITHOUT_MTAAS: Ward 39319 'Nyalikungu' has no mtaa/village
- WARD_WITHOUT_MTAAS: Ward 59501 'Iwawa' has no mtaa/village

## Anomalies

First 50 of 140 (all rows are in `import-anomalies.csv`):

- p50 l8 KITONGOJI_WITHOUT_MTAA: LUSHOTO 217 LUSHOTO 21701 Kitopeni
- p60 l24 KITONGOJI_WITHOUT_MTAA: Handeni 218 Chanika 21801 Mnazini
- p64 l9 KITONGOJI_WITHOUT_MTAA: Vibaoni 21802 Nkavunka
- p67 l1 KITONGOJI_WITHOUT_MTAA: Kideleko 21803 Kideleko
- p84 l19 KITONGOJI_WITHOUT_MTAA: Arumeru 233 Usa River 23301 Usa River
- p87 l17 KITONGOJI_WITHOUT_MTAA: Monduli 234 Monduli Mjini 23401 Magharibi
- p91 l10 KITONGOJI_WITHOUT_MTAA: Karatu 236 Karatu 23601 Gwandumehhi
- p92 l3 KITONGOJI_WITHOUT_MTAA: Ganako 23602 Ayalabe Kusini
- p131 l23 KITONGOJI_WITHOUT_MTAA: MBULU 274 Ayomohe 27401 Ayomohe
- p132 l7 KITONGOJI_WITHOUT_MTAA: Sanu Baray 27402 Ayapara
- p132 l11 KITONGOJI_WITHOUT_MTAA: Imboru 27403 Ujenzi
- p132 l16 KITONGOJI_WITHOUT_MTAA: Endagokot 27404 Endamaski
- p132 l21 KITONGOJI_WITHOUT_MTAA: Ayamaani 27405 Ayamaani
- p133 l3 KITONGOJI_WITHOUT_MTAA: Uhuru 27406 Madukani
- p141 l3 KITONGOJI_WITHOUT_MTAA: Mererani 27609 Songambele 'A'
- p142 l5 KITONGOJI_WITHOUT_MTAA: Endiamtu 27616 Kilima Hewa
- p216 l1 KITONGOJI_WITHOUT_MTAA: MARA 31 MUSOMA CBD 311 Mukendo 31101 Mukendo
- p216 l5 KITONGOJI_WITHOUT_MTAA: Mwigobero 31102 Mwigobero B
- p216 l8 KITONGOJI_WITHOUT_MTAA: Iringo 31103 Iringo A
- p216 l12 KITONGOJI_WITHOUT_MTAA: Kitaji 31104 Kitaji A
- p216 l16 KITONGOJI_WITHOUT_MTAA: Kamunyonge 31105 Biafra
- p216 l21 KITONGOJI_WITHOUT_MTAA: Mwisenge 31106 Majita Road
- p217 l1 KITONGOJI_WITHOUT_MTAA: Nyamatare 31107 Mara Sekondari
- p217 l6 KITONGOJI_WITHOUT_MTAA: Nyasho 31108 Magereza
- p217 l11 KITONGOJI_WITHOUT_MTAA: Nyakato 31109 Baruti
- p217 l17 KITONGOJI_WITHOUT_MTAA: Makoko 31110 Bukanga
- p217 l22 KITONGOJI_WITHOUT_MTAA: Buhare 31111 Buhare
- p217 l25 KITONGOJI_WITHOUT_MTAA: Kigera 31112 Bonde Kati
- p218 l2 KITONGOJI_WITHOUT_MTAA: Bweri 31113 Bweri
- p247 l6 KITONGOJI_WITHOUT_MTAA: TARIME 314 Bomani 31401 Buhemba
- p247 l11 KITONGOJI_WITHOUT_MTAA: Sabasaba 31402 Sabasaba
- p247 l15 KITONGOJI_WITHOUT_MTAA: Nyamisangura 31403 Masati
- p250 l15 KITONGOJI_WITHOUT_MTAA: BUNDA 315 Bunda Mjini 31501 Faranga
- p250 l24 KITONGOJI_WITHOUT_MTAA: Bunda Stoo 31502 Bunda
- p251 l6 KITONGOJI_WITHOUT_MTAA: Balili 31503 Nyamakokoto
- p251 l14 KITONGOJI_WITHOUT_MTAA: Nyasura 31504 Nyasura
- p255 l22 KITONGOJI_WITHOUT_MTAA: SERENGETI 316 Mugumu Mjini 31601 NHC
- p256 l6 KITONGOJI_WITHOUT_MTAA: Mugumu 31602 Sedeco
- p256 l8 KITONGOJI_WITHOUT_MTAA: Stendi Kuu 31603 Chamoto
- p256 l10 KITONGOJI_WITHOUT_MTAA: Geitasamo 31604 Nyamoko
- p256 l13 KITONGOJI_WITHOUT_MTAA: Morotonga 31605 Seronga
- p256 l15 KITONGOJI_WITHOUT_MTAA: Uwanja wa ndege 31606 Burunga
- p270 l14 KITONGOJI_WITHOUT_MTAA: SENGEREMA 333 Ibisabageni 33301 Sekondari Road
- p270 l19 KITONGOJI_WITHOUT_MTAA: Nyatukara 33302 Bomani
- p270 l23 KITONGOJI_WITHOUT_MTAA: Nyampulukano 33303 Nyampulukano/Igogo
- p271 l5 KITONGOJI_WITHOUT_MTAA: Mwambaluhi 33304 Isung'ang'holo
- p297 l6 KITONGOJI_WITHOUT_MTAA: UKEREWE 336 Nansio 33601 Mwaloli
- p300 l12 KITONGOJI_WITHOUT_MTAA: Kwimba 338 Ngudu 33801 Majengo "A"
- p362 l24 KITONGOJI_WITHOUT_MTAA: MULEBA 355 Muleba 35501 Muleba Mjini
- p375 l8 KITONGOJI_WITHOUT_MTAA: NGARA 357 Ngara Mjini 35701 Mumasama
