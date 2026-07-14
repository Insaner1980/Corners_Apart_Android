# Corners Apart: Comprehensive Code Review Questions for Codex

## Build System and Gradle

### Question 1

```
Verify settings.gradle.kts repository policy: plugin resolution must use google, mavenCentral, and gradlePluginPortal with content filters (Android and AndroidX plugins from google; ktlint, detekt, Foojay, OWASP, and Sonar plugins from gradlePluginPortal; those plugin groups excluded from mavenCentral), dependency resolution must use only google and mavenCentral, and repositoriesMode must be FAIL_ON_PROJECT_REPOS. Report any drift or gap that could let an artifact resolve from an unintended repository.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 2

```
Review plugin application in app/build.gradle.kts: AGP 9 built-in Kotlin must be in use, so confirm there is no org.jetbrains.kotlin.android, no kotlin-android, no kapt, and no android.builtInKotlin=false anywhere in the build. Also verify the Compose compiler plugin, Kotlin serialization plugin, Compose Stability Analyzer plugin, KSP, Hilt, ktlint, detekt, and JaCoCo are applied without conflicts.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 3

```
Check gradle/libs.versions.toml for internal consistency: are Kotlin 2.4.0, KSP 2.3.9, Hilt 2.59.2, Compose BOM 2026.05.01, AGP 9.2.1, coroutines 1.11.0, and kotlinx.serialization 1.11.0 mutually compatible according to their declared requirements? Also flag any version declared in the catalog but never used, and any dependency declared outside the catalog.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 4

```
Review gradle.properties: configuration cache enabled, build cache enabled, parallel execution disabled, worker max 2, AndroidX enabled, non-transitive R classes, R8 full mode. Check whether any properties contradict each other or the documented build behavior, and whether anything the build implicitly relies on is missing.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 5

```
Verify the OWASP Dependency-Check conditional application logic in app/build.gradle.kts: when Gradle configuration cache is requested, a fallback dependencyCheckAnalyze task must be registered that fails with guidance; when it is not requested, the real org.owasp.dependencycheck plugin must be applied. Check how the code detects that configuration cache is requested, and whether any scenario exists where neither path or both paths activate.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 6

```
Check the JaCoCo report task jacocoDebugUnitTestReport: does it correctly include Java classes, Kotlin debug classes, and AGP built-in Kotlin class paths, exclude generated Android classes, tests, previews, composable singleton artifacts, and di as documented, and does the XML output path exactly match what the Sonar configuration reads?

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 7

```
Review the root build.gradle.kts sonar wiring: the root sonar task depends on :app:assembleDebug and :app:jacocoDebugUnitTestReport. Verify the dependency wiring is correct and free of configuration-time side effects that could break configuration cache for unrelated tasks.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 8

```
Verify Gradle dependency verification: gradle/verification-metadata.xml must require metadata and signature verification with SHA-256 for every artifact. Confirm the build actually enforces verification, and confirm DependencyVerificationPolicyTest genuinely rejects trusted-artifacts entries and wildcard or regex trusted-key scopes rather than being trivially satisfiable.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 9

```
Search app/build.gradle.kts for logic that executes at configuration time (environment variable reads, task graph inspection in the signing gate, conditional plugin application) and verify each is written in a configuration-cache-safe way, including the documented environment-change scenario.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 10

```
Check the Gradle wrapper setup: gradle-wrapper.properties should match Gradle 9.5.1. Verify whether a distributionSha256Sum is set and whether wrapper integrity is validated anywhere (CI step or test). Report only concrete gaps.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 11

```
Verify the absence of intentionally unused dependencies: PROJECT.md states there is no Navigation Compose, no Hilt Navigation Compose, no DataStore Preferences, no Compose Animation dependency, and no material-icons-extended package. Search build files and source imports for any accidental usage that contradicts this, and confirm the hygiene tests guarding these actually check the right things.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 12

```
Review Kotlin compiler options in app/build.gradle.kts: JVM target 17 consistency between Kotlin and Java compilation, and any freeCompilerArgs. Flag only concrete misconfigurations.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Build Types and Release Signing

### Question 13

```
Review the release signing gate task-name matcher: it explicitly covers assembleRelease, bundleRelease, packageRelease, packageReleaseBundle, signReleaseBundle, and packageReleaseUniversalApk, plus names ending in Release that start with assemble, bundle, package, or publish. Verify against the actual AGP 9.2.1 task graph of this project whether any artifact-producing release task escapes the matcher, and whether the matcher ever false-positives on pure verification tasks.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 14

```
Verify the signing gate correctly allows compileReleaseKotlin, detektRelease, and lintRelease without signing environment variables, and that the gate evaluates reliably under configuration cache, including the documented configuration-cache environment-change scenario that unit tests dry-run.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 15

```
Check release build type configuration: isDebuggable=false, isMinifyEnabled=true, isShrinkResources=true, default optimized ProGuard file plus app/proguard-rules.pro. Review proguard-rules.pro for missing keep rules that could break kotlinx.serialization, Hilt, Compose, or Play Services Nearby in an R8 full-mode release build at runtime.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 16

```
Verify the four CORNERS_APART_* signing environment variables are read safely: values must never be logged, printed in task output, or serialized into configuration cache entries or build reports in plain text.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 17

```
Confirm the release identity guardrail tests actually assert applicationId and namespace com.finnvek.cornersapart, version code 1, version name 1.0.0, and the app label, in a way that would fail on real drift rather than re-deriving expected values from the same source they test.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Android Manifest and Permissions

### Question 18

```
Review AndroidManifest.xml permission declarations against the documented SDK bands: ACCESS_WIFI_STATE and CHANGE_WIFI_STATE max SDK 31, BLUETOOTH and BLUETOOTH_ADMIN max SDK 30, ACCESS_COARSE_LOCATION unbounded, ACCESS_FINE_LOCATION min 29 max 31, BLUETOOTH_ADVERTISE and BLUETOOTH_CONNECT min 31, BLUETOOTH_SCAN min 31 with neverForLocation, NEARBY_WIFI_DEVICES min 32 with neverForLocation. Verify these match what Play Services Nearby 19.3.0 requires for target SDK 37 and flag concrete mismatches only.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 19

```
The runtime permission policy adds android.permission.ACCESS_LOCAL_NETWORK for SDK >= 37, but AndroidManifest.xml does not declare that permission. Verify in code what actually happens when the app requests an undeclared permission at runtime on an SDK 37 device, whether Nearby functionality breaks, and what the minimal correct fix is (manifest declaration, uses-sdk attributes, or policy change).

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 20

```
Verify application security flags: allowBackup=false, fullBackupContent=false, dataExtractionRules pointing at the exclusion XML, usesCleartextTraffic=false. Then check app/src/main/res/xml/data_extraction_rules.xml excludes root path "." for both cloud-backup and device-transfer sections with schema that is valid for target SDK 37.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 21

```
Confirm MainActivity is the only exported component and that no library manifest merged into the final manifest introduces additional exported components, services, receivers, or providers. If the merged manifest cannot be produced, inspect the dependencies known to add components.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 22

```
Evaluate ACCESS_COARSE_LOCATION having no maxSdk bound: on SDK >= 32 Nearby uses neverForLocation Bluetooth and NEARBY_WIFI_DEVICES, so check whether the unbounded coarse-location declaration is actually required by the runtime policy for older bands only, and whether adding a maxSdk would be correct or would break SDK 31-and-below devices.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 23

```
Check the manifest theme (platform Material Light NoActionBar) together with enableEdgeToEdge and the Compose Material 3 theme: verify there is no status bar, navigation bar, or splash inconsistency on the min SDK 26 baseline and on SDK 37.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 24

```
Verify no build-type or source-set manifest overrides weaken the security flags, and confirm the manifest security policy test actually reads all source-set manifests rather than only the main one.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 25

```
Check the launcher icon setup: adaptive icon XML under mipmap-anydpi-v26, monochrome drawable, and colors.xml background and foreground entries. Report only concrete rendering or configuration problems for API 26 and themed-icon devices.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Application Entry

### Question 26

```
Review App.kt and MainActivity.kt: @HiltAndroidApp and @AndroidEntryPoint correctness, enableEdgeToEdge call placement, and the Compose content structure of CornersApartTheme, Surface with fillMaxSize, and GameRoute. Report only real lifecycle or initialization problems.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 27

```
The Nearby runtime permission launcher lives in GameRoute rather than MainActivity. Verify the launcher is created with the proper Compose API so it survives recomposition and activity recreation, and that a pending host or discover intent is not silently lost across configuration change or process death.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 28

```
Check MainActivity for any missing intent handling, launchMode, or task configuration issues relevant to a single-activity Compose game, including behavior when the activity is relaunched from the launcher while a game is in progress.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Architecture and Package Boundaries

### Question 29

```
Confirm the documented dependency cycle: model depends on engine through ScoreBreakdown.plus(ScoreDelta), and engine depends on model from scoring and rules code. Verify the cycle exists exactly as described, whether it causes any practical problem in the current single-module build, and what the single simplest change would be to break it. Do not propose splitting into Gradle modules.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 30

```
Review GameRuntimeModule living in the data package while providing GameEngine, ComputerOpponentEngine, TimeProvider, ConnectionsClientFacade, and NearbyConnectionsCoordinator: verify scoping annotations, absence of duplicate bindings against PersistenceModule, and that all provided singletons are actually safe as singletons.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 31

```
Audit rule-logic duplication: the guardrail says placement, scoring, ranking, bonus, piece, and turn logic must live only under engine/. Search UI, viewmodel, session, and opponent code for any re-implemented rule fragments (for example diagonal-contact checks or score math) and report exact duplicates only.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 32

```
Verify serializable domain models live under model/ and Nearby protocol messages (GameMessage, GameProtocol) live under multiplayer/ as the guardrails require, and report any strays.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 33

```
Search for dead code across the main source set: unused classes, functions, properties, and constants. In particular check whether the historical corner lists in GameConstants are referenced anywhere now that GameModeConfigs generates corner ownership.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 34

```
Verify the TimeProvider abstraction is used everywhere wall-clock time is needed: search for direct System.currentTimeMillis, SystemClock, LocalDate.now, or Instant.now calls outside SystemTimeProvider and test code, and report violations.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Domain Constants

### Question 35

```
Verify GameConstants values are the single source used everywhere: board sizes 20 and 14, player count 4, piece count 21, total cells 89, bonus counts 10 and 6, bonus points 3, cell point 1, completion bonus 10, max history 50. Search engine, opponents, data, and UI code for duplicated magic numbers that should reference these constants.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 36

```
Review the timing constants (board interaction lock 160 ms, invalid feedback cooldown 180 ms, opponent delay 300 ms plus 400 ms range, turn advance 400 ms, human auto-pass 1500 ms, save notification 2000 ms, reconnect timeout 60000 ms, background timeout 300000 ms): verify each constant is actually consumed somewhere, and check whether any pair of consumed delays can overlap in a way that creates an input race or double-processing.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 37

```
Verify whether the avatar constants (max dimension 160, max file size 5 MiB) are actually enforced in any code path handling customAvatarPath, or whether they are declared but never checked. Report the exact enforcement points or their absence.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Game Modes

### Question 38

```
Verify GameModeConfigs matches the documented table exactly: Solo 20-board 10-bonus 4-slot with owners 0,1,2,3 and computer slots 1,2,3; Two-Color Duel owners 0,1,0,1 with no computer slots; Compact Duel 14-board 6-bonus 2-slot; Three-Player 3 slots; Four-Player default. Also verify the start-corner coordinates per mode are correct for the corresponding board size.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 39

```
Trace Two-Color Duel end to end: turn order must remain color slots 0 through 3 while ranking aggregates by ownerIndex. Follow one full scoring path from engine move application through Scoring.rankPlayers to GameOverDialog and history entries, and confirm owner aggregation is preserved at every step.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 40

```
Verify Three-Player mode uses exactly the first three standard slots and that the unused fourth corner produces no residual behavior: no start marker, no corner ownership, no phantom player in scoring or ranking, correct bonus generation for the full board.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 41

```
Check Compact Duel configuration (board 14, 6 bonuses, 2 slots, starts top-left and bottom-right) and determine how requiresPlayTesting=true is consumed: does any code gate on it, or is it purely informational? Report exactly what reads the flag.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 42

```
Verify Solo mode computer style assignment matches the documented formula: player index mod 3 equal to 1 gives Expansionist, 2 gives Opportunist, otherwise Blocker. Check the actual code expression, including what style player index 0 would get if it were ever computer controlled.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 43

```
Confirm GameModeConfigs is the only source of mode defaults: search for any other location that hardcodes corner ownership, slot counts, computer flags, or default modes that could drift from GameModeConfigs.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Board Model

### Question 44

```
Review BoardSnapshot invariants: cells.size must equal size*size and size must be positive. Verify these are enforced in the constructor or init block, and verify that kotlinx.serialization deserialization of a corrupted or hand-edited save also triggers the validation rather than constructing an invalid board.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 45

```
Verify flat row-major indexing (row * size + col) is used consistently in every board read and write: placement validation, move application, bonus tile checks, board rendering, and tap-to-cell mapping. Specifically look for any transposed col * size + row usage.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 46

```
Review MutableBoard: verify equals and hashCode over the backing IntArray are implemented correctly and consistently with each other, and that conversion to and from BoardSnapshot copies the array rather than sharing a reference that could leak mutations into immutable snapshots.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 47

```
Check MutableBoard bounds behavior: is out-of-bounds access guarded internally or is every caller responsible? Verify no engine path can index outside the array under any validator-approved move.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 48

```
Verify empty-cell handling: BoardSnapshot.EMPTY is -1 and occupied cells store player indexes. Search for any comparison against 0, null, or another sentinel instead of EMPTY.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 49

```
Check CellPosition and CellOffset for consistent row-first, column-second ordering everywhere they are constructed and destructured, and report any call site with swapped arguments.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Piece Catalog and Transforms

### Question 50

```
Verify PieceCatalog integrity: exactly 21 pieces, exactly 89 total cells, all ids unique, and each piece's cell offsets form a connected polyomino of the declared size. Spot-check the shapes against their names, for example five-cross should be a plus shape and four-step should be an S or Z tetromino shape.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 51

```
Verify the SINGLE_CELL_ID, TWO_LINE_ID, and THREE_BEND_ID constants map to existing catalog ids (one-dot, two-bar, three-corner) and check every place these constants are consumed.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 52

```
Review PieceTransforms.normalize: does it produce a true canonical form (minimum row and column shifted to zero and cells in a deterministic order) so that orientation deduplication by equality is reliable?

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 53

```
Verify getAllOrientations builds four rotations of the original cells plus four rotations of a horizontal flip, normalizes, deduplicates, and never exceeds the cap of 8. Check symmetric pieces specifically: one-dot should have 1 orientation, four-block 1, five-cross 1, two-bar 2, and verify the code produces these reduced counts.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 54

```
Review the ConcurrentHashMap orientation cache: verify correct atomic population (computeIfAbsent or equivalent), that stored orientation lists are immutable or never mutated after caching, and that concurrent first access from multiple threads cannot produce inconsistent entries.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 55

```
Verify the rotateCW, rotateCCW, and flipH coordinate math is correct and mutually consistent: rotating clockwise then counterclockwise followed by normalize must be identity, and flipping twice must be identity.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 56

```
Check getOrientation behavior for an out-of-range orientationIndex: exception, clamp, or wrap? Verify the behavior is consistent with the UNKNOWN_ORIENTATION rejection in PlacementValidator so no path can bypass validation via an exotic index.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Game State Model

### Question 57

```
Review the serializable GameState, Player, and Move definitions for default-value hazards: with encodeDefaults=true in the protocol JSON and ignoreUnknownKeys=true in persistence JSON, check whether any field default could silently mask missing data after a schema change, and whether saved games and protocol messages could diverge in interpretation of the same model.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 58

```
Verify ScoreBreakdown: total must be computed from placedCellPoints, bonusTilePoints, and completionBonus rather than stored, and plus(ScoreDelta) must return a correctly summed new instance without mutation. Check ScoreDelta fields align one-to-one with the three categories.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 59

```
Check BonusTile claim semantics: how is an unclaimed tile represented (null claimedByPlayerIndex or a sentinel), and is that representation checked consistently in the validator, scoring, generator, and board rendering?

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 60

```
Verify immutability discipline: Player.usedPieceIds and other collections inside GameState must not be mutated in place by move application; confirm the engine creates new instances so older snapshots held by flows or history remain valid.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 61

```
Assess moveHistory growth: it is stored inside GameState and serialized into saves and FullSync messages. Verify whether it is bounded, estimate the size for a full four-player game, and report whether unbounded growth creates a concrete problem for DataStore saves or Nearby payloads.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Core Rules and Engine

### Question 62

```
Verify GameEngine.newGame(config): correct player list built from the mode config with names, colors, owners, and computer flags; start corners assigned per mode; bonus tiles generated deterministically from the seed; currentPlayerIndex and turnNumber initialized correctly.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 63

```
Review PlacementValidator check ordering: cheap checks (game over, turn, player index, passed, piece known, orientation known) should run before board scans. Also verify the returned rejection reason is the most accurate one for the situation rather than an earlier check masking the real cause the UI should report.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 64

```
Verify the first-placement rule: the placed cells must cover the moving player's own start corner. Confirm the comparison uses the player's assigned corner from the game state, not a hardcoded coordinate, and works on both 20 and 14 boards.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 65

```
Verify same-player edge-contact rejection: the orthogonal neighbor check must skip out-of-bounds neighbors correctly and must only consider cells owned by the moving player, never opponents.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 66

```
Verify the diagonal-contact requirement after the first placement: at least one diagonal neighbor owned by the moving player must exist, diagonal checks must handle board edges without wrapping or crashing, and the rule must combine correctly with the edge-touch rejection (a move with both a diagonal own-contact and an edge own-contact must still be rejected).

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 67

```
Verify opponent contact permissiveness: placements touching opponent cells by edge or corner must be allowed, and no validator branch accidentally rejects them.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 68

```
Verify overlap and bounds validation: every target cell computed from anchor plus orientation offsets must be within bounds and empty. Also confirm the exact same anchor-plus-offset math is used by the UI preview so what the user sees is what the validator checks.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 69

```
Verify pass mechanics: only the current player may pass, pass sets the passed flag and increments turnNumber, and an accepted move clears the mover's passed flag. Then trace game-end detection to confirm that clearing passed on a move cannot incorrectly resurrect a player who should count as finished.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 70

```
Review game-over detection: the game ends when all active-scoring players have passed or have no valid move. Verify the hasValidMove evaluation here cannot miss an end state, for example when the last mobile player becomes blocked by their own move, and check the computational cost of running it at every turn advance.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 71

```
Verify nextPlayerIndex: it must skip inactive, passed, and no-valid-move players, must not advance when the game is over, and must have explicit protection against an infinite loop when no player can act.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 72

```
Verify applyMove atomicity: board cells, the player's usedPieceIds, scoreBreakdown, bonus tile ownership, turnNumber, and moveHistory must all update together in the returned state, with no partially updated state reachable on any rejection path.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 73

```
Verify previewPlacement shares its validation logic with applyMove rather than duplicating rules, and returns preview cells and claimable bonus tiles that exactly match what an actual application of the same move would produce.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 74

```
Verify getValidMoves correctness and completeness: it must enumerate unused pieces times valid orientations times anchors, respect passed state and player validity, and satisfy the invariant that hasValidMove(state, p) is true exactly when getValidMoves(state, p) is non-empty.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 75

```
Verify engine purity: no android.* imports, no logging, no wall-clock or unseeded random usage anywhere under engine/. Report any impurity with the exact location.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Scoring and Ranking

### Question 76

```
Verify Scoring.scoreMove: 1 point per placed cell, 3 points per claimed bonus tile, and a 10-point completion bonus only when the move completes the full 21-piece set. Confirm completion detection counts the player's used pieces after including the current move.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 77

```
Verify bonus claiming: only unclaimed bonus tiles covered by the move's target cells are claimed, claimedOnTurn is set to the correct turn number, and no code path can claim an already-claimed tile or double-count its points.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 78

```
Verify Scoring.rankPlayers owner grouping: filtering to active-scoring players, grouping by ownerIndex, combining score breakdowns within a group, counting claimed bonus tiles by owner, and using the owner name format Player {ownerIndex + 1} only when a group contains multiple color slots.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 79

```
Verify the ranking sort order implementation matches exactly: higher total score, then higher placed-cell points, then higher claimed bonus tile count, then fewer remaining pieces, then lower owner index. Check the comparator chaining carefully for inverted comparisons or missing tiebreakers.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 80

```
Verify the remaining-pieces tiebreaker for multi-slot owners: determine whether remaining pieces are summed, averaged, or taken per slot across an owner's color slots in Two-Color Duel, and whether that choice is consistent with how the other aggregated fields are combined.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 81

```
Check the isActiveScoring flag: identify every place a player can become inactive for scoring, and verify Three-Player mode and Nearby disconnect states cannot interact with the flag in a way that wrongly drops a player from rankings.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 82

```
Verify that GameOverDialog and history entries consume the identical rankedScores produced by one call path through Scoring.rankPlayers, so the displayed final ranking and the stored history ranking can never diverge.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Bonus Tile Generation

### Question 83

```
Verify BonusTileGenerator seed derivation: the template index and the transform index must be derived from the seed deterministically and non-negatively. Check the mixing and modulo math for negative-seed handling and for bias.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 84

```
Verify the four transforms (identity, rotate 180, mirror vertical, mirror horizontal) are mathematically correct for both the 20 and 14 board sizes and always produce in-bounds positions from in-bounds template positions.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 85

```
Check the template positions: the 10 standard positions and 6 compact positions must not collide with any mode's start corners or with each other after any of the four transforms. Also determine whether MIN_BONUS_DISTANCE = 2 is actually enforced anywhere or is a declared-but-unused constant, and report which.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 86

```
Verify count truncation: when the requested bonus count is smaller than the template size, check which positions are dropped and whether the truncated result is still deterministic for a given seed.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 87

```
Verify the layout id format template.id:transform: check whether bonusLayoutId is ever parsed back anywhere (resume, debugging, tests), and if so whether the parsing is robust against the colon in the template id.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Opponent System

### Question 88

```
Verify MoveGenerator ordering is a total deterministic order: larger piece size first, then piece id, orientation index, anchor row, anchor column. Confirm the difficulty candidate soft cap is applied after sorting so the cap selects the intended prefix.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 89

```
Verify MoveCandidate metadata: placed cell count and claimed bonus tile count must be computed with the same logic the engine uses, not with re-implemented rule fragments that could drift.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 90

```
Review MoveEvaluator component math: placed cells, bonus claims, spread away from the player's own start corner, center pressure, and blocking proximity to opponent start corners. Verify distance calculations and any normalization behave correctly on both the 20 and 14 board sizes.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 91

```
Verify the difficulty parameter table is faithfully applied: temperature 3.0/2.0/1.0/0.5/0.2, candidate caps 10/25/80/200/500, time budgets 250/400/700/1200/1800 ms, large-piece bias -0.40 to 0.45, bonus awareness 0.2 to 1.8, blocking awareness 0.0 to 1.7. Check the code constants against this table exactly.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 92

```
Review ComputerOpponentEngine deadline handling: verify a monotonic time source is used, define what happens when the deadline expires mid-evaluation, and confirm at least one candidate always gets evaluated even at the 250 ms BEGINNER budget on a slow device.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 93

```
Verify seeded selection determinism: the seed must mix GameState.randomSeed, turn number, player index, difficulty, and style, and identical inputs must always produce the identical move. Also verify the threshold logic that treats very low temperature as effectively deterministic.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 94

```
Verify the safety chain: the chosen move is revalidated against current state before returning, with fallback to the first valid evaluated move and then to pass. Confirm this chain can never return an invalid move and never loops.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 95

```
Verify OpponentAction.Pass is returned when no candidates exist and that LocalSession translates it into an engine pass for the correct player index at the correct time.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 96

```
Verify dispatcher usage: the selection dispatcher defaults to Dispatchers.Default and must be injectable for tests. Confirm no part of opponent selection runs on the main thread.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 97

```
Review the temperature-weighted selection math: check the weighting or softmax computation for overflow, underflow, or NaN when evaluation totals differ greatly and temperature is 0.2, and verify the weights are used to pick correctly rather than inverted.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 98

```
Verify OpponentDifficultyMapper: persisted values 1 through 5 map to the five difficulties, out-of-range persisted values are clamped, and the default persisted value 3 maps to MEDIUM.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 99

```
Assess candidate-cap interaction with styles: with the BEGINNER cap of 10 applied to a size-descending sort, evaluate whether the Blocker and Opportunist styles ever receive candidates relevant to their heuristics, and report whether this starvation is a real behavioral bug or acceptable intended weakness at low difficulty.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Sessions and Local Play

### Question 100

```
Verify LocalSession rejection translation: engine rejections become IllegalArgumentException with reason.name as the message. Find every consumer that parses that message back into a reason and verify unknown or changed names are handled without crashing.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 101

```
Review the Solo computer-turn loop: it runs until the next current player is human or the game ends. Verify loop exit conditions, the use of the opponent delay constants, cancellation when the session is replaced or a saved game is restored, and that the loop terminates when all computer players are blocked.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 102

```
Verify LocalSession flow publications stay consistent: gameState, players, and connectionState CONNECTED must update together, and SessionPlayer derived fields such as usedPieceCount must refresh on every accepted move and pass.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 103

```
Verify replaceState for saved-game restore: restoring a GameState mid-session must reset players, selection-relevant derived state, and any running computer turn so a stale computer coroutine cannot apply a move computed against the previous state.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 104

```
Verify startNewGame(config) on LocalSession: full reinitialization including a fresh seed and bonus layout, with no state leaking between consecutive games in the same session instance.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 105

```
Review LocalSessionFactory: what it constructs and injects, and whether sessions created by it are correctly disposed or abandoned when GameViewModel starts a new game or switches to a Nearby session.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 106

```
Verify sendPass(playerIndex) layering: what the session validates versus what the engine validates, and what happens if a UI race sends a pass for a player who is no longer current.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 107

```
Verify that LocalSession's always-CONNECTED connectionState is never misread by UI code that renders Nearby connection status, so local play cannot display Nearby-specific connection text.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Nearby Protocol, Host Coordinator, and NearbySession

### Question 108

```
Verify GameProtocol configuration: class discriminator type, encodeDefaults=true, ignoreUnknownKeys=false, and every GameMessage subtype carrying its documented serial name (placeMove, moveAccepted, moveRejected, pass, fullSync, playerJoined, playerLeft, gameConfig, ping, pong). Confirm the unknown-type rejection test exercises a realistic forged payload, not just a trivially invalid string.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 109

```
Assess the protocol strictness tradeoff: ignoreUnknownKeys=false means any added field breaks cross-version play between devices on different app versions. Verify this strictness is handled gracefully at decode time (clean FAILED state, no crash) and report whether a versioning strategy is needed before release, in the simplest possible form.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 110

```
Verify HostGameCoordinator move handling: PlaceMove is applied through GameEngine.applyMove, and MoveAccepted broadcasts the full post-move authoritative state plus a score delta that exactly matches Scoring.scoreMove for that move.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 111

```
Verify rejection routing: MoveRejected must be sent only to the originating endpoint and must never be broadcast or leak to other endpoints.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 112

```
Verify pass handling in the host coordinator: a valid pass broadcasts FullSync; an invalid pass sends MoveRejected with a placeholder empty move. Check that the placeholder move cannot be mistaken for a real move by client-side handling.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 113

```
Verify PlayerJoined handling: the broadcast to existing endpoints plus the FullSync to the new endpoint. Check the ordering guarantees so the new endpoint cannot process a MoveAccepted before it has any state, and report any real race.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 114

```
Verify host-side message filtering: FullSync, GameConfig, MoveAccepted, MoveRejected, and Pong arriving from clients must be ignored at the host handler layer. Confirm a malicious client cannot inject authoritative state through any of these.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 115

```
Check Ping and Pong end to end: the host responds with endpoint-targeted Pong. Determine whether anything actually consumes Pong for liveness, and report whether the mechanism is dead code or wired to a purpose.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 116

```
Verify NearbySession client behavior: PlaceMove and Pass go only to MessageTarget.Host, and the client applies state only from FullSync and MoveAccepted. Confirm the client never applies its own move optimistically in a way that could diverge from the authoritative state.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 117

```
Verify NearbySession host behavior: the host's own local moves must pass through HostGameCoordinator with identical validation to remote moves, and the resulting messages must go out through the transport so clients stay in sync.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 118

```
Verify reconnect state handling: connectionState is RECONNECTING while any player index is marked reconnecting. Trace the marking and unmarking lifecycle and check whether the 60-second reconnect timeout constant is actually enforced anywhere, so a permanently departed player cannot lock the session in RECONNECTING forever.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 119

```
Verify host-only startNewGame: determine exactly what happens if the client role invokes it (exception, silent ignore, or protocol message) and whether that behavior is safe.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 120

```
Verify move authorization consistency: a Move carries playerIndex inside it, while the coordinator authorizes by endpoint-to-owner mapping. Check both PlaceMove and Pass paths so a client cannot act for a playerIndex outside its mapped owner, including owner groups with multiple color slots.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 121

```
Trace the GameConfig message end to end: who sends it, when, and how the client applies board and mode configuration relative to the first FullSync. Report any ordering gap where a client could render with a wrong board size.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Nearby Connections Coordinator and Play Services Facade

### Question 122

```
Verify the coordinator basics: service id com.finnvek.cornersapart, Strategy.P2P_STAR for both advertising and discovery, and clean role handling where the host advertises and the client discovers with no state where both run simultaneously.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 123

```
Verify the pending connection flow: endpoint name and authentication digits are stored before accept or reject. Check cleanup on reject, on disconnect, and on discovery stop, so a stale pending entry can never be accepted later against a different endpoint.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 124

```
Verify endpoint-to-owner assignment: each accepted remote endpoint gets the next non-computer owner index that is not the local owner. Check exhaustion handling when more endpoints connect than owners exist, reassignment behavior after a disconnect, and correctness for Two-Color Duel owner groups.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 125

```
Verify payload handling: only BYTES payloads are accepted through the facade; FILE and STREAM payloads must be rejected safely. Confirm decode and payload failures set ConnectionState.FAILED without crashing, and determine whether FAILED is recoverable or terminal for the session.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 126

```
Verify host-side inbound authorization coverage: a connected endpoint is required, and PlaceMove, Pass, and PlayerJoined are restricted to the endpoint's mapped owner with unauthorized PlaceMove receiving MoveRejected NOT_PLAYERS_TURN. Check every other message type for authorization gaps, especially PlayerLeft and Ping.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 127

```
Verify client-side sync gating: FullSync and other authoritative messages are accepted only from the selected connected host endpoint, so a second endpoint cannot inject state into a client.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 128

```
Verify disconnect handling symmetry: host losing a client removes the endpoint mapping and marks the departed owner's player slots reconnecting through PlayerLeft; verify what the client does when the host disconnects, and that coordinator state (connected, approved, mappings, host id) is fully cleaned in both directions.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 129

```
Verify send-path routing: MessageTarget.Broadcast, MessageTarget.Host, and endpoint-targeted sends resolve to correct endpoint ids, and check the behavior of each send path when invoked while disconnected or before any endpoint exists.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 130

```
Review PlayServicesConnectionsClientFacade callback translation: verify every Play Services callback the coordinator logic implicitly relies on (connection initiated, result, disconnected, endpoint found, endpoint lost, payload received, payload transfer update) is actually forwarded, and report any missing forwarding that would cause silent stalls.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 131

```
Verify coordinator thread safety: Play Services callbacks arrive on the main thread while coroutines may touch the same mutable maps and sets for connected endpoints, approvals, and owner mappings. Check the synchronization or confinement strategy and report concrete unsafe access.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 132

```
Verify advertising and discovery lifecycle: started only after permission grant, stopped on connect, on leave, and on coordinator disconnect. Also determine whether the 300-second background timeout constant is enforced against lingering advertising or discovery when the app is backgrounded.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 133

```
Verify currentSession lifecycle: exactly when it becomes non-null and null, and confirm GameViewModel's collector can never observe a session before its flows and coordinator wiring are fully initialized.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Nearby Runtime Permissions

### Question 134

```
Verify NearbyPermissions.requiredRuntimePermissions band logic exactly: SDK <= 28 coarse location; 29-30 fine location; 31 coarse plus fine plus the three Bluetooth permissions; >= 32 the three Bluetooth permissions plus NEARBY_WIFI_DEVICES; >= 37 additionally ACCESS_LOCAL_NETWORK. Check each boundary value (28, 29, 30, 31, 32, 36, 37) against the code's comparison operators.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 135

```
Verify hasRequiredPermissions: it requires every permission for the SDK band to map to true. Check how the permission grant map is produced, which Android API is used for the check, and whether a permission missing from the map counts as denied.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 136

```
NearbyPermissionsTest covers SDK 28, 30, 31, 32, and 33 only. Verify what the policy returns for SDK 34, 35, 36, and 37, and write out exactly which additional test cases would close the documented SDK 37 validation gap. Recommend only the minimal test additions.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 137

```
Verify the GameRoute permission flow: the launcher must request exactly the policy's permissions for the running SDK, and host or discover must continue only after a full grant. Check handling of partial grants and permanent denial so the UI cannot get stuck in a connecting state.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 138

```
Verify the ACCESS_LOCAL_NETWORK permission string: it is added as a raw string for SDK >= 37. Confirm the exact string value matches the real Android 17 platform permission constant, since a typo here would fail silently.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Persistence and DataStore

### Question 139

```
Verify CornersApartJson (encodeDefaults=true, ignoreUnknownKeys=true) is the JSON instance used for all three DataStore files, and that no persistence path accidentally uses the strict GameProtocol JSON or vice versa.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 140

```
Verify JsonDataStoreSerializer failure handling: serialization and illegal-argument parse failures convert to CorruptionException. Then check what actually happens on corruption at the DataStore level: is a ReplaceFileCorruptionHandler installed for each store, or does a corrupt file make reads fail permanently until app data is cleared?

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 141

```
Verify each serializer's defaultValue: it must match the documented defaults of SavedGameData, ProfilesData, and GameSettings (difficulty 3, sound true, haptics true, preferred mode FOUR_PLAYER), so a fresh install and a corruption reset produce identical state.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 142

```
Verify GameRepository: saveGame(state, settings, savedAtEpochMillis) writes a complete SavedGameData in one transactional update, clearSavedGame resets to the default instance, and check the difference between the savedGameData and savedGame exposures and which consumers rely on which.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 143

```
Verify the ProfileRepository active-profile invariant: across every combination of upsertProfile and setActiveProfile calls, exactly one profile must be active, never zero and never more than one. Check the invariant enforcement inside the transactional updates, not just in tests.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 144

```
Verify appendHistory: entries append to the matching profile only, unknown profileId is handled safely, and the trim to the 50 newest entries keeps the correct end of the list.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 145

```
Verify SettingsRepository.updateSettings uses DataStore updateData transactionally with no read-modify-write outside the transform, so concurrent updates from settings UI and game flow cannot lose writes.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 146

```
Verify the dormant settings removal: reducedMotionEnabled and preferredRuleset must be absent from GameSettings, from the settings UI, and from serialized output, while old persisted JSON that still contains those keys must load cleanly via ignoreUnknownKeys. Confirm the existing serializer test really covers the legacy-JSON load direction, not only the output direction.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 147

```
Verify DataStore instance uniqueness: the Context extension factories (gameRepository, profileRepository, settingsRepository) and the Hilt PersistenceModule are two creation paths. Confirm they cannot ever create two DataStore instances for the same file in one process, which crashes at runtime, and identify which path production code actually uses.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 148

```
Verify saved-game settings snapshot semantics on resume: which values come from the snapshot versus current settings when a game is restored, and confirm ResumeGameDialog fields (time, mode, leader, bonus count, difficulty) are all read from the snapshot.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Profiles, Avatars, and History

### Question 149

```
Verify LocalAvatarGenerator: avatarSeed with fallback to profile id, initials from up to two name parts uppercased with CA for blank names, and four deterministic palette entries. Confirm the hash mixing always produces fully opaque ARGB values and identical output for identical input.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 150

```
Verify customAvatarPath: determine whether it is consumed anywhere in the UI, whether the avatar dimension and file-size constants are enforced when it is set, and whether a stored path to a missing or unreadable file is handled without crashing.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 151

```
Verify HistoryEntry population at game over: every field (date, rank, totalScore, scoreBreakdown, claimedBonusTiles, piecesPlaced, difficulty, ruleset, gameMode, timeSeconds, scores) must come from the correct source, with rank derived from Scoring.rankPlayers for the active profile's owner specifically.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 152

```
Verify HistoryStatsCalculator: the win definition, win rate math, averages, best score, average rank, average bonus tiles, completion bonus count, favorite difficulty with higher-difficulty tie-breaking, and the score trend over the last 20 entries. Check every division for zero-entry guards and check edge cases with one entry.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 153

```
Verify per-difficulty stats: aggregation keyed by the persisted 1-5 difficulty value consistently, with no mixing of enum ordinals and persisted values.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 154

```
Verify timeSeconds: game start time comes from TimeProvider. Check what happens to the start time when a saved game is resumed, whether the recorded duration for a resumed game is correct or restarts from zero, and whether that behavior is intentional.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## GameViewModel

### Question 155

```
Review GameViewModel initialization: the settings, saved-game, profiles, Nearby-state, and currentSession collections all start at construction. Verify there is no race between the initial local session creation, the saved-game emission, and the resume-decision flag that could skip or duplicate the resume prompt.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 156

```
Verify active-session routing: with nearbySession ?: localSession as the active GameSession, confirm every user action (place, pass, new game where applicable) goes through the active session and no code path still references localSession directly when a Nearby session is active.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 157

```
Verify the Nearby-session-appears transition: selection, orientation, and start time reset; resume decision marked made; the session's gameState collected; and one-shot events forwarded as GameEffect. Check job management so repeated host-leave-host cycles cannot accumulate duplicate collectors or leak jobs.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 158

```
Verify leaveNearbySessionForLocalPlay ordering: active Nearby jobs and session cleared and the coordinator disconnected. Confirm the ordering cannot let a final stale Nearby state or effect emission land in the freshly started local game.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 159

```
Verify the local-only persistence guard: saved games and history must persist only for local sessions. Check the guard condition itself and confirm a Nearby game over can neither write history nor overwrite the existing local saved game.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 160

```
Verify save-on-accepted-turn behavior: every accepted local turn triggers GameRepository.saveGame with state, settings, and now. Check whether computer turns each trigger a separate save, whether passes are saved, and whether the resulting save frequency in a fast Solo game is a real performance or wear concern.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 161

```
Verify the game-over history once-guard: history is recorded exactly once per finished game. Check the guard against effect re-emission, recomposition, rapid state emissions, and starting a new game immediately after.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 162

```
Verify saved-game clearing and the resume-decision lifecycle after game over: once a game finishes and a new one starts, the ResumeGameDialog must not reappear for the finished game under any emission ordering.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 163

```
Verify selection normalization: when the current player has used the selected piece, what does normalization select next, and what happens when the current player has zero pieces remaining? Confirm no crash or invalid selection state.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 164

```
Verify rotation math against per-piece orientation counts of 1, 2, 4, and 8: clockwise increments modulo the count, counterclockwise decrements modulo the count. Check the counterclockwise path specifically for negative modulo results in Kotlin.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 165

```
Verify flip behavior: flip normalizes a horizontal flip of the current orientation and selects the matching orientation when present. Check symmetric pieces where the flip equals an existing rotation, and check the fallback when no matching orientation exists.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 166

```
Verify effect emissions completeness and exactness: MoveRejected built from reason names with safe parsing, MoveAccepted only when the score delta is positive (confirm no legal move can produce a zero delta given 1 point per cell), GameOver on both move-ending and pass-ending paths, ActionFailed for failed Nearby session events. Report missing or duplicate emissions.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 167

```
Verify pass gating: pass is ignored after game over and is sent for the current player only. Check the race window where the user taps pass exactly as a computer turn advances the current player.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 168

```
Verify GameUiState.currentPlayer indexing: players[currentPlayerIndex] assumes list position equals player index. Check every mode and the Nearby lobby state for any configuration where the assumption breaks and would throw IndexOutOfBoundsException in the UI.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Compose UI

### Question 169

```
Verify GameRoute effect collection: the LaunchedEffect keys, the SharedFlow configuration, and behavior across configuration changes. Determine whether effects can be dropped or replayed incorrectly during rotation, and confirm haptics and sound are gated by the settings flags at the moment of the effect, not stale captures.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 170

```
Review GameSoundPolicy and GameSoundPlayer: the mapping from game events to sounds, the lifecycle of the tone generation resources (creation, release, leak on recomposition), and the behavior when sound is disabled between event and playback.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 171

```
Verify the Nearby permission launch flow in GameRoute: the pending host-or-discover intent must survive recomposition, denial must return the UI to a clean idle state, and a grant must continue exactly the action the user chose.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 172

```
Verify the ResumeGameDialog trigger: a saved game exists and no continue-or-new decision has been made. Check the interaction where a Nearby session appearing marks the decision made, and confirm the dialog cannot flash briefly during startup emission ordering.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 173

```
Review dialog state handling in GameScreenContent: local showSettings, showProfiles, and showHelp states, dismiss behavior, and whether losing these on configuration change is acceptable or causes a real UX bug given the current activity setup.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 174

```
Verify GameOverDialog: it must not dismiss on outside tap or back per the documented behavior, play-again and stats buttons must route correctly, and the ranking rows must render owner-aggregated Two-Color Duel results with the owner naming rules.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 175

```
Verify GameBoard tap mapping: offset-to-cell conversion must be exact for both 20 and 14 boards including the outermost cells at every density, and the 160 ms interaction lock plus 180 ms invalid-feedback cooldown must not swallow legitimate rapid taps in normal play.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 176

```
Review GameBoard Canvas drawing: draw order of cells, bonus diamonds, start markers on empty corners only, and glossy occupied rendering. Check for per-frame allocations inside the draw loop (objects created per cell per frame) that are worth a simple fix.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 177

```
Verify the piece strip and selected-piece preview: pieces filtered to the current player's available set, used pieces dimmed with the documented alpha, and the preview rendering the exact selected orientation that placement would use.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 178

```
Verify GameLayoutPolicy: compact below 840 dp and expanded at 840 dp or wider. Check what width source feeds the policy and confirm both layouts expose every control including the Nearby actions and pass button.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 179

```
Verify accessibility: the Game board content description, semantic descriptions on all icon buttons, the polite live announcement node, and confirm announcements fire for move accepted, rejected, and game over with text sourced from strings.xml.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 180

```
Verify the haptic mapping implementation: text-handle feedback for accepted moves unless a bonus tile was claimed, long-press feedback for bonus claims, rejections, and game over. Check the actual HapticFeedbackType constants used match this intent on current Compose APIs.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 181

```
Verify the Nearby UI states: status text, error display, discovered endpoint connect buttons, and the pending authentication code with accept and reject. Check empty and loading states, and confirm stale discovered endpoints disappear when discovery stops or an endpoint is lost.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 182

```
Scan the ui/ package for guardrail violations: hardcoded user-facing strings that belong in strings.xml, and hardcoded colors or dimensions that duplicate theme tokens. Report exact literals and locations only.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 183

```
Review recomposition hygiene in the game screen: unstable parameters, lambdas recreated per recomposition on hot paths (board, piece strip), and confirm the current code matches the tracked Compose stability baselines given failOnStabilityChange is true.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 184

```
Verify PlayerScoreBar: chunking player cards into rows of two must render odd counts (Three-Player) correctly, and the current-player ghost highlight and passed-player dimming must use theme tokens rather than literals.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Theme and Visual Tokens

### Question 185

```
Verify the theme token values match the documented hex values for all player colors, backgrounds, board colors, and text colors, and confirm ghost variants with 0x4D alpha are used only where translucency is intended, never as solid fills.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 186

```
Verify typography: quicksand.ttf loads from res/font, and only displayLarge, headlineMedium, bodyLarge, labelLarge, bodySmall, and labelSmall are defined. Check whether any composable references an undefined Material text style and silently falls back to defaults in a visually inconsistent way.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 187

```
Verify the player palette mapping from colorIndex to Indigo, Amber, Coral, and Teal is applied consistently across the board rendering, score bar, piece strip, previews, and dialogs, with no component maintaining its own color list.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 188

```
Review color contrast in actual usage: text primary, secondary, and muted on the app background and card surface, and the white on-player color on all four player colors including their dark variants. Flag only combinations that are genuinely hard to read.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Resources, Strings, and Localization

### Question 189

```
Verify strings.xml completeness: every string, plural, and accessibility string referenced from code exists, and identify genuinely unused strings. Cross-check against the lint unused-resources check and any baseline that might be masking findings.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 190

```
Verify the eight local vector drawables (flip, help, history, person, rotate left, rotate right, settings, skip next): correct rendering at their used sizes, tinting behavior with the theme, and no hardcoded fill colors where a tint is expected.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 191

```
Verify plurals usage: every plural resource is consumed through the quantity-aware API with the right quantity argument, with no hardcoded English pluralization in code.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 192

```
Assess RTL behavior: supportsRtl is true with English-only strings. Check whether the board controls, rotate icons, and horizontal piece strip behave sensibly under a forced RTL locale, and state whether the simplest correct approach is keeping RTL support or explicitly not, based on the actual code.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Tests

### Question 193

```
Review assertion strength in the engine unit tests: do placement, scoring, and game-end tests assert exact expected states and values, or do some only assert absence of exceptions? Identify the weakest genuinely valuable assertions to strengthen, and nothing more.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 194

```
Verify coroutine test correctness in GameViewModel tests: MainDispatcherRule usage, runTest with proper time control, and check whether any test depends on real delays tied to the opponent timing constants in a way that makes it slow or flaky.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 195

```
Verify InMemoryJsonStateStore fidelity: its update and flow-emission semantics must match DataStoreJsonStateStore closely enough (transactional transform, emission on change, initial value behavior) that repository tests are not passing against unrealistically forgiving behavior.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 196

```
Identify genuinely missing critical tests: specifically check whether NearbyConnectionsCoordinator decode-failure and FAILED-state paths, reconnect timeout behavior, and the GameRoute permission grant and denial flows have any coverage. List only the top missing tests by real risk, not an exhaustive wish list.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 197

```
Verify the single instrumented test file covers the five documented areas (game screen controls, history and stats tabs, settings toggles, help sections, game-over breakdown), and confirm whether instrumented tests run in any CI workflow at all, since build.yml appears to run only unit tests.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 198

```
Verify the guardrail tests parse the real files they guard: release identity, repository policy, dependency verification policy, manifest security, dependency hygiene, and stability baseline tests must read the actual Gradle, manifest, and metadata files rather than duplicating expected values that could drift silently alongside the source.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 199

```
Verify Nearby protocol test coverage includes hostile inputs: malformed JSON, wrong discriminator values, missing required fields, and truncated payloads, not only happy-path round trips. Report which hostile cases are actually covered.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 200

```
Verify the opponent determinism tests: identical seed and state must produce the identical move across all difficulties, and check whether legality-across-many-states property coverage exists or moves are only tested on a few fixed boards.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 201

```
Review MockK usage in ViewModel and coordinator tests: identify relaxed mocks that could silently swallow important interactions, and verify that critical calls (save, history append, disconnect, message sends) are explicitly verified where it matters.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 202

```
Verify coverage exclusion consistency: the JaCoCo exclusions and the Sonar coverage exclusions (entrypoints, UI screens, PlayServicesConnectionsClientFacade) must describe the same set, so local and SonarCloud coverage numbers agree.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Static Analysis, Security Tooling, and CI

### Question 203

```
Review .github/workflows/build.yml: verify the pinned action SHAs correspond to the intended action versions, evaluate whether --no-configuration-cache is actually necessary for the assembleDebug, test, detekt, and lint steps or only for OWASP, and note that ktlint does not appear to run in CI; confirm whether that omission is intentional and covered elsewhere.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 204

```
Review .github/workflows/sonar.yml: fetch depth 0, the SONAR_TOKEN guard, and verify the no-token path (the Finnish notice) exits successfully so pull requests from forks without secrets do not fail.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 205

```
Review .github/workflows/codeql.yml: it scans only GitHub Actions workflows because Kotlin 2.4 was outside CodeQL's supported range. Check the current CodeQL Kotlin support status and report whether Kotlin source scanning can now be enabled with a minimal change, or must stay disabled.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 206

```
Review dependabot.yml against dependency verification: a Dependabot version bump will fail Gradle dependency verification unless verification-metadata.xml is updated. Verify whether any automation or documented process handles metadata regeneration for Dependabot PRs, and report the gap if none exists.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 207

```
Review the detekt setup: version 2.0.0-alpha.5 with buildUponDefaultConfig, the documented thresholds (long method 60 ignoring composables, TooManyFunctions limits), Compose-aware exclusions, and inspect app/detekt-baseline.xml for the number and nature of baselined findings that might be masking real issues.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 208

```
Review the ktlint and detekt interaction: android=true, generated directories ignored, and check for any formatting rules where the two tools disagree and would cause churn between runs.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 209

```
Verify the Compose Stability Analyzer configuration: tracked baselines for debug and release exist and are in Git, failOnStabilityChange true, allowMissingBaseline false, and the stabilityDump task integrates with normal builds without breaking configuration cache.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 210

```
Review the Dependency-Check configuration: debug and release runtime classpaths scanned, test groups skipped, OSS Index disabled, fail CVSS 7 default. Inspect config/dependency-check/suppressions.xml and verify the compose-stability-runtime-android GitHub Enterprise CPE suppression is still needed and scoped as narrowly as possible.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 211

```
Review the six custom Semgrep matchers (android-exported-component, android-security-boundary-surface, android-uri-share-without-clipdata, fileprovider-broad-path, raw-nearby-bypass, sensitive-android-log): verify each pattern actually matches what its name claims against this codebase's patterns, and report obvious false-negative gaps in the sensitive-log term list.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 212

```
Verify the Sonar configuration: project key, sources and tests paths, coverage XML path, and the coverage exclusions all match the real project layout, and confirm tools/sonar.ps1 cannot write the token value into reports/sonar.txt or reports/sonar-issues.json.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 213

```
Verify the two osv-scanner.toml files: the documented ignore of Gradle verification metadata as a lockfile is correct, and neither config accidentally ignores files that should be scanned.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 214

```
Review the Android lint configuration: abortOnError true, checkReleaseBuilds true, OldTargetApi fatal, GradleDependency and AndroidGradlePluginVersion disabled. Evaluate only whether any specific enabled or disabled choice is demonstrably wrong for this project, and confirm AndroidLintPolicyTest asserts what it claims.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 215

```
Verify the tools/ wrapper scripts: each delegates the documented command to the shared InvokeProjectCheck.ps1, and tools/sonar.ps1 handles a missing or invalid token gracefully. Also confirm reports/ is gitignored and no report artifact is committed.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 216

```
Verify .gitignore correctness: reports, build outputs, Gradle and Android-check caches, and IDE files excluded, while the Compose stability baselines under app/stability remain tracked with no conflicting ignore pattern, as the guardrail hygiene test asserts.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Security and Privacy

### Question 217

```
Search the entire main source set for logging of sensitive data: endpoint ids, authentication digits, payload contents, profile names, save contents, and board or move data. The Semgrep rule exists, but verify manually and report any Log or println call that leaks Nearby or profile information.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 218

```
Verify authentication digit handling: the digits are shown to the user for verification, and must never be persisted to disk, written to logs, or retained after accept or reject completes. Trace the full lifetime of the value.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 219

```
Verify the no-network guarantee: profiles, history, and saves are local only and the only radio usage is Play Services Nearby. Search for any URL, HttpURLConnection, OkHttp, or other network client usage anywhere in the app code.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 220

```
Assess Nearby BYTES payload size limits: FullSync carries the entire GameState including moveHistory. Estimate the serialized size of a late-stage four-player game against the Nearby Connections BYTES payload cap, and report whether FullSync can realistically exceed the cap and fail, with the simplest mitigation if so.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 221

```
Review hostile-input robustness after decode: a syntactically valid GameMessage can still carry extreme values (negative player indexes, huge anchors, absurd orientation indexes, oversized collections). Verify engine validation catches every such value after protocol decode, and identify any field that reaches logic unvalidated.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 222

```
Verify the product identity guardrails in resources: no user-facing claims that the app has AI, and no external board-game names, logos, or official wording anywhere in strings.xml or other user-visible resources.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Concurrency and Flows

### Question 223

```
Review every StateFlow and SharedFlow configuration in the app: replay counts, buffer sizes, and overflow strategies, especially for the GameEffect SharedFlow. Determine whether rapid emissions (spamming invalid moves) can drop effects or backpressure the emitter.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 224

```
Audit coroutine scope lifecycles: viewModelScope jobs in GameViewModel, the coordinator's internal scope, and LocalSession's computer-turn coroutines. Verify every launched job is cancelled when its owner is cleared, replaced, or disposed, and report any job that can outlive its owner.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 225

```
Verify main-thread safety: identify anything computationally heavy that runs on the main thread, in particular whether getValidMoves or piece-availability computation executes during state mapping for the UI on every emission.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 226

```
Audit mutable state outside flows: the coordinator's endpoint maps and sets, session player collections, and any ViewModel fields mutated from multiple contexts. Verify each is either confined to one thread or properly synchronized.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 227

```
Search production code for runBlocking and report every occurrence with its call context.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 228

```
Verify the resume-dialog race: a late saved-game flow emission arriving after the user already started a new game must not re-trigger the ResumeGameDialog. Trace the exact flag and emission ordering that prevents this, or report the hole.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Performance

### Question 229

```
Analyze getValidMoves complexity: pieces times orientations times board anchors on a 20x20 board. Verify what early-exit or pruning exists, how often the function is called per turn (validator, game-end check, opponent generation, UI availability), and whether any per-recomposition call path exists. Recommend only the simplest caching if a real hot path is confirmed.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 230

```
Review board rendering efficiency: verify the Canvas redraws only when relevant state changes and identify per-frame allocations in the draw path that a trivial hoist would fix. Do not propose a rendering rewrite.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 231

```
Verify EXPERT opponent resource usage: 500 candidates with an 1800 ms budget on Dispatchers.Default with Gradle-unrelated runtime parallelism. Check whether evaluation yields cooperatively or can occupy Default workers in a way that delays other coroutines on low-core devices.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 232

```
Estimate the cost of per-accepted-turn saveGame calls: serialized SavedGameData size for a mid-game state and DataStore write frequency during a fast Solo game with three computer opponents. Report whether debouncing is genuinely warranted or the current behavior is fine.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 233

```
Verify piece orientation cache effectiveness: normalization and deduplication must run once per piece id for the process lifetime, and no call path should recompute orientations per query.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Known Review Targets from PROJECT.md

### Question 234

```
Evaluate the Nearby debugging surface: does NearbyConnectionsCoordinator plus the rendered Nearby UI expose enough host, client, and player-owner state for two-device stress testing and reconnect debugging, or is the current status, endpoint, and auth-code surface too thin? Base the answer on what the code actually exposes, and propose only the minimal concrete additions if genuinely insufficient.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 235

```
Evaluate surfacing disconnect: GameViewModel.disconnectNearby() exists but is not in GameScreenActions or rendered in GameScreenContent, so users cannot leave a Nearby session without switching to local play. Verify this from the code and assess the actual user consequence, then state the minimal change if one is warranted.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 236

```
Evaluate GameSoundPlayer: verify whether it currently generates platform tones at runtime, and whether that approach has concrete measurable problems (latency, audio focus, volume stream choice) that justify moving to res/raw assets before release, or whether tones are fine for v1.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 237

```
Evaluate ProfilesDialog: review the current edit workflow for functional defects only (lost input, invalid states, missing validation). Density and polish preferences are out of scope; report only things that are actually broken.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 238

```
Confirm the SDK 37 gap precisely: the NearbyPermissions SDK 37 local-network branch, the missing AndroidManifest.xml declaration for ACCESS_LOCAL_NETWORK, and the missing SDK 37 unit test. Verify each element from the code and state the minimal complete fix as a set of exact changes.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 239

```
Evaluate the release-signing matcher's future robustness: given the actual AGP 9.2.1 task names this project produces (including makeApkFromBundleForRelease and zipApksForRelease which unit tests dry-run), determine whether any current artifact task escapes, and whether the name-based approach has a proven gap today. Do not speculate about future AGP versions beyond noting the maintenance risk.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 240

```
Evaluate the dependencyCheckAnalyze fallback design: is the failing guidance task under configuration cache the right approach, and do the wrapper and report flows always invoke the real task with --no-configuration-cache? Verify the wrapper side too if the scripts are readable from the repository.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 241

```
Audit PRIVACY-POLICY.md: list every remaining placeholder ([App Name], [your email], [date]) and additionally verify the policy's actual claims against real app behavior: local-only data, no analytics, Nearby radio usage, and the Play data-safety implications of the declared permissions. Report factual mismatches only.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 242

```
Evaluate the sonar task's dependency on assembleDebug and jacocoDebugUnitTestReport: is the artifact coupling a real problem for the current workflow, and would it become one only if release-style gates are added later? Give a verdict grounded in the current build files.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 243

```
Verify the dormant settings removal end to end one final time: reducedMotionEnabled and preferredRuleset absent from the GameSettings model, all settings UI, and serializer output, with the existing test asserting the serialized-output side. Confirm whether anything anywhere still references those names.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

## Cross-Cutting Final Checks

### Question 244

```
Perform a whole-project scan and report the ten most impactful genuine issues across all areas, ranked by severity, ignoring style nits and speculative concerns. If fewer than ten genuine issues exist, list only the real ones and say so explicitly.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 245

```
Verify PROJECT.md accuracy against the live code: list every statement in PROJECT.md that no longer matches the source, since the document is used for agent handoff and stale claims cause bad future decisions. Only report actual mismatches you can point to in code.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 246

```
Check AGENTS.md and memory/MEMORY.md against the actual build files: verify the documented commands, guardrails, and workflow claims still match reality, and list only concrete inconsistencies.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 247

```
Scan the repository for TODO, FIXME, and HACK comments: classify each as genuinely unfinished work, a stale note that should be deleted, or an intentional documented limitation, with file locations.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 248

```
Review error surfaces across module boundaries: exception types and messages that cross from engine and sessions toward the UI, especially the reason.name string channel. Verify nothing internal or confusing can reach a user-visible string, and that every boundary translates errors consistently.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```


## Additional Questions Added from Older Document Comparison
The original Fable 5 questions above are left unchanged. The following addendum contains only the additional review questions identified by comparing `CODEX_CODE_REVIEW_QUESTIONS_REVISED.md` from Question 42 onward against the newer PROJECT.md-based question bank.

### Question 249

```
Verify holistic GameState acceptance gates: every decoded, restored, synchronized, or replacement GameState must be semantically validated before it becomes live state. Check board size and cells, player indexes and ordering, currentPlayerIndex, ownerIndex values, usedPieceIds, scores, bonus tiles, moveHistory, gameMode/config compatibility, randomSeed/bonusLayoutId consistency, and isGameOver derivation. Confirm saved-game restore, FullSync, MoveAccepted.state, and replaceState all pass through the same gate, or explain exactly why a path is safe without it.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 250

```
Verify getValidMoves against an independent brute-force oracle rather than treating optimized production helpers as proof of themselves. If no independent oracle exists, specify the smallest test needed: enumerate all pieces, all distinct orientations, and all board anchors on small and representative reachable states, then compare exact valid-move sets, rejection parity, hasValidMove equivalence, and duplicate suppression against getValidMoves.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 251

```
Trace concurrent turn actions end to end: two rapid local placements, two remote host requests for the same turn, and a placement racing a pass. Verify state mutation is serialized so exactly one action validates against the old state, the next action validates against the new state, turnNumber increments at most once per accepted action, and stale pass or move callbacks cannot affect the next player.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 252

```
Verify release artifact safety end to end: a release artifact must never silently use the debug key, an incomplete signing config, or an unsigned output when some signing variables are missing. Inspect assemble, bundle, package, sign, install, and publish task variants, and verify missing or partial signing input fails closed for artifact-producing release tasks while still allowing intentional release verification tasks.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 253

```
Inspect release debuggability and artifact mix-up risks: review the merged release manifest and build outputs for debuggable, testOnly, profileable, JNI debugging, pseudo-locales, developer metadata, ambiguous artifact names, stale outputs, debug/release path confusion, retained R8 mapping files, resource-shrink reports, and exact dependency/build metadata needed for crash diagnosis.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 254

```
Trace minified release startup as a smoke path: application startup, Hilt initialization, theme and font loading, repository creation, default-profile creation, settings collection, initial GameViewModel state, Play Services facade setup, and first Compose render. Verify a minified release build has evidence that this path starts without crashing before UI appears, and report the smallest missing release-mode smoke test if it does not.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 255

```
Verify Nearby permission lifecycle after the initial grant: permissions or radios may be revoked while advertising, discovering, connecting, or playing. Check SecurityException handling, state transitions, stale pending host/discover actions, request races, user-facing rationale accuracy, and whether neverForLocation remains truthful for the actual Play Services Nearby usage and app code.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 256

```
Evaluate process-death resume evidence for an unfinished game: background the app, kill the process, restart, and resume. The scenario must cover the resume dialog, active profile, saved settings snapshot, elapsed-time semantics, current-player ownership, pending Solo computer turn cancellation, stale Nearby state cleanup, and transient dialog or pending permission action cleanup. Report existing automated evidence and the smallest missing manual or instrumented test sequence.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 257

```
Assess future migration readiness: identify every persisted or network-visible compatibility contract, including save/profile/settings schemas, protocol message names and strict-key policy, GameMode and Ruleset enum values, piece ids, orientationIndex ordering, scoring fields, bonusLayoutId format, and Nearby service id. Report which contracts are documented, versioned, migrated, or currently fragile.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 258

```
Inventory third-party license and notice obligations: bundled font, libraries, Gradle plugins, vector icons, sound assets if present, generated-avatar inputs, copied snippets, and packaged META-INF metadata. Verify what repository files prove, identify missing notices or provenance, and separate legal or license-holder confirmation from code-review findings.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 259

```
Map accessibility evidence to a release checklist: TalkBack gameplay, state-aware labels, semantic roles and selected/checked/disabled states, switch access, keyboard or D-pad navigation, focus order, dialog focus, large font, contrast, RTL, touch targets, reduced motion, and live announcements. Separate automated evidence from manual device checks and define pass criteria for every manual item.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 260

```
Trace CI secret exposure and workflow privilege boundaries: SONAR_TOKEN, signing variables, dependency-service credentials, Gradle properties, and any report-upload credentials across push, pull_request, scheduled, and forked PR contexts. Verify workflow permissions are least-privilege and untrusted code cannot exfiltrate secrets or alter privileged workflow behavior.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 261

```
Expand signing-secret exposure checks beyond console output: audit Gradle scripts, CI logs, exception messages, process arguments, generated BuildConfig, merged manifests, report files, configuration-cache entries, local caches, and task inputs for keystore paths, passwords, aliases, or signing properties. Verify secrets are never serialized or surfaced in plain text.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 262

```
Review merged manifests for every build variant, not only source manifests: identify dependency-contributed permissions, exported components, providers, services, receivers, metadata, features, and intent filters that are not obvious from app/src/main/AndroidManifest.xml. Verify build-type or source-set manifest overrides cannot weaken backup, device-transfer, cleartext, or exported-component policy.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 263

```
Verify visible Nearby disconnect and reconnect control coverage: GameViewModel may expose disconnect or cleanup actions, but Compose actions and UI must surface the safe controls needed for host, client, discovered endpoint, pending connection, reconnecting player, and failed session states. Check whether owner-role and endpoint-owner debug surfaces are sufficient for manual two-device validation without leaking sensitive endpoint or payload details.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

### Question 264

```
Tie moveHistory, DataStore write cost, and Nearby payload size together under realistic play: rapid local turns, Solo chains with three computer opponents, long games, and repeated FullSync broadcasts. Verify saveGame frequency, JSON size, BYTES payload size, moveHistory trimming or bounding, and allocation cost do not create UI stalls, DataStore churn, or Nearby payload failures.

Note for you as the reviewer: you are an LLM and you can hallucinate. Do not invent problems that do not exist; "no issues found" is a fully acceptable and good result. Only report issues you can verify directly in the actual source code of this repository, cite the exact file and code location for every finding, and prefer the simplest sufficient fix over complex or over-engineered changes. Do not propose speculative refactors.
```

Total questions: 264
