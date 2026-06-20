# Corners Apart Android - Jäljellä olevan non-release-työn toteutussuunnitelma

## Summary

Tavoite on viimeistellä kaikki nykyisen checkoutin ei-release-keskeneräiset kohdat: repository-pohjainen ViewModel-tila, save/resume, profiilit ja historia, asetusten oikea persistointi, pelin päättymisen ranking, sound/motion-polish sekä oikea Google Play services Nearby -kytkentä. Release-polku jätetään pois pyynnön mukaisesti: privacy placeholderit, Compact Duel play-test, kahden laitteen Nearby stressitesti ja Play Store data-safety eivät kuulu tähän suunnitelmaan.

Suunnitelma perustuu nykyiseen koodiin ja virallisiin lähteisiin: [DataStore Compose -arkkitehtuuri](https://developer.android.com/topic/libraries/architecture/datastore), [Compose state/UDF](https://developer.android.com/develop/ui/compose/state), [Hilt ViewModels](https://dagger.dev/hilt/view-model.html), [Nearby overview](https://developers.google.com/nearby/connections/overview), [Nearby get started](https://developers.google.com/nearby/connections/android/get-started), [advertise/discover](https://developers.google.com/nearby/connections/android/discover-devices), [manage connections](https://developers.google.com/nearby/connections/android/manage-connections), ja [exchange data](https://developers.google.com/nearby/connections/android/exchange-data).

## Key Changes

- `GameViewModel` muutetaan Hilt-injektoiduksi koordinaattoriksi, joka käyttää `GameRepository`, `ProfileRepository` ja `SettingsRepository` -rajapintoja eikä pidä asetuksia vain muistissa.
- `GameSession`/`LocalSession` laajennetaan restore/save-flowta varten: lisää kyky korvata nykyinen `GameState`, luoda uusi `LocalSession` valitulla vaikeustasolla ja tallentaa keskeneräinen peli jokaisen hyväksytyn siirron/passin jälkeen.
- `SavedGameData` laajennetaan tallentamaan myös `GameSettings`-snapshot, jotta resume-dialogi voi näyttää oikean vaikeustason ja jatkopeli palauttaa saman opponent-konfiguraation.
- `GameSettings.DEFAULT_DIFFICULTY` muutetaan arvoon `3`, jotta uusi settings-pohjainen Solo säilyttää nykyisen `OpponentDifficulty.MEDIUM`-käytöksen. Mapping on lukittu: `1=BEGINNER`, `2=EASY`, `3=MEDIUM`, `4=HARD`, `5=EXPERT`.
- `GameUiState` laajennetaan vähintään kentillä `preferredDifficulty`, `preferredMode`, `history`, `activeProfileName`, `hasSavedGame`, `resumeSummary`, `rankedScores`, `nearbyState` ja `profiles`.
- Profiilit otetaan käyttöön repositoryn kautta: jos profiileja ei ole, ViewModel luo yhden aktiivisen paikallisen oletusprofiilin. Historia kirjataan aktiiviselle profiilille, ei globaaliin tyhjään listaan.
- Game-over ranking käyttää `Scoring.rankPlayers(state)` -tulosta eikä UI-pelaajalistan yksittäispisteiden lajittelua, jotta Two-Color Duelin owner-aggregointi säilyy oikein.
- Nearby toteutetaan Google Play services Nearby Connectionsilla, ei raw Bluetooth/Wi-Fi Direct -fallbackeilla. Topologia: `Strategy.P2P_STAR`, `serviceId = "com.finnvek.cornersapart"`, payload-tyyppi vain `BYTES`, viestimuoto nykyinen `GameProtocol`.
- Nearby-yhteys vahvistetaan käyttäjälle authentication tokenilla. Automaattista hyväksyntää ei tehdä, koska Googlen ohje pitää autentikointia olennaisena suojausrajana.
- `MotionPolicy` kytketään oikeisiin animaatioihin ja soundit lisätään vain paikallisiin ihmispelaajan tapahtumiin, jos `soundEnabled=true`.
- Arkkitehtuurimuutosten jälkeen päivitetään `AGENTS.md`, `memory/MEMORY.md`, `PROJECT.md` ja tarvittaessa `README.md`.

## Implementation Plan

### 1. Settings + Session Factory

- Luo `OpponentDifficultyMapper`, joka validoi ja mapittaa persisted difficulty -numeron `OpponentDifficulty`-arvoksi. Kaikki tuntemattomat arvot clampataan välille `1..5`.
- Luo `LocalSessionFactory`, jonka tehtävä on rakentaa `LocalSession(initialConfig, opponentDifficulty)` eikä ViewModel enää kutsu `LocalSession()` suoraan.
- Muuta `GameSettings.DEFAULT_DIFFICULTY = 3`.
- Lisää `LocalSession.replaceState(state: GameState)` ja vastaava `GameSession.replaceState`, jotta tallennettu peli voidaan palauttaa ilman uuden pelin generointia.
- Testit:
  - difficulty 1..5 mapittuu oikeisiin enum-arvoihin
  - arvot 0 ja 99 clampataan BEGINNER/EXPERT
  - uusi Solo käyttää difficulty 3:lla `MEDIUM`-opponenttia
  - `replaceState` julkaisee uuden boardin, pelaajat ja current turnin

### 2. Repository-Backed GameViewModel

- Vaihda `GameViewModel` konstruktoriksi:
  - `LocalSessionFactory`
  - `GameRepository`
  - `ProfileRepository`
  - `SettingsRepository`
  - testattava `Clock` tai pieni `TimeProvider`
- Poista `private var settings: GameSettings = GameSettings()` ja kerää settings `SettingsRepository.settings`-flowsta `stateIn`/`combine`-mallilla.
- Kaikki settings-mutaatiot tehdään `settingsRepository.updateSettings { ... }`.
- `startGame(mode)` käyttää nykyisiä persisted settings -arvoja: mode tallennetaan `preferredMode`, ruleset pysyy `STANDARD`, difficulty ohjaa uutta `LocalSession`-instanssia.
- Pelin hyväksytyn siirron tai passin jälkeen:
  - päivitä UI state
  - jos peli ei ole ohi, kutsu `gameRepository.saveGame(state, settings, now)`
  - jos peli päättyi, luo history entry, tallenna se aktiiviselle profiilille ja tyhjennä saved game
- Testit:
  - settings toggle säilyy repositoryssa uuden ViewModel-instanssin yli
  - startGame tallentaa preferredMode-arvon
  - hyväksytty siirto tallentaa saved game -datan
  - game over appendaa historian täsmälleen kerran ja clearSavedGame kutsutaan

### 3. Save / Resume UI

- Laajenna `SavedGameData` muodoksi: `gameState`, `savedAtEpochMillis`, `settings`.
- Lisää `ResumeGameDialog`, joka näyttää tallennusajan, pelimuodon, johtajan, pisteet, claimed bonus -määrän ja difficulty-arvon.
- `GameRoute` näyttää resume-dialogin app launchissa, jos `savedGameData.gameState != null` ja käyttäjä ei ole vielä valinnut continue/new game.
- `Continue` kutsuu `viewModel.resumeSavedGame()`, joka palauttaa `LocalSession`-tilan ja settings snapshotin.
- `New game` kutsuu `clearSavedGame()` ja aloittaa `preferredMode`-pelin nykyisillä asetuksilla.
- Testit:
  - saved game näkyy resume-dialogina
  - Continue palauttaa boardin ja turnin
  - New game tyhjentää saved gamen eikä näytä dialogia uudestaan saman session aikana

### 4. Profiles + History

- Luo UI-mallit `ProfileUiState` ja tarvittaessa `ProfileEditorState`.
- Lisää `ProfilesDialog`, jossa voi:
  - nähdä profiilit
  - vaihtaa aktiivisen profiilin
  - lisätä paikallisen profiilin nimellä, väripreferenssillä ja avatar-tyylillä
  - muokata nimeä, väriä ja avatar-tyyliä
- Ei toteuteta custom image importia tässä vaiheessa; v1 käyttää vain `INITIALS`, `GEOMETRIC`, `MOSAIC`, `RINGS`.
- Jos profiileja ei ole, luodaan aktiivinen oletusprofiili `id="local-default"`, `name="Player"`, `active=true`.
- `ProfileRepository.appendHistory` rajaa historian `GameConstants.MAX_HISTORY_ENTRIES` viimeisimpään entryyn.
- `HistoryStatsDialog` saa aktiivisen profiilin historian `GameUiState.history`-kentästä.
- History entry luodaan `Scoring.rankPlayers(state)`-tuloksen perusteella. Aktiivisen profiilin rank vastaa owneria `0`; Two-Color Duelissa owner 0 aggregoi värit 0 ja 2.
- Testit:
  - default profile syntyy tyhjään storeen
  - active profile vaihtuu ja historia kirjautuu oikealle profiilille
  - history trimmaa 50 viimeisimpään
  - HistoryStatsDialog näyttää repositoryn historian eikä tyhjää oletuslistaa

### 5. Settings Dialog Expansion

- Laajenna `GameSettingsDialog` sisältämään:
  - difficulty selector 1..5
  - preferred mode selector nykyisille viidelle moodille
  - sound switch
  - haptics switch
  - reduced motion switch
- `preferredRuleset` pidetään mallissa ja tallennetaan, mutta sitä ei näytetä valintana ennen kuin rulesetejä on enemmän kuin `STANDARD`.
- Kaikki tekstit lisätään `strings.xml`:ään; ei hardcoded UI-tekstejä.
- Testit:
  - difficulty näkyy ja vaihtuu
  - preferred mode näkyy ja vaihtuu
  - togglet säilyvät nykyisissä testeissä
  - invalid difficulty ei päädy UI stateen clampaamattomana

### 6. Game Over Ranking + Stats

- Muuta `GameOverDialog` ottamaan `rankedScores: List<PlayerScore>` eikä itse lajittelemaan `PlayerUiState`-listaa.
- `GameViewModel` laskee ranked scores aina nykyisestä `GameState`sta `Scoring.rankPlayers(state)`-helperillä.
- UI:n player cards voivat edelleen näyttää väri-slotit, mutta game-over ja history käyttävät owner-aggregoitua rankingia.
- Lisää passin jälkeinen `GameEffect.GameOver`, jos pass päättää pelin.
- Testit:
  - Two-Color Duel game-over näyttää kaksi owner-riviä, ei neljää väri-slotia
  - ranking tie-breakerit seuraavat `Scoring.rankPlayers`
  - passilla päättyvä peli kirjaa historian ja emittoi game-over effectin

### 7. Nearby Connections Implementation

- Luo Android-spesifi `NearbyConnectionsCoordinator`, joka wrapaa `ConnectionsClient`-käytön ja tarjoaa ViewModelille:
  - `nearbyState: StateFlow<NearbyUiState>`
  - `currentSession: StateFlow<NearbySession?>`
  - `startHosting(config)`
  - `startDiscovery()`
  - `connectToEndpoint(endpointId)`
  - `acceptPendingConnection(endpointId)`
  - `rejectPendingConnection(endpointId)`
  - `disconnect()`
- Luo testattava `ConnectionsClientFacade`, jotta unit-testit eivät riipu final Play Services -luokista.
- Advertising/discovery:
  - host kutsuu `startAdvertising(localName, serviceId, connectionLifecycleCallback, AdvertisingOptions(P2P_STAR))`
  - guest kutsuu `startDiscovery(serviceId, endpointDiscoveryCallback, DiscoveryOptions(P2P_STAR))`
  - discovery pysäytetään, kun valittu endpoint aloittaa yhteyden
- Authentication:
  - `onConnectionInitiated` tallentaa endpointin nimen ja auth tokenin `NearbyUiState.pendingConnection`-kenttään
  - UI näyttää confirm dialogin
  - Accept kutsuu `acceptConnection(endpointId, payloadCallback)`
  - Reject kutsuu `rejectConnection(endpointId)`
- Payloadit:
  - vain `Payload.fromBytes(GameProtocol.encode(message).encodeToByteArray())`
  - decode tehdään vain BYTES-payloadista
  - decode-virhe asettaa `ConnectionState.FAILED` ja näyttää user-facing errorin
- Host-authoritative flow:
  - host omistaa `NearbySession.host(...)`
  - client lähettää `PlaceMove`/`Pass` hostille `MessageTarget.Host`
  - host käyttää nykyistä `HostGameCoordinator`ia ja lähettää `MoveAccepted`, `MoveRejected`, `FullSync`, `PlayerJoined`, `PlayerLeft`
  - client soveltaa vain hostilta tulevat accepted/full sync -viestit
- UI:
  - `NearbyActions` avaa host/discovery-lobbyn eikä pelkkää permission requestia
  - löydetyt endpointit listataan; käyttäjä valitsee mihin yhdistää
  - lobby näyttää connected/reconnecting players
  - disconnect palauttaa local sessioniin vasta vahvistuksen jälkeen
- Permission flow:
  - siirrä permission launcher `GameRoute`en tai erilliseen Compose-friendly wrapperiin, jotta grant-tulos voi jatkaa oikeaa host/discover-actionia
  - käytä nykyistä `NearbyPermissions.requiredRuntimePermissions`
  - älä lisää SDK 37 `ACCESS_LOCAL_NETWORK` manifestiin ennen kuin target/compile siirtyy 37:ään
- Testit:
  - startHosting kutsuu facadea oikealla serviceId/strategyllä
  - startDiscovery listaa löydetyt endpointit
  - accept/reject kutsuu oikeaa facade-metodia
  - BYTES-payload decode reitittyy `NearbySession.applyRemoteMessage`
  - host broadcast lähettää kaikille connected endpointeille
  - client move ei muokkaa local statea ennen hostin accepted/full sync -viestiä
  - disconnect merkitsee reconnecting playerin ilman crashia

### 8. Sound + Motion Polish

- Lisää `GameSoundPlayer`, joka soittaa vain paikallisten ihmispelaajien tapahtumista:
  - normal placement
  - bonus claim
  - game over optional short cue
- Ääniresurssit tehdään alkuperäisinä lyhyinä raw-resursseina `res/raw/`; ei ulkoisia lisensoituja assetteja.
- Sound dispatch tapahtuu `GameRoute`ssa `GameEffect`-keräyksen yhteydessä, mutta vain jos `state.soundEnabled`.
- Lisää näkyvät animaatiot, jotka kaikki kulkevat `MotionPolicy.durationMillis(...)` kautta:
  - active player pulse
  - score increase highlight
  - bonus claim pulse
  - invalid move shake
  - selected piece scale/highlight
- Reduced motion: duration 0, ei infinite-pulsea.
- Testit:
  - `MotionPolicy` säilyy nykyisissä testeissä ja UI-policy testaa reduced motion -polun
  - `GameSoundPolicy` mapittaa effectit oikeisiin sound eventteihin
  - soundia ei pyydetä, kun `soundEnabled=false`

### 9. Documentation + Project Memory

- Päivitä `AGENTS.md`: uusi ViewModel data flow, session factory, saved game snapshot, profile/history flow, Nearby coordinator.
- Päivitä `memory/MEMORY.md`: sama nykytila tiiviisti, erityisesti ettei ViewModel enää käytä DataStorea ohi repositoryjen.
- Päivitä `PROJECT.md`: poista vanhat “partially wired” -kohdat, jotka toteutuvat, ja lisää mahdolliset aidosti jäljellä olevat release-only-kohdat erilliseen future/release-osioon.
- Päivitä `README.md`, jos käyttötapa tai arkkitehtuurikuvaus muuttuu.
- Commit-viestit suomeksi, esimerkiksi:
  - `test: lukitse pysyvien asetusten odotettu toiminta`
  - `feat: kytke pelitila repositoryihin`
  - `feat: lisää tallennetun pelin jatkaminen`
  - `feat: kytke profiilit ja historia`
  - `feat: toteuta Nearby-yhteysadapteri`
  - `docs: päivitä Corners Apart -arkkitehtuurikuvaus`

## Test Plan

- Aja kapeat testit jokaisen vaiheen jälkeen:
  - `.\gradlew.bat :app:testDebugUnitTest --tests "*SettingsRepositoryTest*"`
  - `.\gradlew.bat :app:testDebugUnitTest --tests "*GameViewModelTest*"`
  - `.\gradlew.bat :app:testDebugUnitTest --tests "*ProfileRepositoryTest*"`
  - `.\gradlew.bat :app:testDebugUnitTest --tests "*Nearby*"`
- Aja laajempi verifiointi vaiheiden lopussa:
  - `.\gradlew.bat test`
  - `.\gradlew.bat assembleDebug`
  - `.\gradlew.bat :app:detekt`
  - `.\gradlew.bat lint`
- Älä aja `lc` tai `sc` toteutuksen aikana automaattisesti. Jos käyttäjä ajaa ne, lue vain `reports/ktlint.txt`, `reports/detekt.txt`, `reports/lint.txt`, `reports/security-code.txt` ja `reports/security-deps.txt`.
- Manuaalinen ei-release smoke:
  - käynnistä appi, tee siirto, sulje/avaa, resume näkyy
  - jatka peliä, passaa/tee game-over, history päivittyy
  - vaihda difficulty, aloita Solo, varmista ettei crashaa
  - avaa Nearby host/discovery yhdellä laitteella sen verran, että permission/lobby state toimii

## Assumptions

- Release-polku jätetään pois täsmälleen pyynnön mukaan.
- Custom local avatar image import jätetään pois tästä kierroksesta, koska nykyinen v1-rajaus sanoo käyttää paikallisia generoituja avatar-tyylejä eikä Photo Picker -polkua ole erikseen päätetty.
- `preferredRuleset` pysyy tallennettuna mutta ei saa UI-valitsinta ennen kuin `Ruleset` sisältää enemmän kuin `STANDARD`.
- Nearby toteutetaan oikeana Play Services -adapterina, mutta kahden fyysisen laitteen stressitestaus jää release/manual-vaiheeseen.
- Toteutus päivittää arkkitehtuuridokumentit samassa muutossarjassa, koska data flow ja session-vastuut muuttuvat.
