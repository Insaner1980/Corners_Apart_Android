# Rivals — Opponent Character Roster

Date: 2026-07-20
Status: approved direction ("character roster" pitch accepted; details delegated)

## Goal

Turn the invisible AI personality system (`OpponentStyle` × `OpponentDifficulty`)
into visible, named opponents the player challenges one by one. Converts existing
engine work into content, emotional stakes, and replayability.

## Concept

A ladder of **12 rivals**, two per difficulty tier (Beginner → Master). Each rival
is a fixed `OpponentStyle` + `OpponentDifficulty` combo with a name, a candy-style
drawn avatar face, a one-line tagline, and a per-profile win/loss record.
Beating a rival (rank 1) unlocks the next one. Matches are 1-v-1 on the Compact
Duel board (14×14, fast games) with a random seed per match.

### The roster (id, tier, style, color family, tagline)

| # | Name  | Tier      | Style        | Color | Tagline |
|---|-------|-----------|--------------|-------|---------|
| 1 | Jelly | Beginner  | Expansionist | Pink  | "Just happy to be here." |
| 2 | Pip   | Beginner  | Opportunist  | Mango | "Ooh, shiny bonus tiles!" |
| 3 | Sprout| Easy      | Expansionist | Lime  | "Growing in every direction." |
| 4 | Coco  | Easy      | Blocker      | Pink  | "Your corner? My corner." |
| 5 | Dash  | Medium    | Opportunist  | Cyan  | "Fast hands, faster points." |
| 6 | Fig   | Medium    | Blocker      | Lime  | "Every wall has a purpose." |
| 7 | Blaze | Hard      | Expansionist | Mango | "Watch me take the whole board." |
| 8 | Luna  | Hard      | Opportunist  | Cyan  | "I saw that bonus three turns ago." |
| 9 | Onyx  | Expert    | Blocker      | Pink  | "You shall not pass. Anywhere." |
|10 | Nova  | Expert    | Expansionist | Cyan  | "Expansion is an art form." |
|11 | Vex   | Master    | Opportunist  | Mango | "I already know your next move." |
|12 | Sol   | Master    | Blocker      | Lime  | "The final corner is mine." |

Names are code constants (same precedent as `GameConstants.PLAYER_NAMES`);
taglines and all other UI copy live in `res/values/strings.xml` (English).

## Architecture

- **`opponents/OpponentCharacter.kt`** — pure Kotlin: `OpponentCharacter(id, name,
  style, difficulty, colorIndex)` + `OpponentRoster` object (ordered list, lookup,
  `isUnlocked(id, wins)` ladder logic). No Android deps.
- **`model/Profile`** — new fields `rivalWins: Map<String, Int>` and
  `rivalLosses: Map<String, Int>` (default empty; JSON-forward-compatible).
  `ProfileRepository.recordRivalResult(profileId, rivalId, won)`.
- **`multiplayer/LocalSession`** — new optional `opponentStyleOverride:
  OpponentStyle?` passed to `chooseAction` (applies to all computer slots; a rival
  match has exactly one). `LocalSessionFactory.createRivalMatch(config, character)`.
- **`viewmodel/GameViewModel`** — `startRivalMatch(rivalId)`: builds a Compact
  Duel config where slot 1 is computer-controlled, named after the rival, colored
  with the rival's color family (adjusted so it never collides with the active
  profile's display color under `ProfileDisplayMapper`'s 0↔profileColor swap).
  Tracks `activeRivalId`; on game over records win/loss and exposes
  `RivalMatchResult` to UI state.
- **`GameUiState`** — `rivals: List<RivalUiState>` (id, name, tier, style,
  colorIndex, wins, losses, unlocked), `activeRivalId`, `rivalResult`.

## UI (candy bevel language, must wow)

- **Entry**: header button row becomes Challenges / Rivals / Nearby (three
  `CandyButton`s; Rivals uses Primary style).
- **Rivals dialog** (`RivalsDialog.kt`): vertical ladder of rival cards. Each card:
  56 dp drawn avatar, name, tagline, difficulty pips (filled dots 1–6), W–L record,
  "Defeated" crown badge once beaten. Locked rivals are dimmed with a lock glyph
  and "Beat <previous> first". The next undefeated unlocked rival gets a glowing
  gradient ring ("next challenger").
- **Avatar** (`RivalAvatar.kt` in `ui/components/`): Canvas-drawn candy tile
  (reuses `drawCandyCell`) with a face on top — eyes + style-specific expression
  (Expansionist: wide smile; Opportunist: smirk + raised brow; Blocker: flat
  determined mouth + straight brows), Master-tier rivals get a small gold crown
  (`BonusAccentBright`). All colors from `Tokens.kt` / player palette (guardrail:
  no `Color(0x` outside Tokens).
- **VS intro overlay**: on rival match start, a ~1.8 s full-screen overlay —
  player chip slides in from the left, rival avatar from the right, big "VS"
  pops in the middle, then fades. Tap to skip.
- **In-game**: rival's name flows through existing player name plumbing (slot
  name = rival name), so score bar and "X's turn" work for free.
- **Game over**: rival matches get a result headline ("You beat Jelly!" /
  "Jelly wins this time") and, on first-ever win, "New rival unlocked: <name>".
  Existing confetti covers celebration.

## Sounds

Reuse existing SoundPool samples (place/bonus/game over). No new assets.

## Testing

- `OpponentRosterTest`: 12 unique ids/names, tiers non-decreasing and covering
  Beginner→Master, colors in range, unlock ladder (first always unlocked, next
  unlocks on ≥1 win, no skipping).
- `LocalSessionTest` addition: style override plumbs through to the engine.
- `ProfileRepository` / serializer tests: rival fields persist and default safely.
- Existing guardrail tests (`./gradlew test`) must stay green.

## Out of scope (v1 of this feature)

New audio assets, rival rematches with modifiers, rival-specific board layouts,
achievements for rivals (can be follow-up), animations beyond the VS intro and
existing board effects.
