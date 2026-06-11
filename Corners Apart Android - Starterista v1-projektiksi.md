# Corners Apart Android - Starterista v1-projektiksi

## Summary

Muunnetaan nykyinen `android-kotlin-starter` Corners Apart -projektiksi speksin `corners_apart_android_spec_reviewed.md` perusteella. Speksi on kanoninen lähde; muita mukana olevia sääntö/prototyyppidokumentteja ei käytetä toteutuslähteinä.

Lukitut päätökset:
- `namespace` ja `applicationId`: `com.finnvek.cornersapart`
- App-nimi: `Corners Apart`
- Persistointi: DataStore + kotlinx.serialization JSON, ei Roomia v1:ssä
- Toteutuslaajuus: koko v1 vaiheittain, mutta jokainen vaihe pidetään erikseen buildattavana ja testattavana
- UI-tekstit, helpit ja accessibility labelit englanniksi; käyttäjäviestintä, commit-viestit ja välttämättömät koodikommentit suomeksi

## Key Changes

- Päivitä build-identiteetti: `rootProject.name = "CornersApart"`, `app_name = "Corners Apart"`, signing-prefix `CORNERS_APART`, paketit polkuun `app/src/main/java/com/finnvek/cornersapart/`.
- Korjaa Gradle-pluginpohja: lisää `org.jetbrains.kotlin.android` ja `org.jetbrains.kotlin.plugin.serialization`; pidä Compose/Hilt/KSP; poista Room-riippuvuudet, Room KSP schema-arg, tyhjä `DatabaseModule` ja Room ProGuard -säännöt.
- Pidä `compileSdk = 36`, muuta `minSdk = 26`, `targetSdk = 36`; säilytä Java/JDK 17, koska nykyinen Android toolchain käyttää sitä käytännön baselineksi.
- Tee version refresh ensimmäisenä toteutusaskeleena virallisista lähteistä. Tämän suunnitelman tarkistuksessa vakaat ehdokkaat olivat mm. Compose BOM `2026.05.01`, Kotlin `2.4.0`, Hilt `2.59.2`, Room poistetaan, Nearby `19.3.0`; älä käytä alpha/rc-versioita ilman erillistä syytä.
- Luo repo-muisti, koska sitä ei nyt ole: `memory/MEMORY.md` kuvaamaan arkkitehtuurin nykytila, ja päivitä `AGENTS.md` aina kun moduulit, data flow tai vastuut muuttuvat.

## Public Interfaces And Types

- Luo puhdas Kotlin-engine ilman Android UI -riippuvuuksia:
  - `model`: `BoardSnapshot`, `MutableBoard`, `BonusTile`, `BonusTileLayout`, `PieceDef`, `CellOffset`, `CellPosition`, `Player`, `ScoreBreakdown`, `GameState`, `GameMode`, `Ruleset`, `Move`, `Profile`, `HistoryEntry`, `PlayerScore`, difficulty/style-tyypit.
  - Kaikki save/network-mallit merkitään `@Serializable`; board snapshot on flat `List<Int>`, ei `Array<IntArray>` data classissa.
- Engine-rajapinta:
  - `GameEngine.newGame(config)`, `applyMove(state, move)`, `pass(state, playerIndex)`, `getValidMoves(state, playerIndex)`, `hasValidMove(state, playerIndex)`, `previewPlacement(state, move)`.
  - Palauta eksplisiittinen tulosmalli kuten `MoveResult.Accepted(state, scoreDelta)` / `MoveResult.Rejected(reason)`.
- Multiplayer-first session-rajapinta:
  - `GameSession`, `LocalSession`, myöhemmin `NearbySession`.
  - `GameMessage` JSON-protokollalla: `PlaceMove`, `MoveAccepted`, `MoveRejected`, `Pass`, `FullSync`, `PlayerJoined`, `PlayerLeft`, `GameConfig`, `Ping`, `Pong`.
- UI boundary:
  - `GameViewModel` exposeaa `StateFlow<GameUiState>` ja kertaluonteiset tapahtumat `SharedFlow<GameEffect>`.
  - Compose kerää flowt `collectAsStateWithLifecycle()`-tavalla.

## Implementation Plan

1. **Repository and Build Foundation**
   - Päivitä paketit, manifest, root/app names, strings ja README starterista Corners Apartiksi.
   - Poista käyttämätön Room buildista; lisää serialization plugin ja JSON dependency.
   - Lisää `GameConstants` ja theme tokenit speksin väreille, typografialle, spacingille ja animaatioille.
   - Verifioi: `.\gradlew.bat assembleDebug`, `.\gradlew.bat test`. Nykyinen `test` ei valmistunut 120 sekunnissa, joten ensimmäinen toteutusvaihe alkaa puhtaalla pidemmällä baseline-ajolla.

2. **Pure Game Engine**
   - Toteuta mallit, 21 piece definitionia, orientation-cache, placement validator, start-corner rules, bonus-tile template generation, scoring ja game-end logic.
   - TDD ensin: board equality, orientation uniqueness, `PIECE_COUNT = 21`, `TOTAL_PIECE_CELLS = 89`, first move corner coverage, diagonal-only same-player contact, opponent edge contact allowed, bonus claim +3, completion +10, ranking descending.

3. **Local Session and Playable Core UI**
   - Toteuta `LocalSession`, `GameViewModel`, `GameScreen`, Canvas-pohjainen board, player score bar, controls, selected piece preview ja piece panel.
   - Ensimmäinen pelattava tila: Four-player local state ja Solo shell; computer slots voivat aluksi passata vain, kunnes opponent engine valmistuu.
   - Kaikki icon-only kontrollit saavat content descriptionit ja 48dp touch targetit.

4. **Computer-Controlled Opponents**
   - Toteuta `MoveGenerator`, `MoveEvaluator`, `ComputerOpponentEngine`, 3 tyyliä ja 5 vaikeustasoa.
   - Käytä seeded RNG:tä `GameState.randomSeed`-arvosta; aikabudjetti pakottaa aina laillisen siirron tai passin.
   - Testaa, että jokainen vaikeustaso palauttaa vain laillisia siirtoja eikä ylitä kohtuuttomasti time budgetia.

5. **Game Modes**
   - Lisää Solo, Two-Color Duel, Compact Duel, Three-Player ja Four-Player mode configit.
   - Toteuta color/corner ownership niin, että Two-Color Duelissa pelaaja voi omistaa kaksi väriä, mutta turn order pysyy väreissä 0-3.
   - Compact Duel pidetään speksin kulma-aloituksilla; balanssi merkitään play-testattavaksi ennen releaseä, ei muuteta oletuksena.

6. **Nearby Multiplayer and Pass-And-Play**
   - Lisää `GameProtocol`, host-authoritative validation, lobby, pass-and-play flow, Nearby dependency, manifest/runtime permissions ja reconnection handling.
   - Nearby-terminologia UI:ssa: “Nearby game”, “Create nearby game”, “Find nearby game”.
   - Älä lisää raw Bluetooth/Wi-Fi Direct fallbackia v1:een.

7. **Persistence, Profiles, History, Settings**
   - Toteuta DataStore JSON repositories: saved game, profiles, settings.
   - Lisää local generated avatars: initials, geometric, mosaic, rings; custom local image import vasta kun Photo Picker -polku on erikseen testattu.
   - Toteuta history/stats dialogit higher-is-better scoringilla.

8. **Polish and Release Prep**
   - Lisää haptics, sound toggles, reduced-motion behavior, adaptive portrait/tablet layout, help dialog original wordingilla ja game-over score breakdown.
   - Tee accessibility-audit font scale 1.3x, contrast, TalkBack labels, turn announcements ja bonus announcements.
   - Release-vaiheessa tarkista product identity: ei ulkoisia pelinimiä, ei “AI”-sanaa user-facing-teksteissä, ei remote avatar/network image dependencyä.

## Test Plan

- Unit: engine rules, scoring, bonus layouts, serialization round-trips, ranking, game modes, opponent legal moves.
- UI: Compose screen smoke tests, board rendering state mapping, controls enabled/disabled, dialog content strings.
- Integration/manual: save-resume, pass-and-play, Nearby two-device create/find/connect/move/sync/reconnect.
- Commands per milestone: `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat :app:detekt`, `.\gradlew.bat lint`.
- Älä aja `lc` tai `sc`; käyttäjä ajaa ne itse ja raportit luetaan tarvittaessa `reports/`-kansiosta.

## Assumptions And References

- `corners_apart_android_spec_reviewed.md` on ainoa toteutusta ohjaava speksi.
- AGENTS.md on nyt untracked; toteutusvaiheessa sitä ei hävitetä, vaan päivitetään projektin uuteen arkkitehtuuriin.
- Viralliset tarkistuslähteet: [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16), [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [Compose state](https://developer.android.com/develop/ui/compose/state), [Nearby Connections](https://developers.google.com/nearby/connections/android/get-started), [AGP release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes), [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping), [Kotlin 2.4.0](https://kotlinlang.org/docs/whatsnew24.html).
