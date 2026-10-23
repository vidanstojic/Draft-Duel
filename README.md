# Draft Duel

Brz 1v1 duel (3–5 min): dva igrača sa po 20 poena licitiraju za 10 kartica sportista, a pobeđuje veći zbir ocena.
Jedan kod za Android i iOS: **Kotlin + Compose Multiplatform**.

> **Status:** samo UI. Podaci su lažni (`data/fake`), protivnik u svim modovima je bot na telefonu, a server još ne postoji.
> Pravila igre (`MatchEngine`) su prava i pokrivena testovima.

## Pokretanje

### Android (telefon ili emulator)
1. Otvori folder projekta u **Android Studio** (Ladybug ili noviji, AGP 8.9.1). Gradle sync povlači sve zavisnosti.
2. Samsung telefon: *Settings › About phone › Software information*, 7× tapni **Build number**, pa u *Developer options* uključi **USB debugging**.
3. Poveži telefon, izaberi konfiguraciju **composeApp** i klikni **Run**.

Pregledi ekrana bez pokretanja: otvori bilo koji `*Screen.kt` i koristi Preview panel (`@Preview` funkcije su na dnu fajlova).

### iOS (kasnije, na Mac-u)
1. Instaliraj Xcode i XcodeGen: `brew install xcodegen`.
2. `cd iosApp && xcodegen generate` pa otvori `iosApp.xcodeproj`.
3. Izaberi simulator ili iPhone i pokreni. Xcode pre builda sam kompajlira Kotlin framework (`embedAndSignAppleFrameworkForXcode`).

Za iOS se ne piše novi UI. Swift deo je samo omotač (`iosApp/iosApp/ContentView.swift`), a svi ekrani su u `commonMain`.

## Struktura

```
composeApp/src/
├── commonMain/kotlin/com/vidanstojic/draftduel/
│   ├── App.kt                 koren UI-ja (Android i iOS)
│   ├── di/                    AppContainer: ručni DI, ovde se lažne implementacije menjaju pravim
│   ├── domain/
│   │   ├── model/             Sport, AthleteCard, ThemePack, GameMode, MatchSetup…
│   │   ├── match/             MatchEngine (pravila), MatchState, BotStrategy, GameRules
│   │   └── repository/        interfejsi: paketi, profili, sesija partije
│   ├── data/fake/             primer paketa (fudbal, košarka) i bot profili
│   ├── navigation/            type-safe rute i NavHost
│   ├── platform/              expect/actual (za sada samo sistemsko deljenje)
│   └── ui/
│       ├── theme/             tokeni boja, tipografija, razmaci, DraftDuelTheme
│       ├── surface/           podloge odozgo: sto, teren, parket, šljaka (crtane kodom)
│       ├── components/        dugmad, kartice, avatar, gomila kartica, tajmer, kontrole licitiranja…
│       └── screens/           home, category, matchmaking, invite, match, result, local
├── commonMain/composeResources/values/strings.xml   svi tekstovi (engleski)
├── commonTest/                testovi pravila (primeri iz spec §3)
├── androidMain/               MainActivity, Application, manifest, ikonica
└── iosMain/                   MainViewController za Swift
iosApp/                        Swift omotač + XcodeGen project.yml
```

**Konvencije**
- Svaki ekran ima `XRoute` (uzima ViewModel i stanje), `XScreen` (stateless, može da se pregleda u Preview-u) i, gde ima stanja, `XViewModel` + `XUiState`.
- Boje se ne pišu direktno u komponentama, nego se čitaju iz `DraftDuelTheme.colors`. Isti ekran se zato sam prebarva za sto, teren ili parket.
- `commonMain` ne zna za `android.*` ni `UIKit`. Platformske stvari idu kroz `expect/actual` u `platform/`.
- Tekst je u `strings.xml`, pa se srpski kasnije dodaje kao `values-sr/strings.xml`.

## Šta je urađeno u UI-ju

| Ekran | Šta prikazuje |
|---|---|
| Home | Tri moda: *Find opponent*, *Same device*, *Play a friend* + pravila |
| Category | Sport (podloga se vidi na pločici, tenis „uskoro“), izvor bazena (nasumično, po kategoriji, sopstveni samo za prijatelja) |
| Find opponent | Traženje → „Opponent found“ → odbrojavanje. U v1 je to bot sa imenom igrača |
| Invite | Kod i link, kopiranje, sistemsko deljenje, istek linka, ulazak preko koda, ponuda bota posle 60 s |
| Match | Licitacija na podlozi sporta, gomile kartica bez limita, budžeti, skrivene ocene, „X can bid up to N“, biranje kad je neko na 0, dodela |
| Result | Obe ekipe otkrivene, rezultat, „best buy“, deljenje, revanš |
| Same device | Imena + izbor: podeljen ekran (gornja polovina okrenuta 180°) ili predavanje telefona (ekran za predaju, tajmer pauziran) |

**Odluke iz dogovora koje odstupaju od speca v1**
- Nema limita od 5 kartica, jedino ograničenje je budžet. Partija ima tačno 10 dodela.
- Ocena je skrivena tokom licitacije. Vlasnik je vidi kad osvoji karticu, a protivnik tek na kraju. Na podeljenom ekranu ocene su skrivene za oba igrača do kraja.
- Kartica = igrač + sezona + država (bez kluba i grba). Avatar je silueta bez lica, sa neutralnim bojama dresa.
- „Find opponent“ je u v1 bot, a v2 menja samo repozitorijum.

## Testovi

```
./gradlew :composeApp:allTests
```
`MatchEngineTest` pokriva redove tabele primera iz spec §3, prilagođene pravilima bez limita kartica.

## Sledeći koraci
- Fontovi: dodati Bricolage Grotesque i IBM Plex Mono u `composeResources/font` i zameniti ih u `ui/theme/Typography.kt`.
- Animacija „kartica leti u gomilu“ (spec §6) i slika za deljenje rezultata.
- Izdvojiti `domain/match` u modul `:game-core` koji deli i Ktor server (spec §8).
- Pravi repozitorijumi (keš paketa, WebSocket klijent) umesto `data/fake`.
