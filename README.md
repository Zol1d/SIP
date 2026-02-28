# Stundu Izmaiņu Paziņotājs

Projekts tika uzsākts 2021. gadā, lai stundu izmaiņu uzziņu padrītu ērtāku Rīgas 64. Vidusskolā.

Vēlāk 2024. gadā noņemts no Play Store.

### Projekta statuss
Kopš 2023. gada oktobra vairs netiek uzturēts. Šobrīd, 2026. gadā, principā vēl strādā.

XML UI sistēma, var teikt, ir novecojusi, kā arī vēlreiz publicēt Google Play būtu šausmas, tādēļ paturu tiaki savai lietošanai, kā arī, protams, ja kādam ir vēlme uzbūvēt lokāli, visam principā būtu jāstrādā.
Situācijā, ja klases ir nav atjaunotas, vai kaut kas nedarbojas, viens no iemesliem varētu būt Firebase projekts, ko es visticamāk neatjaunoju. Šādā situācijā tev būs jauztaisa savs Firebase projekts ar Realtime Database funkciju ieslēgtu, jānomaina google-services.json fails uz savējo un failā MainActivity.kt jānomaina Realtime Database links. Datubāzes struktūra ir šāda:
```
{
  "classes": [
    "5.A",
    ...
    "6.B",
    "6.C",
    "6.D",
    "7.A",
    ...
  ]
}
```

Manuprāt Scraper.kt scrapeChanges() funkcija joprojām ir savos uzdevuma augstumos, tādēļ šī būtu vienīgā lieta, ko paturētu, ja kādreiz pārtaisītu šo.

### Zināmās problēmas
Kādā Android atjauninājumā tika mainītas paziņojumu atļaujas, un šobŗīd, lai gan paziņojumiem vajadzētu būt "piespraustiem", tos var noņemt bez "Sapratu" pogas nospiešanas, tādēļ, cik zinu, nosūtītie paziņojumi netiek pareizi iztīrīti no atmiņas, ja nenospiež "Sapratu" pogu.

