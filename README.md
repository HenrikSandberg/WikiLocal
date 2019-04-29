# WikiLocal
Denne applikasjonen viser artikler rundt deg på norsk. Hvis du trykker på en artikkel så åpner den opp og viser deg innholdet. Oppe til høyre på artikkelen så er det et hjerte. Dersom du trykker på det så lagrer du artikkelen lokalt på telefonen din for senere lesning.  

## Struktur
Denne applikasjonen er strukturert rundt at MainActivity holder orden på fragmenter samt håndtering av overgang til Applikasjonens andre aktivitet Artikkel viseren. 

## Kode filer
### ViewPagerAdapter
Dette er en simple klasse som inneholder logikken for å kunne presentere to fragmenter ved siden av hverandre. Dette gjør det derfor mulig å sveipe mellom de to fragmentene og det føles som om de lever kontinuerlig ved siden av hverandre. Den blir brukt av MainActivity og kunne streng tatt vært en privat indre klasse, men jeg lot den få en egen fil da MainActivity ble veldig lang uten den.

### MainActivity
Dette er prosjektets største fil. Den håndterer mye forskjellige, men det er essensielt at alt går nettopp igjennom denne aktiviteten. Det er vesentlig mye mindre Kontroller logikk i dette prosjektet da denne applikasjonen kaller på mange fler eksterne biblioteker. 

#### MainActivity sine ansvarsområdet
- Håndtere Fragmenter
- Kalle på lokasjonsdata
- Varsle bruker dersom appen trenger tilgang til hardware
- Håndtere data som kommer tilbake fra HTTP kall
- Håndtere bilder som blir tatt, og sending av bilder til Firebase
- Håndtering av tillatelser 
- Gjøre kall til Wikipedia for data
- Håndtere overgangen til Artikkel View

#### MainActivity Forlengelser
 NearYouFragment.OnNearYouFragmentInteractionListener,
 SavedArticleFragment.OnListFragmentInteractionListener,
Dette er tilfellet da den har ansvaret for begge Fragmentene i prosjektet. Begge fragmentene inneholder i tillegg hver sin implementasjon av et recycler view, mer om disse etterpå. 

#### Viktige Metoder
Siden dette er en Aktivitet som gjør mye vil jeg bare dra frem noen av de mer komplekse metodene den gjør her og forklare dem. Jeg har også lagt inn en del kommentarer for å forklare kode der jeg anså det som nødvendig. Det vil si, jeg anså at navngivingen min ikke var tilstrekkelig. 

private fun recognizeImage(bitmap: Bitmap)
Dette er metoden som generer et Http kall først til Firebase for å få tilbake fra Firebase vision cloud landmark detector om den gjenkjenner et landemerke i bildet som er tatt. Om den ikke gjør det så returner den ingen ting. 

Dersom den finner landemerke så looper denne metoden gjennom å finner det landemerket  med høyest sannsynlighet for å være korrekt og tar vare på den sin latitude og longitude data. Dette blir så brukt til å søke på wikipedia etter denne artikkelen. 

fun requestArticles(tag: String?)
Denne metoden bruker MainActivity sin instans av klassen `DataRequests` til å søke etter artikler i nærheten dersom de globale latitude og longitude variable har fått en verdi. Hvis ikke det er tilfellet så vil den gi bruker en AlertDialog, enten i form av å be om GPS tilgang eller om å be bruker aktivere nettverk slik at den kan gjennomføre jobben sin. Dersom bruker ikke tillater dette så skrur appen seg av. 

private fun updateLocation()
Brukes til å oppdatere lokasjonsdata. Det gjøres da appen blir startet opp for så gjentatte ganger med relativt lange intervaller mellom hver gang det skjer. Du kan også manuelt oppdatere listen med å dra ned en spinner slik at lista oppdateres.

override fun onNearYouFragmentInteraction(article: JSONObject)
Mottar et JSONObject som blir gått igjennom for å få ut data som blir brukt til å åpne artikkel aktiviteten. 

override fun onSavedArticleFragmentInteraction(article: Article)
Mottar en artikkel som den splitter opp og sender delene inn i en Intent som sendes til artikkel skjermen. 

### NearYouFragment
Dette er fragmenten som setter opp listen over artikler i nærheten av deg. Den håndterer også en rekke andre mindre ting slik som hvis du rister telefonen som gir den deg en tilfeldig artikkel å lese. Dette gjøres med at fragmenten forlenger `SensorEventListener`. Dette medføre implementasjon av to metoder `fun onSensorChanged(event: SensorEvent?)`og `fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)` sistnevnte er irrelevant for min kode og er derfor ikke håndtert på noen annen måte enn at den er en tom implementasjon i bunnen av fila. Sjekker om eventen var hardere en 2.7f, hvis den var det så kaller den på MainActivity sin metode `onNearYouFragmentInteraction(article: Article)`og sender med en tilfeldig utvalgt artikkel fra den lokale lista `articles: MutableList<JSONObject>`

Den lytter også etter swipeContainer events. 

### SavedArticleFragment
Denne filen inneholder kun standarisert kode generert da man lager et nytt recycler view. 

### ArticleActivity
Denne aktiviteten mottar data som den presenterer til bruker. Den har også en knapp i øvre høyre hjørnet på skjermen hvor den displayer et hjerte. 

fun addArticle()
fun removeSavedArticle()
Dette er to metoder som legger og fjerne en artikkel fra databasen. Den bruker også den lokale metoden `updateFavoriteIcon()`for å vise til bruker om de har lagret artikkelen eller ikke. 

fun lookIfArticleIsSavedAlready()
Leter igjennom den lokale databasen og opdaterer hjerte ikonet dersom artikelen allerede er lagret. 

### DataRequests
Denne model klassen gjennomfører HTTP kall til Wikipedia. Den har en metode `requestArticles(latitude: Double, longitude: Double, tag: String?)` som returnerer 20 artikler i en radius rund latitude og longitude punktet. 

`putArticles(json: JSONArray, tag: String)` Legger artikler inn i sin lokale artikkel database, disse kan senere bli hentet ut gjennom metode kallet `getArticles()`. 

Den siste viktige metoden å skrive om her er da metoden `requestArticle(title: String)`. Denne ber om en spesifikt artikkel basert på tittel. Dette kallet gjøres også til Wikipedia. Metoden kalles hver gang man går fra en liste til Artikkel visningen. 

        MobileAds.initialize(this, "ca-app-pub-9638675442193636~7851936375")
        val mAdView = adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
Genererer reklame på artikkel siden. 

#### Database
Dette er fulgt oppsett etter hvordan foreleser viste oss å lage en lokal SQLite database i Android. Databasen er satt opp med mulighet for Live data som ikke utnyttes til det fulle,  men det er tenkt at dette en gang kunne blitt en app med ekstern database i fremtiden. Den eneste tabellen som blir tatt med er Lagrede artikler tabellen. 

## Styrker
Temaet i appen er veldig klart. Jeg har jobbet hardt for å gi innrykk av en konsis og simpel applikasjon som ikke overvelder bruker med hundrevis av skjermer og alt for mye knapper og rot. Appen skal føles minimalistisk og simple å burke.

Da applikasjonen var i en lukket alpha fikk jeg mye brukbar informasjon fra brukerene mine (venner av meg uten utvikler kompetanse) om hvordan utformingen burde være. Da hoved fokuset i applikasjonen er å lese artikler og å lagre artikler dersom du ikke rekker å lese mens du er i nærheten av gjenstanden så kunne jeg fjerne en rekke unødvendige funksjoner. 

Selv sensoren som i mitt tilfellet er akselerometer føles hjemme da den kan returnere til deg en tilfeldig artikkel dersom du rister telefonen. Dette er også på kanten til å være et easer egg, da dette ikke intuitivt er en funksjon, men om du rister telefonen så får du også tilbake en tilfeldig artikkel.  

## Svakheter
Den største svakheten slik jeg ser det er dataene jeg får tilbake fra Wikipedia. Kvaliteten er meget varierende og den kommer tilbake i form av HTML. Jeg blir derfor nødt til å fjerne noen elementer ved bruk av regex, noe som definitivt ikke er optimalt. 

Den andre svakheten slik jeg ser det er et krasj mellom to API-er jeg bruker. Firebase sitt landmerke gjenkjenning sender tilbake latitude og longitude med lavere nøyaktighet enn hva wikipedia krever for å returnere akkurat den riktige artikkelen. Derfor har jeg implementert koden min nå slik at den velger bare den aller første artikkelen som kommer tilbake en som ikke alltid viser seg å være riktig. Firebase returner også et navn på landemerket, men det er heller ikke på samme form som det Wikipedia krever, i tillegg er det på engelsk noe som da strider med valget jeg valgte å ta at alle Wikipedia artikler er på norsk. Dette for å returnere mest mulig innhold i de artiklene nær skolen hvor jeg har skrevet koden. 

Jeg er veldig sikker på at om jeg hadde hatt en uke til så hadde jeg også løst disse to problemstillingene mer optimalt, men per nå så fungerer dette relativt godt. 

## Arkitektur og flyt
(img)

## Krav til applikasjon
#### ☑️ Tydelig definert konsept og formål
Alle deler føles som det har en klar sammenheng med hverandre. Applikasjonen genererer et helhetlig bilde hvor ingen ting skal stikke seg ut som irrelevant eller utenfor temaet.

#### ☑️ Applikasjonen skal ha en Fragment-arkitektur.
Applikasjonen bruker to fragmenter til å vise de forskjellige listene. Dette er en stor del av all synlig kode bruker kan interagere med.

#### ☑️ Applikasjonen skal gjøre bruk av et eksternt API 
I dette prosjektet er det brukt en rekke forskjellige eksterne API-er
- Wikipedia: GET requests både på sted og på navn
- Picasso: Presentasjon av bilder fra URL
- Volley: Bibliotek for håndtering av HTTP requests og for å lytte etter slike forespørsler. 
- Firebase: Håndterer maskinlæring i skyen
- JSONObject: Håndtering av data fått tilbake fra web.

#### ☑️ Bruk av en lokal database.
Applikasjonen bruker en lokal database til å lagre artikkel data slik at man kan aksessere disse artiklene på et senere tidspunkt. 

#### ☑️ Multimedia innhold
Applikasjonen presenterer bilder og du kan ta bilder. I tillegg til dette la jeg inn musikk i TicTacToe applikasjonen da jeg ikke ønsket å gjøre dette i en applikasjon som skulle publiseres på play store (har dessverre ingen rettigheter til musikk selv).

#### ☑️ Lokasjonsdata 
Applikasjonen er avhengig av lokasjonsdata for å kunne fungere. Dette blir brukt for å kunne viser artikler i området rundt deg som bruker.

#### ☑️ Sensordata.
NearYouFragment bruker akselerometeret for å la bruker få tilbake en tilfeldig artikkel i området rundt seg. 

## Tillegg
### Estetikk
Jeg har benyttet meg av Google design prinsippene Material Design i denne applikasjonen for å presentere data på en fin måte til brukere. Det er farge kontrast mellom kamera knappen og top linja for å dra oppmerksomhet. 

Listene er egen genererte, men de er bygget på kort prinsippet til google og standar liste oppsette de bruker ofte selv med stort bilde til venstre, tittel og en kort tekst etter. 

### Google Play
[Lenke til min applikasjon]. Det å publisere den var veldig verdifullt. Applikasjonen min er per nå installert på 10 enheter. Det koster meg penger hver gang noen gjøre et kall til Firebase så jeg valget å implementere Google AdMob mest av alt for å teste hvordan det var. Svaret er, det var utrolig lett. Kanskje det enkleste av alle ting jeg har gjort så langt da det kommer til Android.

### Bruker test
Jeg gjennomføre bruker tester med venner for å få tilbakemeldinger og for å se om applikasjonen fungerte på de forskjellige android versjonene og hardwarene deres. Den siste versjonen min har fått følgende tilbakemeldinger: 
(img)(img)(img)
