# Draft Duel
                                     
Brz 1v1 duel (3–5 min): dva igrača sa po 20 poena licitiraju za 10 kartica sportista, a pobeđuje veći zbir ocena.
Jedan kod za Android i iOS: **Kotlin + Compose Multiplatform**.

> **Status:** UI + prava pravila + šest botova. Teme: fudbal (baza ocena v5, 150 igrača), košarka (NBA i Evroliga, 250 igrača), filmovi (150, ručno) i hrana (20 jela, ručno).
> Muzika, zvučni efekti, vibracija i podešavanja; uputstvo „How to play“; pauza u partiji protiv bota i na istom telefonu.
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

## Objava na Google Play

Paket je `com.vidanstojic.draftduel`. Uputstvo za interni test, tekstovi za prodavnicu, politika privatnosti,
odgovori za *App content* i grafike su u [`docs/play/`](docs/play/README.md). Release build se potpisuje
ključem iz `keystore.properties`, koji git ignoriše (šablon je `keystore.properties.example`).

## Struktura

```
composeApp/src/
├── commonMain/kotlin/com/vidanstojic/draftduel/
│   ├── App.kt                 koren UI-ja (Android i iOS)
│   ├── di/                    AppContainer: ručni DI, ovde se lažne implementacije menjaju pravim
│   ├── domain/
│   │   ├── model/             Theme/Scoring, GameCard, ThemePack, MatchPool, GameMode…
│   │   ├── match/             MatchEngine (pravila), MatchState, GameRules
│   │   ├── bot/               ličnosti botova, procena ocene, vrednovanje, lookahead model
│   │   └── repository/        interfejsi: paketi, profili, sesija partije
│   ├── data/pack/             učitavanje paketa iz composeResources/files/packs (JSON, spec §5)
│   ├── data/fake/             primer paketa (košarka, rezervni fudbal) i bot profili
│   ├── navigation/            type-safe rute i NavHost
│   ├── feedback/              GameFeedback (muzika, efekti, vibracija) i SettingsRepository
│   ├── platform/              PlatformServices: KeyValueStore, AudioOutput, HapticOutput, deljenje
│   └── ui/
│       ├── theme/             tokeni boja, tipografija, razmaci, DraftDuelTheme
│       ├── surface/           podloge odozgo: sto, teren, parket, šljaka (crtane kodom)
│       ├── components/        dugmad, kartice, avatar, gomila kartica, tajmer, kontrole licitiranja…
│       └── screens/           home, category, matchmaking, invite, match, result, local
├── commonMain/composeResources/values/strings.xml   svi tekstovi (engleski)
├── commonMain/composeResources/files/packs/         paketi koji idu uz aplikaciju (football, basketball, movies, food)
├── commonTest/                testovi pravila (spec §3), paketa i botova (simulacija spec §17)
├── androidMain/               MainActivity, Application, manifest, ikonica
└── iosMain/                   MainViewController za Swift
iosApp/                        Swift omotač + XcodeGen project.yml
tools/build_football_pack.py   pravi football.json iz draft_duel.db (30 po ligi)
tools/export_pack.py           stari izvoz paketa iz tabele pack_athletes (10 po ligi)
tools/build_basketball_pack.py pravi basketball.json iz tools/basketball/*.csv (NBA i Evroliga)
tools/validate_pack.py         proverava sve pakete (opseg ocena, kategorije, recepti = 100)
tools/audio/make_audio.py      sintetiše muziku i zvučne efekte (files/audio, MP3)
```

**Konvencije**
- Svaki ekran ima `XRoute` (uzima ViewModel i stanje), `XScreen` (stateless, može da se pregleda u Preview-u) i, gde ima stanja, `XViewModel` + `XUiState`.
- Boje se ne pišu direktno u komponentama, nego se čitaju iz `DraftDuelTheme.colors`. Isti ekran se zato sam prebarva za sto, teren ili parket.
- `commonMain` ne zna za `android.*` ni `UIKit`. Platformske stvari idu kroz `expect/actual` u `platform/`.
- Tekst je u `strings.xml`, pa se srpski kasnije dodaje kao `values-sr/strings.xml`.

## Šta je urađeno u UI-ju

| Ekran | Šta prikazuje |
|---|---|
| Home | Četiri moda: *Find opponent*, *Play vs bot*, *Same device*, *Play a friend*, *How to play* i podešavanja |
| How to play | Četiri strane: šta je igra, jedna karta, 20 poena i 5 karata, otkrivanje i rezultat. Otvara se sama pri prvom pokretanju |
| Settings | Muzika, zvučni efekti, vibracija (pamte se), uputstvo, verzija |
| Category | Tema (podloga se vidi na pločici, tenis „uskoro“), izvor bazena (nasumično, po kategoriji, sopstveni samo za prijatelja) |
| Find opponent | Traženje → „Opponent found“ → odbrojavanje. U v1 je to bot sa imenom igrača |
| Invite | Kod i link, kopiranje, sistemsko deljenje, istek linka, ulazak preko koda, ponuda bota posle 60 s |
| Match | Licitacija na podlozi sporta, gomile kartica bez limita, budžeti, skrivene ocene, „X can bid up to N“, biranje kad je neko na 0, dodela |
| Result | Obe ekipe otkrivene, rezultat (u fudbalu i košarci kao pravi rezultat, ispod zbir ocena), „best buy“, deljenje, revanš |
| Same device | Imena + izbor: podeljen ekran (gornja polovina okrenuta 180°) ili predavanje telefona (ekran za predaju, tajmer pauziran) |
| Play vs bot | Sport i bazen, pa izbor jednog od šest botova (opis i težina). Radi bez interneta |

**Odluke iz dogovora koje odstupaju od speca v1**
- Nema limita od 5 kartica, jedino ograničenje je budžet. Partija ima tačno 10 dodela.
- Ocena je skrivena tokom licitacije. Vlasnik je vidi kad osvoji karticu, a protivnik tek na kraju. Na podeljenom ekranu ocene su skrivene za oba igrača do kraja.
- Kartica = igrač + sezona + država (bez kluba i grba). Avatar je silueta bez lica, sa neutralnim bojama dresa.
- „Find opponent“ je u v1 bot, a v2 menja samo repozitorijum.

## Fudbalski paket

`football.json` pravi `tools/build_football_pack.py` direktno iz `draft_duel.db` (model v5, bez pandas): 150 igrača, po 30 iz svake lige
(kategorija = država lige). Po ligi 3 zvezde (90+), 9 jakih (80–89) i 18 solidnih (65–79); Francuska nema 90+, pa ta mesta dobijaju njeni najbolji jaki igrači.
Uvek su u paketu igrači prethodnog paketa i poznata imena (`FAMOUS`: Salah, Saka, Yamal, Bellingham, Vinícius, Musiala, Dembélé…),
zato je odnos 8 / 42 / 50 % umesto 10 / 30 / 60. Svaka liga ima bar 3 GK, 8 DF, 7 MF i 6 FW; solidni su raspoređeni po celom opsegu.

```
python tools/build_football_pack.py C:/Users/<ti>/Desktop/data/v5/draft_duel.db
```

Ako fajl nedostaje ili je neispravan, aplikacija koristi primer paket, pa se ne ruši.

## Košarka

`basketball.json`: 250 igrača u dve kategorije, **NBA** (150, svih 30 klubova) i **EuroLeague** (100, svih 20 klubova).
Ocene su u `tools/basketball/nba_igraci.csv` i `evroliga_igraci.csv` (svi igrači sa spiskova klubova), dodeljene ručno
(spec §5: „autor, ručno“) na osnovu uloge i učinka u sezoni 2025/26, godina i povreda. Svaka liga ima svoju skalu (NBA 69–98, Evroliga 69–93).
Ručno izabrana poznata imena (`PICKS`) su uvek u paketu, a ostatak svake grupe se popunjava iz tabela: prvo najviše ocene,
klub po klub. Odnos je 10 / 30 / 60 % (zvezde: NBA 90+, Evroliga 88+; jaki: 80+).
Na kartici su liga, pozicija (G/F/C) i sezona, bez klubova (spec §5). Broj na dresu je ukrasni, ne pravi.

```
python tools/build_basketball_pack.py          # posle izmene ocena ili izbora igrača (PICKS, TARGETS u skripti)
```

Botovi (300 partija po paru, Default pravila, NBA / Evroliga): Scout 69 / 69 %, Galáctico 60 / 55 %, Bargain 57 / 59 %,
Steady 43 / 42 %, Hustler 37 / 41 %, Sunday Sam 29 / 30 %.

## Bez ponavljanja karata

Bazen partije se vuče iz „shuffle bag“-a (`ShuffleBag`): karta se vraća tek kad su odigrane sve ostale karte istog bazena.
Kad se vreća isprazni, odigraju se poslednje sveže karte, partija se dopuni ostalima i kreće nova vreća. Jedna vreća po temi,
pamti se i posle restarta (`StoredSeenCards` u `KeyValueStore`). Bazen po kategoriji uzima samo karte te kategorije.

## Filmovi

`movies.json`: 150 filmova, ocene su dodeljene ručno (spec §5: „autor, ručno“, bez tuđih ocena), balans 16 / 48 / 86 (11 / 32 / 57 %).
Kategorije su žanrovi (Sci-Fi, Crime, Animation, Comedy, Drama, Action, po 25), na kartici su godina i zemlja.
Kartica je filmska traka sa inicijalima: nema postera ni logotipa. Podloga je bioskop (zavese, platno, sedišta).

## Hrana (recept mod)

Pravila su ista, a menja se samo bodovanje (`Scoring.RECIPE`):
- Partija je jedno jelo (kategorija), 20 jela: pica, burger, karbonara, sarma, ćevapi, maki, grčka salata, palačinke, gvakamole, tiramisu,
  gulaš, paelja, pad thai, čili kon karne, musaka, cezar salata, burek, brauniji, šopska salata, ramen.
  *Random* bira nasumično jelo.
- Pravi sastojci nose poene koji zbirno daju 100 (npr. testo 30, paradajz sos 25, mocarela 25…). Pogrešni oduzimaju (ananas −15, čokolada −20…).
  Zbir zato pokazuje koliko je igrač blizu receptu.
- Bazen čine svi pravi sastojci plus nasumični pogrešni do 10 karata.
- **Otvarač sme da preskoči kartu bez ponude i tada je niko ne dobija.** Bez toga bi pogrešan sastojak uvek pao otvaraču,
  pa bi pobeda zavisila od sreće, a ne od znanja o receptu.
- Kad je protivnik na 0, „Dajem protivniku“ ima smisla: pogrešan sastojak mu se daje.
- Na kartici je grupa namirnice (DAIRY, MEAT…), poeni imaju znak (+25 / −10), a ekran partije pokazuje jelo koje se pravi.
  Podloga je kuhinjski sto sa kariranim stolnjakom.

Provera paketa: `python tools/validate_pack.py`.

## Pravila: Default, Classic, Custom

Na ekranu kategorije biraju se pravila (`MatchRules`):
- **Default**: 5 karata i prepuštanje 1, 2, 3 (ispod). Sa ovim pravilima se igra testira.
- **Classic**: bez limita karata i bez prepuštanja, otvarač mora da ponudi.
- **Custom**: svaki parametar posebno, za testiranje: poeni po igraču (10–40), karata po partiji (6–14),
  **isti broj karata za oba igrača** (uključeno: svako završava sa pola karata partije, limit prati broj karata; isključeno: limit karata isključen ili 3–7),
  prepuštanje (uključeno ili ne, prva cena 1–3, svaka sledeća skuplja za 0–2), vreme poteza (5–20 s) i javne ocene tokom licitacije (spec §4).
  Kod javnih ocena i botovi vide tačnu ocenu. Custom nije dostupan za „Find opponent“, jer će tu kasnije igrati nepoznati ljudi sa istim pravilima.
  Botovi su podešeni za Default i Classic; sa drugim vrednostima igraju legalno, ali ne nužno optimalno.
- Hrana zadržava svoje pravilo (otvarač sme da preskoči), pa se za nju limit karata i prepuštanje ne primenjuju.
- Poslednja izabrana pravila ostaju zapamćena za sledeću partiju u istoj sesiji.

### Default: 5 karata i prepuštanje

- Svaki igrač završava sa tačno 5 karata (spec §3 v1). Kad jedan popuni slotove, ostale karte idu drugom besplatno.
- **Prepuštanje:** otvarač, pre prve ponude, može da plati da protivnik otvori kartu umesto njega. To je za kartu za koju misli da je loša.
  Cena raste za istog igrača: 1, pa 2, pa 3… poena. Plaćeni poeni nestaju (ne idu protivniku).
  Protivnik ne može da vrati prepuštanje i mora da otvori. Posle toga važe obična pravila, pa prvi otvarač može da odustane.
- U partiji dugme „Odustani“ otvaraču postaje „Hand over · −N pts“, a kod oba igrača piše „3/5 cards“.
- Istek vremena otvarača i dalje automatski nudi 1, ne prepušta (spec §3 pravilo 7).

Zašto: sa 5 slotova svaka kupljena karta zauzima mesto boljoj, pa zvezde opet vrede. Galáctico, najslabiji bot u klasičnim pravilima, ovde je drugi po jačini.

| Bot | Težina (5 karata) | Fudbal* | Filmovi* | Prepuštanja po partiji |
|---|---|---|---|---|
| Galáctico Gio | Medium | 65% | 61% | 1,6 (sve ispod tipične dobre karte, dok košta ≤ 2) |
| Sunday Sam | Easy | 27% | 28% | 0 (ne zna za dugme) |
| Bargain Bea | Medium | 56% | 57% | 1,4 |
| Steady Stefan | Medium | 42% | 50% | 0,8 (samo slabe karte, samo za 1) |
| Hustler Hugo | Easy | 36% | 33% | 0 (radije diže cenu; sa slotovima to ga košta) |
| Scout Nina | Hard | 74% | 71% | 1,8 (računa da li se isplati) |

\* prosek pobeda protiv ostalih pet, 1000 partija po paru. Otvarač prve runde pobeđuje u 48,3% (fudbal) i 47,9% (filmovi) partija.

Izmene botova za ova pravila: fer cena po karti je budžet / prazni slotovi (spec §12: S), a baza formule je donja četvrtina paketa umesto 60.
Hustler diže cenu samo na dobrim kartama. Scout-ov model računa i slotove: ko bira dok je protivnik na 0, uzima najbolje od preostalih karata.
Model računa očekivani zbir najboljih k od n preostalih karata (statistike poretka).
Tabela modela se kešira po paketu i pravilima, a prvi potez bota računa se van UI niti.

## Konačan rezultat

Pobednika uvek određuje zbir ocena (`MatchEngine.outcome`). `ScoreProjection` samo pretvara zbir u rezultat koji liči na sport:

- **Veličina pobede** zavisi od razlike u oceni po karti, pa ne zavisi od broja karata. Fudbal: ispod 3 → 1 gol razlike,
  ispod 6,5 → 2, ispod 10,5 → 3, ispod 15 → 4, inače 5. Košarka: 3 poena razlike po poenu ocene po karti (±2).
- **Poraženi:** u fudbalu golovi po fiksnim verovatnoćama za svaku razliku, u košarci poeni po njegovoj prosečnoj karti (osnova 92, 68–128).
- Ista partija uvek daje isti rezultat (seed iz karata i cena). Na botovskim partijama najčešći su 2:1, 1:0, 2:0 i 3:1,
  a u košarci pobednik najčešće ima 90–120 poena.
- Ispod rezultata stoji zbir ocena („Ratings 412 : 398“). Filmovi i hrana zadržavaju zbir.

## Pauza

U *Play vs bot* i *Same device* dugme pauze (gornja traka, odnosno srednja traka na podeljenom ekranu) zaustavlja sat,
pauzu posle dodele i razmišljanje bota. Meni pauze pokriva sto skoro neprovidnim slojem (Resume, Resign).
Partija se pauzira i sama kad aplikacija ode u pozadinu. *Find opponent* nema pauzu, jer zamenjuje pravog protivnika.

## Zvuk i vibracija

Sav zvuk je sintetisan u `tools/audio/make_audio.py` (oscilatori, šum, Karplus-Strong žice), pa je naš i bez licenci.
Muzika: meni (lounge, 92 bpm), fudbal (stadion, 124), košarka (funk, 100), filmovi (gudači i timpani, 76), hrana (bossa, 112).
Efekti: licitacija, pas, osvojena / izgubljena karta, otkucaj poslednje 3 sekunde, istek vremena, pobeda / poraz / nerešeno.
Android: MediaPlayer i SoundPool, vibracija sa dozvolom `VIBRATE`. iOS: AVAudioPlayer (ambient sesija) i UIKit feedback generatori.
Sve troje se gasi u podešavanjima.

## Botovi

Bot vidi isto što i igrač: kartu na stolu, oba budžeta i ocene karata koje je osvojio. Ocena na stolu je skrivena,
pa svaki bot licitira na svoju procenu (`RatingPerception`). Ličnost = procena + vrednovanje (`Valuation`) + stil licitiranja (`BidStyle`).

| Bot | Težina | Kako igra | Fudbal* | Filmovi* | Hrana* |
|---|---|---|---|---|---|
| Galáctico Gio | Easy (u hrani Medium) | Sve na zvezde, ostale kartice nisko; otvara zvezdu sa +5 | 24% | 28% | 50% |
| Sunday Sam | Easy | Procena ±9, precenjuje napadače, podcenjuje golmane, nasumično +1/+2/+5 | 34% | 30% | 32% |
| Bargain Bea | Medium | 85% fer cene, na kraju troši ostatak ravnomerno; „snajper“ | 47% | 46% | 49% |
| Steady Stefan | Medium | Originalna formula iz spec §12 | 57% | 49% | 48% |
| Hustler Hugo | Medium | Fer cena, ali u 45% slučajeva diže +1 preko svoje granice da protivnik plati više | 63% | 62% | 50% |
| Scout Nina | Hard | Procena ±2 i model ostatka partije (`AuctionLookahead`) | 76% | 86% | 72% |

\* prosek pobeda protiv ostalih pet, turnir bot protiv bota, 1000 partija po paru (`BotSimulation.tournament`).

**Korekcije posle turnira:**
- Scout je pre korekcije pobeđivao 89% (fudbal) i 96% (filmovi). Sada ima procenu ±2 umesto ±1 i u temama sa ocenama malo opreznije licitira (0,75–1,05 svoje cene).
- U hrani su botovi sa formulom brojali i pogrešne sastojke kao karte koje vredi kupiti, pa su potplaćivali prave (Bargain Bea 27%).
  Sada računaju samo korisne karte.
- Galáctico je u hrani Medium jer su tamo glavni sastojci zaista „zvezde“; ekran izbora bota prikazuje težinu za izabranu temu.

Svi osim Sunday Sama i Steady Stefana znaju dve stvari o kraju partije: ostatak budžeta posle poslednje karte ne vredi ništa,
a ponuda veća od protivnikovog budžeta + 1 je bačen novac. „Snajper“ licitira tačno protivnikov budžet + 1 kad to odmah osvaja kartu.
Kad je protivnik na 0, bot uzima kartu koja nešto vredi (bez limita slotova svaka takva karta povećava zbir), a pogrešan sastojak daje protivniku.

*Find opponent* i rezervna opcija u pozivu biraju skrivenu ličnost po težinama (`quickMatchWeight`), a revanš zadržava istog bota.

Uvid iz simulacije: pošto je najniža ocena 60, broj karata nosi više od razlike u ocenama, pa je skupo plaćanje zvezde loša strategija.
Otvarač prve runde pobeđuje u 48,7% (fudbal), 50,0% (filmovi) i 51,6% (hrana) partija (cilj iz spec §17: 45–55%).

## Testovi

```
./gradlew :composeApp:allTests
```
- `MatchEngineTest` pokriva redove tabele primera iz spec §3, prilagođene pravilima bez limita kartica.
- `PackJsonTest`: format paketa, odbacivanje ocena van 60–99, rezervni paket, bazen po kategoriji.
- `RecipeModeTest`: bazen jela, preskakanje otvarača, negativni poeni, botovi preskaču i poklanjaju pogrešne sastojke.
- `PersonalityBotTest`: otvarač mora da ponudi, granica i odustajanje, snajper, kraj partije, lookahead model.
- `CustomRulesTest`: budžet i broj karata iz pravila, fiksna cena prepuštanja, opsezi, javne ocene, botovi sa velikim custom vrednostima.
- `FiveSlotsTest`: prepuštanje (cena 1, 2, 3, poeni nestaju, nema vraćanja), pun igrač, botovi koji prepuštaju.
- `ScoreProjectionTest`: isti pobednik kao po zbiru, realni opsezi, veća razlika = veća pobeda, nerešeno.
- `ShuffleBagTest`: bez ponavljanja dok se bazen ne potroši, ravnomerno pojavljivanje, deljena vreća teme.
- `GameFeedbackTest`: prekidači za muziku, efekte i vibraciju, pamćenje podešavanja.
- `BotSimulationTest` (spec §17), na sve tri teme i za Default pravila: samo legalni potezi, invarijante budžeta, prednost otvarača, redosled težina.

## Sledeći koraci
- Fontovi: dodati Bricolage Grotesque i IBM Plex Mono u `composeResources/font` i zameniti ih u `ui/theme/Typography.kt`.
- Animacija „kartica leti u gomilu“ (spec §6) i slika za deljenje rezultata.
- Izdvojiti `domain/match` u modul `:game-core` koji deli i Ktor server (spec §8).
- Pravi repozitorijumi (keš i preuzimanje novih verzija paketa, WebSocket klijent) umesto `data/fake`.
- Bot u „Find opponent“ trenutno bira ličnost nasumično; kasnije po rejtingu igrača.
