# Prva verzija za interni test — 0.1.0 (1)

Paket: **`com.vidanstojic.draftduel`** (ne može da se menja posle prvog uploada).
Grana: `release/0.1.0`.

Interni test ide do 100 testera po email adresi, bez Google-ovog pregleda, i dostupan je odmah.
Ime i opis u prodavnici mogu prvih ~48 h da budu privremeni.

## 1. Upload ključ (jednom)

1. U Android Studiju: *Build › Generate Signed App Bundle › Android App Bundle › Create new…*
   - Key store path: npr. `C:/Users/vidan_gofx79m/Desktop/keys/draft-duel-upload.jks` (van repoa)
   - Alias: `upload`, validity 25+ godina, ime i država.
2. Kopiraj `keystore.properties.example` u `keystore.properties` (u korenu repoa; git ga ignoriše) i upiši putanju i lozinke.
3. Napravi rezervnu kopiju `.jks` fajla i lozinki (npr. menadžer lozinki). Svaka sledeća verzija se potpisuje istim ključem.

## 2. Build

```
./gradlew :composeApp:bundleRelease
```

Rezultat: `composeApp/build/outputs/bundle/release/composeApp-release.aab`.
Pre uploada instaliraj release na telefon i odigraj jednu partiju. Na dnu početnog ekrana treba da piše `v0.1.0 (1)`, a „Play a friend“ treba da je „Coming soon“.

## 3. Play Console

1. *Create app*: ime **Draft Duel**, jezik English (US), **Game**, **Free**, prihvati izjave.
2. *Test and release › Testing › Internal testing*:
   - **Testers**: napravi email listu (do 100 adresa), sačuvaj i kopiraj **link za prijavu**.
   - **Create new release**: prihvati **Play App Signing** (Google čuva ključ za potpisivanje, ti samo upload ključ),
     uploaduj `.aab`, release name `0.1.0 (1)`, release notes iz `store-listing.md`, *Next › Save › Start rollout*.
3. Zadaci sa *Dashboard › Set up your app* (za interni test nisu svi obavezni, ali kasnije jesu):
   - **Privacy policy**: `https://vidanstojic.me/privacy/draft-duel/` (portfolio sajt, tekst u `src/legal/` tog repoa).
   - **App content**: odgovori iz `app-content.md`.
   - **Store listing**: tekstovi iz `store-listing.md`, `icon-512.png`, `feature-graphic-1024x500.png` i 2–8 snimaka ekrana sa telefona.

## 4. Testeri

Pošalji im link za prijavu i poruku iz `tester-message.md`. Svaki tester mora da:
1. otvori link na telefonu, ulogovan na Google nalog koji je na listi;
2. prihvati poziv (*Become a tester*);
3. instalira aplikaciju preko Play prodavnice sa istog linka.

## 5. Sledeći build

- Povećaj `versionCode` u `composeApp/build.gradle.kts` (2, 3…), po potrebi i `versionName`.
- Build, upload u isti interni track, nove release notes.

## Pre zatvorenog testa i produkcije (kasnije)

- Novi lični nalozi: zatvoreni test sa najmanje 12 testera prijavljenih 14 dana bez prekida, pa zahtev za produkciju.
- Rešiti prava na stvarna imena (spec §18) i nazive u probnom košarkaškom paketu (NBA, EuroLeague).
- Uključiti R8 (`isMinifyEnabled = true`) sa pravilima za kotlinx-serialization i proveriti veličinu.
- Ako stignu Firebase, server ili reklame: ažurirati politiku privatnosti i Data safety.
