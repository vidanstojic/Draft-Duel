# Draft Duel

Brz 1v1 duel (3–5 min): dva igrača sa po 20 poena licitiraju za 10 kartica sportista, a pobeđuje veći zbir ocena.
Jedan kod za Android i iOS: **Kotlin + Compose Multiplatform**.

> **Status:** UI + prava pravila + šest botova. Fudbalski paket dolazi iz baze ocena (v5), košarka je još primer (`data/fake`).
> Protivnik u svim online modovima je bot na telefonu, a server još ne postoji. Pravila (`MatchEngine`) i botovi su pokriveni testovima.

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
│   │   ├── model/             Theme, AthleteCard, ThemePack, GameMode, MatchSetup…
│   │   ├── match/             MatchEngine (pravila), MatchState, GameRules
│   │   ├── bot/               ličnosti botova, procena ocene, vrednovanje, lookahead model
│   │   └── repository/        interfejsi: paketi, profili, sesija partije
│   ├── data/pack/             učitavanje paketa iz composeResources/files/packs (JSON, spec §5)
│   ├── data/fake/             primer paketa (košarka, rezervni fudbal) i bot profili
│   ├── navigation/            type-safe rute i NavHost
│   ├── platform/              expect/actual (za sada samo sistemsko deljenje)
│   └── ui/
│       ├── theme/             tokeni boja, tipografija, razmaci, DraftDuelTheme
│       ├── surface/           podloge odozgo: sto, teren, parket, šljaka (crtane kodom)
│       ├── components/        dugmad, kartice, avatar, gomila kartica, tajmer, kontrole licitiranja…
│       └── screens/           home, category, matchmaking, invite, match, result, local
├── commonMain/composeResources/values/strings.xml   svi tekstovi (engleski)
├── commonMain/composeResources/files/packs/         paketi koji idu uz aplikaciju (football.json)
├── commonTest/                testovi pravila (spec §3), paketa i botova (simulacija spec §17)
├── androidMain/               MainActivity, Application, manifest, ikonica
└── iosMain/                   MainViewController za Swift
iosApp/                        Swift omotač + XcodeGen project.yml
tools/export_pack.py           pravi football.json iz draft_duel.db
```

**Konvencije**
- Svaki ekran ima `XRoute` (uzima ViewModel i stanje), `XScreen` (stateless, može da se pregleda u Preview-u) i, gde ima stanja, `XViewModel` + `XUiState`.
- Boje se ne pišu direktno u komponentama, nego se čitaju iz `DraftDuelTheme.colors`. Isti ekran se zato sam prebarva za sto, teren ili parket.
- `commonMain` ne zna za `android.*` ni `UIKit`. Platformske stvari idu kroz `expect/actual` u `platform/`.
- Tekst je u `strings.xml`, pa se srpski kasnije dodaje kao `values-sr/strings.xml`.

## Šta je urađeno u UI-ju

| Ekran | Šta prikazuje |
|---|---|
| Home | Četiri moda: *Find opponent*, *Play vs bot*, *Same device*, *Play a friend* + pravila |
| Category | Sport (podloga se vidi na pločici, tenis „uskoro“), izvor bazena (nasumično, po kategoriji, sopstveni samo za prijatelja) |
| Find opponent | Traženje → „Opponent found“ → odbrojavanje. U v1 je to bot sa imenom igrača |
| Invite | Kod i link, kopiranje, sistemsko deljenje, istek linka, ulazak preko koda, ponuda bota posle 60 s |
| Match | Licitacija na podlozi sporta, gomile kartica bez limita, budžeti, skrivene ocene, „X can bid up to N“, biranje kad je neko na 0, dodela |
| Result | Obe ekipe otkrivene, rezultat, „best buy“, deljenje, revanš |
| Same device | Imena + izbor: podeljen ekran (gornja polovina okrenuta 180°) ili predavanje telefona (ekran za predaju, tajmer pauziran) |
| Play vs bot | Sport i bazen, pa izbor jednog od šest botova (opis i težina). Radi bez interneta |

**Odluke iz dogovora koje odstupaju od speca v1**
- Nema limita od 5 kartica, jedino ograničenje je budžet. Partija ima tačno 10 dodela.
- Ocena je skrivena tokom licitacije. Vlasnik je vidi kad osvoji karticu, a protivnik tek na kraju. Na podeljenom ekranu ocene su skrivene za oba igrača do kraja.
- Kartica = igrač + sezona + država (bez kluba i grba). Avatar je silueta bez lica, sa neutralnim bojama dresa.
- „Find opponent“ je u v1 bot, a v2 menja samo repozitorijum.

## Fudbalski paket

`football.json` je izvezen iz `draft_duel.db` (model v5): 50 igrača, po 10 iz svake lige (kategorija = država lige),
4 zvezde / 16 jakih / 30 solidnih. U odnosu na `pack_football_v1.json` ima i `position` (GK/DF/MF/FW) i `season`.
Novi paket posle ponovnog pokretanja pipeline-a:

```
python tools/export_pack.py C:/Users/<ti>/Desktop/data/v5/draft_duel.db
```

Ako fajl nedostaje ili je neispravan, aplikacija koristi primer paket, pa se ne ruši.

## Botovi

Bot vidi isto što i igrač: kartu na stolu, oba budžeta i ocene karata koje je osvojio. Ocena na stolu je skrivena,
pa svaki bot licitira na svoju procenu (`RatingPerception`). Ličnost = procena + vrednovanje (`Valuation`) + stil licitiranja (`BidStyle`).

| Bot | Težina | Kako igra | Pobede protiv ostalih* |
|---|---|---|---|
| Galáctico Gio | Easy | Sve na zvezde (procena 85+), ostale kartice nisko; otvara zvezdu sa +5 | 25% |
| Sunday Sam | Easy | Procena ±9, precenjuje napadače, podcenjuje golmane, nasumično +1/+2/+5 | 26% |
| Bargain Bea | Medium | 85% fer cene, na kraju troši ostatak ravnomerno; „snajper“ | 47% |
| Steady Stefan | Medium | Originalna formula iz spec §12, bez izmena | 53% |
| Hustler Hugo | Medium | Fer cena, ali u 45% slučajeva diže +1 preko svoje granice da protivnik plati više | 59% |
| Scout Nina | Hard | Procena ±1 i model ostatka partije (`AuctionLookahead`) | 90% |

\* simulacija bot protiv bota, 1000 partija po paru, fudbalski paket.

Svi osim Sunday Sama i Steady Stefana znaju dve stvari o kraju partije: ostatak budžeta posle poslednje karte ne vredi ništa,
a ponuda veća od protivnikovog budžeta + 1 je bačen novac. „Snajper“ licitira tačno protivnikov budžet + 1 kad to odmah osvaja kartu.
Kad je protivnik na 0, bot uvek uzima besplatnu kartu (bez limita slotova svaka karta povećava zbir).

*Find opponent* i rezervna opcija u pozivu biraju skrivenu ličnost po težinama (`quickMatchWeight`), a revanš zadržava istog bota.

Uvid iz simulacije: pošto je najniža ocena 60, broj karata nosi više od razlike u ocenama, pa je skupo plaćanje zvezde loša strategija.
Otvarač prve runde pobeđuje u 48,5% partija (cilj iz spec §17: 45–55%).

## Testovi

```
./gradlew :composeApp:allTests
```
- `MatchEngineTest` pokriva redove tabele primera iz spec §3, prilagođene pravilima bez limita kartica.
- `PackJsonTest`: format paketa, odbacivanje ocena van 60–99, rezervni paket, bazen po kategoriji.
- `PersonalityBotTest`: otvarač mora da ponudi, granica i odustajanje, snajper, kraj partije, lookahead model.
- `BotSimulationTest` (spec §17): samo legalni potezi, invarijante budžeta, prednost otvarača, redosled težina.

## Sledeći koraci
- Fontovi: dodati Bricolage Grotesque i IBM Plex Mono u `composeResources/font` i zameniti ih u `ui/theme/Typography.kt`.
- Animacija „kartica leti u gomilu“ (spec §6) i slika za deljenje rezultata.
- Izdvojiti `domain/match` u modul `:game-core` koji deli i Ktor server (spec §8).
- Pravi repozitorijumi (keš i preuzimanje novih verzija paketa, WebSocket klijent) umesto `data/fake`.
- Košarkaški paket iz baze, kad bude postojala.
- Bot u „Find opponent“ trenutno bira ličnost nasumično; kasnije po rejtingu igrača.
