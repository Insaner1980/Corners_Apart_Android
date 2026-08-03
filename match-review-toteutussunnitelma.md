# Match Review (“Coach”) – toteutussuunnitelma

> Toteutetaan testivetoisesti viidessä itsenäisesti varmennettavassa vaiheessa. Uusia Gradle-riippuvuuksia, pysyvää tallennusta tai Nearby-tukea ei lisätä.

**Tavoite:** Valmiin paikallisen pelin jälkeen omistaja 0 voi selata ottelua vaiheittain, nähdä omat siirtonsa luokiteltuina, verrata niitä kiinteän valmentajakonfiguraation parhaaseen siirtoon ja saada tarkkuusyhteenvedon.

**Arkkitehtuuri:** Uusi puhdas `review/`-paketti rekonstruoi ottelun moottorin julkisten APIen kautta ja analysoi siirrot taustasäikeessä. `GameViewModel` omistaa väliaikaisen arvostelutilan ja Compose näyttää sen uusissa, pääosin tilattomissa komponenteissa.

**Tekniikka:** Kotlin, coroutines/Flow, nykyinen `GameEngine` ja `MoveEvaluator`, Hilt sekä Jetpack Compose. Nykyiset Compose- ja coroutine-versiot säilyvät muuttumattomina.

## 1. Lukitut rajat, käytös ja rajapinnat

### Ominaisuuden rajaus

- Arvostelu on käytettävissä vain juuri päättyneelle `SessionType.LOCAL`-pelille.
- Nearby-, historia- ja aiemmin tallennettuja pelejä ei voi arvostella.
- Arvostelu, aikajana, eteneminen ja nykyinen askel ovat vain muistissa. Prosessin kuolema saa poistaa ne.
- Arvioitavia ovat kaikki pelipaikat, joiden `Player.ownerIndex == 0`; tämä sisältää Two-Color Duelissa paikat 0 ja 2.
- Muiden omistajien siirrot näkyvät aikajanalla ilman luokitusta.
- Tässä versiossa tutkitaan kaikki `GameEngine.getValidMoves()`-siirrot. `MoveGenerator`-otantaa tai ehdokaskattoa ei lisätä.
- Valmentajan kiinteä referenssi on `OpponentDifficulty.MASTER` + `OpponentStyle.BLOCKER`.
- Arvostelu käyttää suoraan `MoveEvaluator`-pisteytystä. Se ei kutsu satunnaisuutta tai MASTER-tason yksityistä 2-ply-logiikkaa sisältävää `ComputerOpponentEngine.chooseAction()`-polkua.
- `MATCH_REVIEW_PLAN.md` säilyy suunnittelulähteenä; toteutunut arkkitehtuuri päivitetään eläviin projektidokumentteihin.

### Uudet puhtaat mallit

Luo `com.finnvek.cornersapart.review`-pakettiin `MatchReviewModels.kt`:

```kotlin
sealed interface ReviewAction {
    data class Placement(val move: Move) : ReviewAction

    data class Pass(
        val playerIndex: Int,
        val hadValidMoves: Boolean,
    ) : ReviewAction
}

data class ReviewTimelineStep(
    val stateBefore: GameState,
    val action: ReviewAction,
    val stateAfter: GameState,
)

enum class MoveClassification {
    GREAT,
    GOOD,
    INACCURACY,
    MISTAKE,
}

enum class AssessmentReason {
    SCORE_GAP,
    PASSED_WITH_AVAILABLE_MOVES,
}

data class MoveAssessment(
    val classification: MoveClassification,
    val reason: AssessmentReason,
    val playedTotal: Double?,
    val bestTotal: Double,
    val bestMove: Move,
    val relativeGap: Double,
    val accuracy: Double,
    val claimedBonusTileCount: Int,
)

data class MatchReviewResult(
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val accuracy: Double?,
    val classificationCounts: Map<MoveClassification, Int>,
)

data class MatchReviewProgress(
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val analyzedCount: Int,
    val totalCount: Int,
    val runningAccuracy: Double?,
)

sealed interface MatchReviewUpdate {
    data class Progress(val value: MatchReviewProgress) : MatchReviewUpdate
    data class Completed(val result: MatchReviewResult) : MatchReviewUpdate
    data class Failed(val failure: MatchReviewFailure) : MatchReviewUpdate
}
```

- Lisää tyypitetty `MatchReviewFailure`, joka erottaa vähintään virheellisen lopputilan, vuorokohdistuksen poikkeaman, hylätyn historiansiirron, loppupassien poikkeaman, lopputilan epätäsmäävyyden ja odottamattoman analyysivirheen.
- Säilytä virheessä lähdehistorian indeksi ja mahdollinen `MoveRejectionReason`, mutta näytä v1-käyttöliittymässä vain lokalisoitu yleisvirhe.
- Älä lisää `@Serializable`-annotaatioita.

### ViewModelin julkinen tila

Luo `viewmodel/MatchReviewUiState.kt`:

```kotlin
enum class MatchReviewPhase {
    ANALYZING,
    COMPLETE,
    FAILED,
}

data class MatchReviewPlayerUiState(
    val index: Int,
    val name: String,
    val colorIndex: Int,
)

data class MatchReviewUiState(
    val phase: MatchReviewPhase,
    val players: List<MatchReviewPlayerUiState>,
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val analyzedCount: Int,
    val totalCount: Int,
    val currentStepIndex: Int,
    val accuracy: Double?,
    val classificationCounts: Map<MoveClassification, Int>,
    val failure: MatchReviewFailure? = null,
)
```

Lisää `GameUiState`-malliin oletusarvoilla:

```kotlin
val canReviewFinishedGame: Boolean = false
val matchReview: MatchReviewUiState? = null
```

Lisää `GameScreenActions`-rajapintaan:

```kotlin
val onStartMatchReview: () -> Unit = {}
val onReviewStepForward: () -> Unit = {}
val onReviewStepBack: () -> Unit = {}
val onReviewJumpTo: (Int) -> Unit = {}
val onCloseMatchReview: () -> Unit = {}
```

Laajenna `GameOverDialog` valinnaisella `onReviewGame: (() -> Unit)? = null` -callbackilla, jotta nykyiset kutsujat ja testit säilyvät yhteensopivina.

## 2. Toteutusvaiheet

### Vaihe 1: Deterministinen pelin rekonstruktio

**Luo:**

- `review/GameReplayer.kt`
- `review/MatchReviewModels.kt`
- `test/.../review/GameReplayerTest.kt`

**Rekonstruktio:**

1. Hylkää tila, joka ei ole päättynyt tai ei läpäise `hasValidIndexDomains()`-tarkistusta.
2. Muodosta alkutila kopioimalla lopputilan identiteetti ja konfiguraatio:
   - samankokoinen tyhjä `BoardSnapshot`;
   - pelaajien nimet, värit, omistajat, aloituskulmat sekä computer/active-liput säilyvät;
   - `usedPieceIds`, `ScoreBreakdown` ja `passed` nollataan;
   - bonusruutujen sijainnit säilyvät, claim-tiedot nollataan;
   - `currentPlayerIndex = 0`, `turnNumber = 0`, tyhjä historia ja `isGameOver = false`;
   - `ruleset`, `gameMode`, `randomSeed` ja `bonusLayoutId` säilyvät.
3. Ennen jokaista historiansiirtoa passaa nykyistä pelaajaa, kunnes vuoro vastaa `move.playerIndex`-arvoa:
   - tallenna jokainen passaus erillisenä `ReviewTimelineStep`-askeleena;
   - laske `hadValidMoves` moottorin `hasValidMove()`-APIlla ennen passia;
   - rajoita silmukka enintään `players.size`-kierrokseen;
   - keskeytä tyypitetyllä virheellä, jos peli päättyy ennen odotettua siirtoa tai vuoro ei kohdistu.
4. Kutsu historiansiirrolle `GameEngine.applyMove()` ja hyväksy vain `MoveResult.Accepted`.
5. Tallenna sijoitusaskeleen `stateBefore`, `ReviewAction.Placement` ja `stateAfter`.
6. Rekonstruoi historian jälkeen myös viimeiset manuaaliset passit:
   - tätä täydennystä tarvitaan, koska pelin päättävillä passeilla ei ole seuraavaa historiansiirtoa;
   - niin kauan kuin rekonstruoitu tila ei ole päättynyt, nykyisen pelaajan pitää olla lopputilassa `passed == true`;
   - kutsu `GameEngine.pass()`, tallenna askel ja käytä erillistä `players.size`-rajaa;
   - automaattisesti ohitettavasta, siirrottömästä pelaajasta ei luoda passiaskeletta.
7. Hyväksy rekonstruktio vain, jos muodostettu `GameState == finalState` kokonaisuudessaan. Tämä varmentaa laudan ja pisteiden lisäksi historian, vuoronumeron, käytetyt palat, passit, bonusclaimit ja nykyisen pelaajan.

**Testit ennen toteutusta:**

- Vapaaehtoinen passi laillisten siirtojen kanssa syntyy oikeaan kohtaan kahden tallennetun siirron väliin.
- Pelin päättävät passit syntyvät myös silloin, kun `moveHistory` on tyhjä.
- Automaattisesti ohitettu pelaaja ei synnytä passiaskeletta; testifixturen pitää erikseen todistaa, että pelaaja ohitettiin `passed == false` -tilassa.
- Rival-pelaajan nimi/computer-lippu, Two-Color Duelin omistajat ja eksplisiittiset bonusruudut säilyvät.
- Korruptoitu pelaajaindeksi, mahdoton vuorojärjestys ja hylättävä historiansiirto palauttavat oikean virhetyypin.
- Jokaisen onnistuneen testin lopuksi verrataan koko rekonstruoitua lopputilaa alkuperäiseen.

**Varmennus:**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.finnvek.cornersapart.review.GameReplayerTest" --no-configuration-cache --console=plain
```

### Vaihe 2: Siirtoarviointi ja inkrementaaliset tulokset

**Luo:**

- `review/MatchReviewAnalyzer.kt`
- `test/.../review/MatchReviewAnalyzerTest.kt`

**Rajapinta:**

```kotlin
fun analyze(
    finalState: GameState,
    reviewedOwnerIndex: Int,
): Flow<MatchReviewUpdate>
```

- Analyysi suoritetaan kokonaan injektoidulla `CoroutineDispatcher`-instanssilla `flowOn(dispatcher)`-rajauksen takana.
- Luo jokaista `analyze()`-kutsua varten oma `MoveEvaluator`, jotta sen muuttuva välimuisti ei jakaudu rinnakkaisten analyysien kesken.
- Emittoi onnistuneen replayn jälkeen heti `Progress`, jossa aikajana on kokonaan selattavissa ja analysoitujen siirtojen määrä on nolla.
- Emittoi uusi kopio tuloskartasta jokaisen arvioitavan askeleen jälkeen.
- Tarkista coroutine-peruutus jokaisen askeleen välissä.
- Emittoi lopuksi yksi `Completed`; replay-poikkeama tuottaa `Failed`.
- Älä tee seinäkelloaikaan perustuvaa automaattitestiä tai ehdokaskattoa tässä versiossa.

**Pisteytys:**

- Arvioi vain askeleet, joiden liikkuvan pelaajan `ownerIndex == reviewedOwnerIndex`.
- Sijoitusaskeleelle:
  - hae kaikki lailliset siirrot `stateBefore`-tilasta;
  - arvioi pelattu ja kaikki ehdokkaat MASTER/BLOCKER-konfiguraatiolla;
  - valitse suurin `MoveEvaluation.total`;
  - tasatilanteessa valitse deterministisesti pienin `(pieceId, orientationIndex, anchorRow, anchorCol)`;
  - pelattu tasapisteinen siirto saa silti `GREAT`-luokituksen, vaikka ghost-siirroksi valikoituisi toinen tasapisteinen siirto.
- Laske:

```text
gap = max(bestTotal - playedTotal, 0)
relativeGap = gap / max(abs(bestTotal), 1)
```

- Keskitetty `ReviewScoring` omistaa rajat:
  - `relativeGap <= 0.02` → `GREAT`
  - `<= 0.15` → `GOOD`
  - `<= 0.40` → `INACCURACY`
  - muuten `MISTAKE`
- Siirtokohtainen tarkkuus:
  - `bestTotal > 0`: `playedTotal / bestTotal`, rajattuna välille 0–1;
  - `bestTotal <= 0` ja piste-ero käytännössä nolla: 1,0;
  - `bestTotal <= 0` ja siirto on heikompi: 0,5.
- Vapaaehtoinen passi, kun siirtoja oli:
  - `MISTAKE`;
  - `playedTotal = null`;
  - paras tarjolla ollut siirto ja sen pisteet tallennetaan;
  - tarkkuus 0,0.
- Passi ilman laillisia siirtoja näkyy aikajanalla, mutta sitä ei arvioida eikä lasketa tarkkuuteen.
- Kokonaistarkkuus on arvioitujen sijoitusten ja vapaaehtoisten passien aritmeettinen keskiarvo. Jos arvioitavia toimia ei ole, tulos on `null`, ja käyttöliittymä näyttää “No moves to assess”.
- `claimedBonusTileCount` tulee toteutuneen siirron `previewPlacement()`-tuloksesta.
- `classificationCounts` sisältää jokaisen enum-arvon myös nollatuloksella.

**Testit:**

- Paras evaluoitu alkusiirto saa `GREAT`.
- Monominoon perustuva tarkoituksella heikko laillinen siirto saa `INACCURACY`- tai `MISTAKE`-luokituksen; fixturen pitää ensin todistaa piste-ero.
- Rajat 2 %, 15 % ja 40 % testataan täsmälleen sekä juuri rajan molemmin puolin.
- Nolla- ja negatiivisen `bestTotal`-arvon tarkkuussäännöt testataan suoraan.
- Vapaaehtoinen passi saa `MISTAKE`-luokituksen ja 0 % tarkkuuden.
- Pakotettu passi jää ilman arviota.
- Two-Color Duelissa sekä paikka 0 että paikka 2 arvioidaan omistajalle 0.
- Ensimmäinen progress-emissio sisältää aikajanan mutta ei arvioita; seuraavat emissiot kasvavat yksi kerrallaan.
- Sama tila tuottaa kaksi kertaa identtisen parhaan siirron, luokitukset ja tarkkuuden.

**Varmennus:**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.finnvek.cornersapart.review.MatchReviewAnalyzerTest" --no-configuration-cache --console=plain
```

### Vaihe 3: Hilt- ja ViewModel-kytkentä

**Muokkaa:**

- `data/GameRuntimeModule.kt`
- `data/GameRuntimeModuleTest.kt`
- `viewmodel/GameUiState.kt`
- `viewmodel/GameViewModel.kt`
- `viewmodel/GameViewModelTest.kt`

**DI:**

- Tarjoa singleton-`GameReplayer`.
- Tarjoa singleton-`MatchReviewAnalyzer`, jolle annetaan sama `GameEngine` ja `Dispatchers.Default`.
- Analyzer pysyy rinnakkaisturvallisena luomalla evaluatorin analyysikutsun sisällä.
- `data -> review` on tarkoituksellinen DI-poikkeus; `review` ei saa riippua `data`, `viewmodel`, `ui`, Android- tai DataStore-tyypeistä.
- Laajenna `GameRuntimeModuleTest` todentamaan molemmat providerit.

Androidin nykyinen coroutine-ohje suosittelee dispatcherin injektointia ja sitä, että pitkä suspend-työ tekee itse main-safe-siirtymänsä. [Android coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)

**ViewModelin omistama tila:**

Lisää:

- `reviewableFinalState: GameState?`
- `matchReviewUiState: MatchReviewUiState?`
- `matchReviewJob: Job?`

Toiminta:

1. Onnistuneen paikallisen siirron tai passin jälkeen hae `stateAfter`.
2. Jos se päätti pelin, kopioi se `reviewableFinalState`-kenttään ennen `refreshUiState()`-kutsua.
3. Nearby-tila ei koskaan täytä tätä kenttää.
4. `GameUiState.canReviewFinishedGame` on tosi vain, kun muistissa on päättynyt paikallinen lopputila.
5. Uuden, palautetun, daily-, challenge-, Rival- tai Nearby-istunnon käynnistyminen:
   - peruuttaa analyysityön;
   - nollaa lopputilan ja arvostelu-UI:n;
   - estää vanhan coroutine-emission palauttamasta arvostelua uuteen peliin.
6. `startMatchReview()`:
   - on no-op, ellei lopputila ole edelleen paikallinen ja päättynyt;
   - peruuttaa mahdollisen aiemman analyysin;
   - luo heti `ANALYZING`-tilan;
   - muodostaa pelaajien näyttönimet ja näyttövärit olemassa olevalla `ProfileDisplayMapper`-logiikalla;
   - kerää analyzerin Flow’n `viewModelScope`ssa;
   - säilyttää käyttäjän nykyisen askelindeksin progress-emissioiden välillä;
   - muuntaa odottamattomat virheet `FAILED`-tilaksi ja käsittelee `CancellationException`-poikkeuksen peruutuksena.
7. `reviewStepForward()`, `reviewStepBack()` ja `reviewJumpTo(index)` rajaavat indeksin välille `0..timeline.lastIndex`; tyhjällä aikajanalla ne eivät tee mitään.
8. `closeMatchReview()` peruuttaa käynnissä olevan analyysin ja nollaa vain dialogin tilan. Lopputilan lähde säilyy pelin vaihtumiseen asti.
9. Arvostelun käynnistys tai selaus ei kutsu yhtään repositorya.

Liiketoimintatila kuuluu ViewModeliin, mutta yksittäisen ghost-kytkimen kaltaisen esitystilan voi pitää lähimmässä composablessa. [Compose state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)

**ViewModel-testit:**

- Paikallisen pelin päättyminen asettaa `canReviewFinishedGame = true`.
- `startMatchReview()` etenee `ANALYZING` → `COMPLETE` ja tuottaa aikajanan.
- Progress-päivitys ei palauta askelindeksiä nollaan.
- Edellinen/seuraava/hyppy rajautuvat oikein.
- `closeMatchReview()` poistaa dialogitilan.
- Uusi peli poistaa arvostelun ja review-saatavuuden.
- Uusi peli analyysin aikana peruuttaa työn; myöhemmin vapautettu testidispatcher ei palauta vanhaa tilaa.
- Nearby-lopputila ei tarjoa arvostelua, ja `startMatchReview()` on no-op.
- Two-Color Duelin molemmat omistaja 0:n värit päätyvät arvioihin.
- Arvostelu ei luo tallennusta, historiaa tai asetuspäivitystä.

### Vaihe 4: Compose-käyttöliittymä ja jaettu piirto

**Luo:**

- `ui/screens/MatchReviewDialog.kt`
- `ui/screens/ReviewBoard.kt`
- `ui/components/BoardCanvasDrawing.kt`
- `res/drawable/ic_chevron_right_24.xml`
- `androidTest/.../ui/screens/MatchReviewDialogTest.kt`

**Muokkaa:**

- `ui/screens/GameBoard.kt`
- `ui/screens/GameScreen.kt`
- `ui/screens/GamePolishDialogs.kt`
- `ui/components/CandyControls.kt`
- `ui/theme/Tokens.kt`
- `res/values/strings.xml`
- tarvittavat `GameScreenTest`-tapaukset

**Jaettu lautapiirto:**

- Siirrä `GameBoard`-tiedostosta tyhjien solujen, bonusmerkkien, aloitusmerkkien ja varattujen candy-solujen piirto `BoardCanvasDrawing.kt`-tiedostoon.
- Määritä pieni `BoardVisualPlayer(index, colorIndex, startCorner)`-malli, jotta sekä `PlayerUiState` että replayn `Player` voidaan muuntaa samaan renderöintiin.
- Lisää jaettu `Modifier.candyBoardPanel()` laudan panel/clip/border/padding-rakenteelle.
- Siirrä nykyinen solujoukon perimeter-piirto yleiseksi apuriksi, jota sekä placement preview että review-highlight käyttävät.
- `GameBoard`-käytöksen, animaatioiden, drag-logiikan ja visuaalisen tuloksen pitää säilyä ennallaan.
- `ReviewBoard` on täysin read-only: ei `pointerInput`, drag-controlleria tai placement-callbackia.

**ReviewBoard:**

- Normaalitilassa piirrä valitun askeleen `stateAfter`.
- Sijoitusaskeleessa korosta toteutuneen palan solut kirkkaalla review-outline-tokenilla.
- Passiaskeleessa älä korosta soluja.
- “Show best move”:
  - on käytettävissä vasta, kun nykyisellä askeleella on arvio ja `bestMove`;
  - vaihtaa laudan `stateBefore`-tilaan;
  - piirtää parhaan siirron candy-haamuna arvioidun pelaajan näyttövärillä;
  - käyttää keskitettyä alpha-tokenia;
  - palautuu pois päältä aina askelta vaihdettaessa.
- Näin vaihtoehtoinen siirto ei piirry toteutuneen siirron jo varaamien solujen päälle.

**MatchReviewDialog:**

- Käytä `CandyDialog`-komponenttia.
- Näytä:
  - otsikko;
  - analyysin aikana `LinearProgressIndicator`, `analyzedCount / totalCount` ja selausvalmis aikajana;
  - valmis yhteenveto: pyöristetty tarkkuusprosentti ja ei-nollaiset luokitusmäärät;
  - nykyisen askeleen read-only-lauta;
  - askelteksti “Step N of M”;
  - pelaajan nimi, sijoitetun palan lokalisoitu nimi tai passikuvaus;
  - vain arvioidulla askeleella semanttinen statuspilleri;
  - bonusclaimien määrä, jos suurempi kuin nolla;
  - “Show/Hide best move” -painike;
  - ensimmäinen, edellinen, seuraava ja viimeinen -ikonipainike;
  - sulkupainike.
- Käytä olemassa olevaa `ic_skip_next_24`-ikonia viimeiseen askeleeseen ja peilaa se ensimmäiseen.
- Lisää yksi chevron-right-vektori; käytä sitä seuraavaan ja peilattuna edelliseen.
- Poista ensimmäinen/edellinen käytöstä indeksissä 0 ja seuraava/viimeinen viimeisessä indeksissä.
- Analysoimattoman omistaja 0:n askeleen kohdalla näytä “Analyzing this move” luokituksen sijaan.
- Virhetilassa näytä lokalisoitu yleisvirhe ja sulkupainike; älä näytä tyhjää lautaa tai rikkinäisiä kontrollipainikkeita.

**Game over -sisääntulo:**

- Lisää `Review game` täysleveänä `CandyButton`-painikkeena GameOverDialogin sisältöön, ei nykyiseen alarivin kahden napin joukkoon. Näin kolmen leveän napin riviylivuoto vältetään.
- Näytä painike vain, kun callback on annettu.
- `GameScreenContent` omistaa `rememberSaveable(state.isGameOver)`-sidotun `gameOverDismissedForReview`-lipun:
  - Review-painike piilottaa GameOverDialogin välittömästi ja käynnistää ViewModel-analyysin;
  - review-dialogi näytetään, kun `state.matchReview != null`;
  - reviewn sulkeminen palauttaa käyttäjän valmiille pelilaudalle ilman GameOverDialogin uudelleenavausta;
  - uuden pelin `isGameOver = false` nollaa lipun seuraavaa peliä varten.
- Muut GameOverDialogin tulos-, stats- ja play-again-käytökset eivät muutu.

**Komponentit ja tokenit:**

- Lisää `CandyStatusChip`, joka on ei-klikkautuva statuspilleri.
- Jaa sen visuaalinen candy-runko nykyisen `CandyChip`-komponentin kanssa; statuspilleri ei saa `Role.Button`- tai click-semanticsia.
- Lisää `Tokens.kt`-tiedostoon semanttiset alias-tokenit:
  - Great: positiivinen vihreä pari;
  - Good: cyan-perhe;
  - Inaccuracy: bonus/kultainen pari;
  - Mistake: warning-punainen pari;
  - nykyisen siirron outline;
  - parhaan siirron ghost-alpha.
- Älä lisää uusia `Color(0x...)`-arvoja muualla kuin `Tokens.kt`:ssä tai inline-`dp`-arvoja.

**Resurssit:**

Lisää vähintään:

- otsikko, Review game ja Close;
- analysointi-, progress-, valmis-, virhe- ja ei-arvioitavia-siirtoja -tekstit;
- Step N of M;
- Great, Good, Inaccuracy, Mistake;
- luokitusmäärien plurals-resurssit;
- accuracy-prosentti;
- Show/Hide best move;
- ensimmäinen/edellinen/seuraava/viimeinen content description;
- sijoitus- ja passiaskeleen kuvaukset;
- bonusclaimien plural;
- read-only-laudan dynaamiset accessibility-kuvaukset normaalille ja best-move-tilalle.

**Saavutettavuus:**

- Laudalla on yksi kuvaava semanttinen solmu: askel, kokonaismäärä, pelaaja, toiminto ja palan nimi.
- Askelindikaattori on polite live region, jotta painikkeella vaihtaminen ilmoitetaan.
- Jokaisella ikonipainikkeella on yksilöllinen lokalisoitu sisältökuvaus.
- Pois käytöstä olevat rajapainikkeet säilyttävät kuvauksensa ja ilmoittavat disabled-tilan.
- Statuspilleri on tekstiä, ei tekaistu painike.
- Dialogin sisältö säilyy vieritettävänä suurella fontilla.
- Compose-testit etsivät toimintoja semantiikkapuusta, joka on myös Compose-saavutettavuuden perusta. [Compose semantics and testing](https://developer.android.com/develop/ui/compose/testing/semantics), [Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

**Instrumentoidut testit:**

- Review-painike näkyy päättyneessä paikallisessa pelissä mutta ei Nearby-pelissä.
- Painike kutsuu callbackia ja piilottaa GameOverDialogin.
- Analysointitila näyttää progressin ja aikajanan.
- Valmis tila näyttää tarkkuuden, luokituksen ja askeltekstin.
- Ensimmäinen/edellinen ja seuraava/viimeinen ovat oikeissa rajoissa disabled.
- Painikkeiden callbackit saavat oikeat askelindeksit.
- Best move -kytkin vaihtaa tekstin ja saavutettavuuskuvauksen ennen-siirtoa näyttävään tilaan.
- Passiaskel näkyy ilman siirtokorostusta.
- Virhetila voidaan sulkea.
- Review-dialogin sulkeminen ei avaa GameOverDialogia uudelleen.

## 3. Dokumentaatio, varmennus ja hyväksymiskriteerit

### Arkkitehtuuridokumentit

Koska uusi paketti ja data flow ovat arkkitehtuurimuutos, päivitä nykyisiä käyttäjän likaisia muutoksia säilyttäen:

- `AGENTS.md`: `review/`-vastuu, sallitut riippuvuudet, transientti rajaus, omistaja 0, MASTER/BLOCKER ja ei-persistenssiä.
- `memory/MEMORY.md`: sama nykytilan tiivistelmä sekä loppupassien rekonstruktioratkaisu.
- `PROJECT.md`: pakettitaulukko, replay/analyzer/ViewModel/UI-virta, `moveHistory`-passirajoite, testit ja v1-non-goals.
- `CLAUDE.md`: lyhyt vastaava nykyarkkitehtuuririvi, jotta sen “Current Architecture” ei jää ristiriitaan.

Älä korvaa tai palauta dokumenttien nykyisiä commitoimattomia muutoksia.

### Automaattinen varmennus

Aja vaiheittain kohdennetut testit ja lopuksi:

```powershell
.\gradlew.bat :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain
```

Kun emulatori tai laite on käytettävissä:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.finnvek.cornersapart.ui.screens.MatchReviewDialogTest --no-configuration-cache --console=plain
```

Lisäksi:

- `PackageDependencyBoundaryTest` pysyy vihreänä eikä muodostu uutta sykliä.
- serializable-boundary pysyy vihreänä.
- color/token-guardrail pysyy vihreänä.
- nykyinen `GameScreenTest` todistaa, ettei jaetun lautapiirron irrotus muuttanut perusruutua.
- `lc`- ja `sc`-wrappereita ei ajeta; ne kuuluvat käyttäjälle.
- `reports/`-hakemistoon ei kirjoiteta eikä sitä commitoida.

### Manuaalinen smoke

1. Päätä Solo-peli ja avaa review.
2. Varmista, että aikajanaa voi selata analyysin vielä jatkuessa.
3. Tarkista first/previous/next/last, sijoituskorostus, progress ja lopullinen yhteenveto.
4. Vaihda best move päälle: laudan pitää vaihtua ennen-siirtoa-tilaan ja haamun olla laillinen.
5. Tarkista vapaaehtoinen passi siirtojen kanssa: `MISTAKE` ja 0 %.
6. Tarkista peli, joka päättyy useaan passiin: viimeistenkin passien pitää näkyä.
7. Tarkista Rival: vain omistaja 0 arvioidaan.
8. Tarkista Two-Color Duel: paikkojen 0 ja 2 siirrot arvioidaan.
9. Tarkista tavallinen Compact Duel sekä Three/Four Player: muiden omistajien siirrot näkyvät ilman pilleriä.
10. Tarkista portrait/landscape, kompakti/leventynyt leveys, vähintään 1,0× ja 2,0× fontti sekä TalkBack-kuvaukset.
11. Sulje review: GameOverDialog ei palaa, vaan valmis lauta jää näkyviin.
12. Aloita uusi peli: review-tila ja saatavuus katoavat.
13. Käynnistä sovellus uudelleen: reviewta ei palauteta eikä sitä löydy historiasta.
14. Tarkkaile responsiivisuutta; analyysi ei saa jäädyttää UI:ta. Tässä toteutuksessa ei aseteta epävakaata aikarajatestiä eikä aktivoida kandidaattiotantaa.

### Valmis, kun

- Engine-tuotettu paikallinen lopputila rekonstruoituu täsmälleen, mukaan lukien keskellä ja lopussa tapahtuneet passit.
- Kaikki ja vain omistaja 0:n arvioitavat toimet luokitellaan määritellyillä rajoilla.
- Tulokset tulevat inkrementaalisesti taustadispatcherilta ja analyysi on peruutettavissa.
- Review on käytettävissä vain juuri päättyneelle paikalliselle pelille.
- UI on read-only, tokenisoitu, lokalisoitu ja semanttisesti testattu.
- Pysyvää mallia, DataStorea, Nearby-protokollaa tai Gradle-riippuvuuksia ei ole muutettu.
- Kaikki kohdennetut ja täydet suorat Gradle-portit läpäisevät.
- Arkkitehtuuridokumentit kuvaavat toteutunutta tilaa.

## 4. Oletukset ja toimitusrajat

- Käyttäjän kysymyksiin ei saatu vastausta määräajassa, joten lukitut oletukset ovat:
  - vapaaehtoinen passi vaikuttaa tarkkuuteen arvolla 0 %;
  - best move näytetään siirtoa edeltävällä laudalla;
  - reviewn sulkeminen palauttaa valmiille laudalle ilman GameOverDialogia.
- Tarkkuuden puuttuessa UI näyttää tekstin eikä tekaistua 100 % tulosta.
- Reviewn “paras siirto” tarkoittaa v1:ssä kiinteän MASTER/BLOCKER-`MoveEvaluator`-referenssin parasta täyden ehdokasjoukon siirtoa, ei ComputerOpponentEnginen satunnaistettua 2-ply-valintaa.
- Nykyinen likainen työpuu säilytetään. Toteutus ei saa tehdä resettiä, siivota muita muutoksia tai korvata dokumentteja vanhalla HEAD-versiolla.
- Committeja, branchia, pushia tai PR:ää ei tehdä ilman erillistä valtuutusta. Jos commitointi myöhemmin sallitaan, viestit ovat suomeksi ja vaiheet pidetään erillisinä.
