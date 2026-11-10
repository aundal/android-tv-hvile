# Hvile – sluk børnenes TV selv

Hvile slukker et Android TV af sig selv: efter stilhed foran skærmen, eller efter en
maks skærmtid. Til børneværelset, hvor TV'et ellers kører videre efter godnat-historien.

Når tiden er gået, vises **"Ser du stadig TV?"** over det der spiller, med nedtælling:
**Ja** fortsætter, **Nej** (eller stilhed ved nul) slukker TV'et. Ethvert tryk på
fjernbetjeningen tæller som "jeg ser med", og lyd der spiller (film, serie) tæller også –
så intet slukker midt i noget.

## To timere

| Kolonne | Slukker når |
|---|---|
| **Inaktivitet** | Fjernbetjeningen er uberørt og der er stille i N minutter |
| **Skærmtid** | Skærmen har været tændt i N minutter, uanset hvad der ses |

Hver timer har: til/fra, tid 1–300 minutter og egen advarsel 0–300 sekunder. Advarsel 0
betyder sluk med det samme uden varsel. Skærmtid nulstilles ved hver sluk/vågn.

## Krav

- Android TV 9 eller nyere, på samme Wi-Fi som computeren ved installering
- To manuelle tilvalg på TV'et: tilgængelighedstjenesten **Hvile** og
  **Vis over andre apps** (install-scriptet beder om begge)

## Installering

```powershell
addons\idle-sleep\build-and-install.ps1 -TvHost <tv-ip>
```

Åbn derefter **Hvile** på TV'et og sæt minutterne. Afinstaller med `-Uninstall`.

Bygget med JDK 17+ og Android SDK 34. Ingen netværk, ingen telemetri – appen snakker
kun med TV'et selv.

## Tilladelser

| Tilladelse | Hvorfor |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Ser fjernbetjeningstryk for at nulstille timeren (sluger ingen tryk) og udfører låseskærm-handlingen der slukker TV'et. Læser ikke skærmen. |
| `SYSTEM_ALERT_WINDOW` | Viser advarslen over pauset film eller andre apps. |
| `SCHEDULE_EXACT_ALARM` | Rammer minuttet præcist på Android 12+. |
| `RECEIVE_BOOT_COMPLETED` | Starter en frisk periode efter genstart. |

Metoden (låsskærm-handling via tilgængelighedstjeneste) følger
[CleanYourTVs sleep-timer](https://github.com/Gathin23/cleanyourtv/tree/main/addons/sleep-timer).
