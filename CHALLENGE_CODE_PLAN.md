# Shareable Challenge Codes — Implementation Plan

Status: planned, not started.
Audience: implementing agent (Codex). Read `CLAUDE.md`, `AGENTS.md`, and `PROJECT.md` before starting. When this plan and the source code disagree, the source code wins — verify every referenced symbol before using it.

## Ground rules for the implementing agent

- Do NOT invent problems or refactor unrelated code. Implement the smallest thing that satisfies the acceptance criteria.
- No new Gradle dependencies. No `INTERNET` permission — this feature is deliberately serverless. Sharing happens via the Android share sheet (`Intent.ACTION_SEND`, plain text) and manual code entry; neither needs any permission.
- No new exported components. The release manifest policy test asserts only `MainActivity` is exported. v1 has NO deep links / URI schemes — codes are plain text.
- Comments and commit messages in Finnish (project convention). All user-facing strings in `res/values/strings.xml`. No `Color(0x...)` outside `Tokens.kt` (guardrail test). Run `./gradlew test` after UI changes.
- `GameViewModel.kt` and `GameScreen.kt` are already at their detekt size suppressions — put new logic in NEW files, wire thinly.

## Feature summary

A Wordle-style social loop with zero backend. A player generates a **challenge code** — a short human-typeable string like `CA1-7KQ4-M9X2-R` — that deterministically encodes a game setup (seed + mode + difficulty). A friend with the app enters the code and plays the *exact same board*: same bonus-tile layout, same computer opponents behaving identically for identical inputs. Both share their scores (as text, over any messaging app) and compare.

Everything needed for this already exists: `GameConfig.randomSeed` fully determines the bonus layout (`BonusTileGenerator` + `SeedMixer` are deterministic), and computer opponents are deterministic for equivalent state (seed mixes game seed, turn, player, difficulty, style — see `ComputerOpponentEngine`). The daily challenge and the 20 fixed-seed challenge levels prove the seeded-game path works. This feature only adds: a code format, two entry points, and share text.

Honest determinism claim (reflect this in the UI copy): the *board and opponents* are identical; opponent responses depend on the human's own moves, so two players' games diverge as they play — which is exactly the point. Comparable: final score on the same board at the same difficulty.

## Code format

New pure Kotlin object in `model/`: `ChallengeCode` (encoder + decoder). No `@Serializable` needed — it is a string codec, not persisted state (the package boundary test restricts serialization declarations; a codec object does not declare serialization).

Payload fields:

| Field | Bits | Notes |
|---|---:|---|
| format version | 4 | constant `1` for now; reject unknown versions with a typed error |
| game mode | 3 | index into a FIXED ordinal-independent mapping (do NOT use `GameMode.ordinal` directly — define an explicit stable map in `ChallengeCode` so enum reordering never breaks codes) |
| difficulty | 3 | 1–6, clamped via existing `OpponentDifficultyMapper` semantics |
| seed | 32 | random 32-bit value generated at share time, widened to `Long` for `GameConfig.randomSeed` |
| checksum | 5 | over all previous bits (simple CRC-5 or a documented sum-fold; anything deterministic and tested) |

Total 47 bits → 10 characters in Crockford Base32 (0-9 A-Z minus I, L, O, U; decoding accepts lowercase and maps `O→0`, `I/L→1`). Render grouped for readability: `CA1-XXXX-XXXX-XX` where `CA1` is a fixed human-readable prefix (app + version tag) that is validated but not part of the bit payload. Decoder strips whitespace/hyphens and the prefix before parsing.

Decoder returns a sealed result: `Valid(config fields)` / `Invalid(reason)` — never throws for user input. Reasons: bad length, bad character, bad checksum, unknown version, unknown mode.

v1 supported modes: `SOLO` only (three deterministic computer opponents make the score comparable; duels vs. nothing are meaningless solo). The mode field exists so later versions can add more without a format break.

Version compatibility note for the file header docs: codes are only guaranteed comparable across devices running app versions with identical `BonusTileGenerator` templates and opponent logic. The format version field is the escape hatch; bump it if determinism-affecting logic changes.

## Game flow wiring

`GameViewModel` additions (thin; helper logic in a new file, e.g. `viewmodel/SharedChallenge.kt` or inside the codec's companion where it fits naturally):

- `createSharedChallenge()`: generate a random 32-bit seed, build the code with current mode=SOLO and the profile's current preferred difficulty, start a Solo game via the existing local-session start path with `GameConfig(randomSeed = seed, mode = SOLO)`, and expose the code string in `GameUiState` (one new nullable field, e.g. `activeChallengeCode: String?`).
- `startSharedChallenge(rawCode: String)`: decode; on `Invalid`, surface the typed reason as a one-shot `GameEffect` (follow the existing rejection-effect pattern; localized message per reason). On `Valid`, start the identical game and set `activeChallengeCode`.
- `activeChallengeCode` is cleared when any other game type starts. It survives to the game-over state so the result share text can include it.
- IMPORTANT — verify before coding: check how `startDailyChallenge()` builds its `GameConfig` and start the shared-challenge game through the SAME path so difficulty handling matches. Note the known issue in `PROJECT.md`: `HistoryEntry.difficulty` records the global setting, and the daily "Play again" replays plain Solo — do NOT inherit that second bug: game-over "Play again" for a shared challenge must restart the SAME code (same seed), mirroring what the daily flow *should* do. Keep the fix scoped to the new flow; do not silently change daily behavior.

Difficulty: the code carries its own difficulty (from the sharer). The receiver plays at the sharer's difficulty regardless of their local setting — that is what makes scores comparable. Make this explicit in the pre-game UI ("Difficulty 4 — set by the challenger").

## Persistence (minimal, optional but recommended)

Mirror the existing `dailyBestScores` pattern exactly (`Profile` + `ProfileRepository.recordDailyBest`): add `challengeCodeBests: Map<String, Int>` to `Profile` (code → best score), capped at the 30 most recently touched codes, best-only-improves, written through a new `ProfileRepository.recordChallengeCodeBest(profileId, code, score)`. This lets the game-over dialog show "Your best on this challenge: 87" on replays. Persistence JSON ignores unknown keys, so the new field is migration-safe — but keep a default value (`emptyMap()`) so old stores deserialize.

If this adds friction, it may be cut from v1 without harming the core loop — the share text is the product.

## UI (new files under `ui/screens/`, wiring only in existing files)

Entry points inside the existing `ChallengeDialog` (it already hosts daily + levels; add a third section "Friend challenge" with two `CandyButton`s):

1. **Challenge a friend** → calls `createSharedChallenge()`, closes the dialog, game starts. The code is shown in a small dismissible banner/chip near the header during play AND on the game-over dialog.
2. **Enter a code** → opens new `ChallengeCodeEntryDialog` (`CandyDialog`): a text field (auto-uppercase, monospace-ish, accepts pasted text with hyphens/spaces), inline validation error from the decoder's typed reason, and a start button. Keyboard type: visible password / ascii-capable to avoid autocorrect mangling codes.

Share actions (UI layer only — an `Intent.ACTION_SEND` with `EXTRA_TEXT` fired from the composable's context; no ViewModel involvement beyond providing the text pieces):

- During play / pre-game: share the invitation — localized template, e.g. `"Corners Apart challenge! Beat my board: CA1-XXXX-XXXX-XX"`.
- On game over of a shared-challenge game: share the result — e.g. `"I scored 84 (difficulty 4) on Corners Apart challenge CA1-XXXX-XXXX-XX — your turn."` Use the owner-0 aggregated score from the existing ranked results (`Scoring.rankPlayers` output already drives the game-over dialog; reuse the same displayed value, do not recompute).
- Also provide a "Copy code" action (clipboard, no permission needed).

All template strings in `strings.xml` with placeholders. No emojis in v1 share text (keeps encoding trivial everywhere); a decorative result grid can be a later iteration.

Accessibility: the code chip gets a content description reading the code character-by-character grouped ("C A 1, 7 K Q 4, ..."), share/copy buttons get descriptions, entry field gets a label. Follow existing patterns in the challenge/Rivals dialogs.

## Tests (JVM)

1. `ChallengeCodeTest`:
   - round trip: encode → decode returns identical fields, for boundary seeds (0, -1 as unsigned max, random sample) and all difficulties 1–6;
   - decoder tolerance: lowercase, hyphens, whitespace, `O/I/L` substitutions all decode;
   - corruption: every single-character mutation of a valid code fails checksum or produces `Invalid` (property-style loop over positions is fine);
   - unknown version and unknown mode → typed `Invalid`, never an exception.
2. Determinism: two `GameEngine.newGame` calls from the decoded config produce identical bonus layouts and player setups (this mostly re-asserts existing `SeedMixer` tests — one integration-level assertion is enough).
3. `GameViewModel` tests: `startSharedChallenge` with a bad code emits the error effect and starts nothing; with a good code starts a Solo game whose state has the decoded seed; `activeChallengeCode` set/cleared correctly; game-over of a shared challenge exposes the code for sharing; "Play again" restarts the same seed.
4. If persistence is included: `ProfileRepository.recordChallengeCodeBest` improves-only + cap behavior (mirror the existing daily-best tests — and note `PROJECT.md` flags that some progression mutators lack direct tests; do not copy that gap for the new one).
5. Existing guardrail suite stays green: `./gradlew test`.

## Phases

1. `ChallengeCode` codec in `model/` + full codec tests. (Pure, verifiable alone.)
2. ViewModel flows (`create`/`start`/replay-same-seed) + tests.
3. UI: ChallengeDialog section, code entry dialog, code chip, share/copy actions, strings.
4. Optional: per-profile best persistence + tests.
5. Full `./gradlew test` + manual smoke: generate a code on one device/emulator, type it on another (or fresh profile), confirm identical bonus layout visually, play both, compare share texts.

## Explicit non-goals (do not build these)

- Any server, leaderboard sync, or link-based deep linking.
- Nearby integration (this is an asynchronous, offline feature by design).
- Encoding full game results or board states into codes — only the setup is encoded.
- QR codes, images, or emoji result grids (possible later; not v1).
- Supporting duel/multiplayer modes in codes (format reserves space; v1 is Solo only).
