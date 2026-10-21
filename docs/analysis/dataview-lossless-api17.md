# DataView lossless analysis: api-17 delta

The full analysis was done on api-12, and its report lives on branch `analysis/dataview-lossless` in
`docs/analysis/dataview-lossless.md`. That report has the H1–H9 verdicts, X1–X19, the decisions, and file:line
evidence for api-12. This document records only what is different on **api-17**, the base for the implementation
work, which merges up to api-18 through api-21.

The same reproducer tests were cherry-picked onto `upstream/api-17` and run unchanged first. Every test that failed
there marks a behavior difference. Those tests were then re-pinned to api-17 behavior.

## Versions

| Item | Value |
|---|---|
| Sponge | `upstream/api-17` = `5ecbb2441b87229188ef4ea9f50b5e086ccadf40` ("fix(neo): let test and gameTest run in the same build") |
| SpongeAPI | `f7088f6a8baa661d788a536732e04372164b2c44` ("Make Mannequin extends ArmorEquipable"), `version=17.1.0-SNAPSHOT`. This is the Sponge submodule pin and also the `upstream/api-17` tip; SpongeAPI is still built as an included build. |
| Minecraft | 1.21.10 (`gradle.properties`) |
| Forks | `api-17` mirrored to `gabizou/SpongeCommon` and `gabizou/SpongeAPI` as new branches, identical to upstream |

## What changed between api-12 and api-17

**Code:**
- **Unchanged:** `MemoryDataView`, `MemoryDataContainer`, `SpongeDataView`, `DataSerializer`, `Coerce`,
  `JsonDataFormat`, `NBTDataFormat` and `DataTranslatorProvider`.
- **Rewritten:** `NBTTranslator`, for the newer tag API (`25278b3`, plus the `3bbe5c5` merge).
- **Small edits:** `CompoundTagDataView` (5 lines: `keySet()`/`entrySet()` replace accessors).
- **API:** nothing relevant changed in SpongeAPI `data/persistence` (one line in `DataFormat.java`).

**Behavior.** The first run of the unchanged api-12 tests gave 70 tests and 4 failures. All four are explained
below:

| Finding | api-12 | api-17 | Cause |
|---|---|---|---|
| **X20 (new): `CompoundTagDataView` booleans** | `Boolean` | **`Byte`** (`get`, `values`, equality); `getBoolean` still works | The `$Boolean` check in `NBTTranslator.fromTagBase` was lost while resolving merge `3bbe5c5`. The api-12 parent has it; the api-17 parent and the result don't. Also missing on api-18 to api-21. Issue draft 04. |
| X17 mixed lists (`[1, "a"]`) | NBT write throws `UnsupportedOperationException` | **round-trips** | Minecraft 1.21.5+ `ListTag` accepts mixed element types. X17's `List<Boolean>` → `List<Byte>` part is unchanged. |
| H7 cross-implementation `equals` | equal with a boolean in the content | not equal, because of X20 | H7 still holds: a nested view alone gives equal views with different `hashCode`s. The tests now use that. |
| Matrix | | 5 cells differ: `Boolean true`, `Boolean false`, and `nested DataView with boolean` (CompoundTagDataView column, PASS → LOSSY); `List mixed` (NBT and CompoundTagDataView columns, ERROR → PASS) | the two causes above |

**Unchanged on api-17:**
- **Hypotheses:** H1–H9.
- **Additional findings:** X1–X16, X18 and X19, the `List<Boolean>` part of X17, and the `Float`/`Double` `-0.0`
  and long-string NBT behavior (X1, X2).
- **Issue drafts 01–03:** still apply. The `issues/` copies on this branch use api-17 versions and line numbers.

## Decisions

All decisions and follow-ups (1–10, F1–F4) from the api-12 report apply unchanged. Implementation work targets
api-17.

## Commands run and results (api-17)

Maven Central returned HTTP 429 to this container for every request during the session. api-17's
`settings.gradle.kts` resolves `mavenCentral()` first, while api-12 used Sponge's repository, so the cached
artifacts were not reused. All api-17 Gradle runs therefore used a local, uncommitted init script. It adds
`https://repo.spongepowered.org/repository/maven-public/`, a repository the build already declares, ahead of the
others. Dependency verification (`gradle/verification-metadata.xml`) still applies.

| Command | Result |
|---|---|
| `git fetch upstream api-17` (both repos); `git push fork upstream/api-17:refs/heads/api-17` (both) | forks' `api-17` = `5ecbb24` / `f7088f6` |
| `git checkout -b analysis/dataview-lossless-api17 upstream/api-17`; `git cherry-pick 5f253c0 4976a8a` | clean |
| `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.lossless.*'` | failed to resolve: HTTP 429 from `repo.maven.apache.org`, including four retries with backoff from 30 to 240 s, and `--offline` |
| same, with `--init-script sponge-repo-first.init.gradle` (api-12 tests, unchanged) | 70 tests, **4 failed** (the differences above) |
| same, after re-pinning to api-17 behavior and adding X20 | **BUILD SUCCESSFUL**, 71 tests, 0 failures |
| `./gradlew --no-daemon --init-script … :SpongeVanilla:test --tests 'org.spongepowered.common.data.*'` | **BUILD SUCCESSFUL**, 218 tests, 0 failures, 14 skipped (existing `@Disabled`: 10 in `EntityDataTest`, 4 in `ItemStackDataTest`) |
| `./gradlew --no-daemon --init-script … :spotlessJavaCheck` | BUILD SUCCESSFUL |
| `git show 3bbe5c5^1:…/NBTTranslator.java`, `3bbe5c5^2:…`, `3bbe5c5:…` searched for the `fromTagBase` `$Boolean` check | found only in the api-12 parent |
| the same search on `upstream/api-18` … `api-21` | absent on all four |

## Lossless matrix (api-17)

Columns and comparison rules are as in the api-12 report.

| Value | primitive overload | Object overload | NBT round trip | JSON round trip | CompoundTagDataView (extra) |
|---|---|---|---|---|---|
| Boolean true | PASS | PASS | PASS | PASS | LOSSY: Boolean(true) -> Byte(1) |
| Boolean false | PASS | PASS | PASS | PASS | LOSSY: Boolean(false) -> Byte(0) |
| Byte 0 | PASS | PASS | PASS | LOSSY: Byte(0) -> Integer(0) | PASS |
| Byte MIN | PASS | PASS | PASS | LOSSY: Byte(-128) -> Integer(-128) | PASS |
| Byte MAX | PASS | PASS | PASS | LOSSY: Byte(127) -> Integer(127) | PASS |
| Short MIN | PASS | PASS | PASS | LOSSY: Short(-32768) -> Integer(-32768) | PASS |
| Short MAX | PASS | PASS | PASS | LOSSY: Short(32767) -> Integer(32767) | PASS |
| Integer 0 | PASS | PASS | PASS | PASS | PASS |
| Integer MIN | PASS | PASS | PASS | PASS | PASS |
| Integer MAX | PASS | PASS | PASS | PASS | PASS |
| Long 1 (fits int) | PASS | PASS | PASS | LOSSY: Long(1) -> Integer(1) | PASS |
| Long MIN | PASS | PASS | PASS | PASS | PASS |
| Long MAX | PASS | PASS | PASS | PASS | PASS |
| Float 0.1 | PASS | PASS | PASS | LOSSY: Float(0.1) -> Double(0.1) | PASS |
| Float 1.0 | PASS | PASS | PASS | LOSSY: Float(1.0) -> Double(1.0) | PASS |
| Float NaN | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was NaN | PASS |
| Float +Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was Infinity | PASS |
| Float -Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was -Infinity | PASS |
| Float -0.0 | PASS | PASS | LOSSY: Float(-0.0) -> Float(0.0) | LOSSY: Float(-0.0) -> Double(-0.0) | LOSSY: Float(-0.0) -> Float(0.0) |
| Float MIN_VALUE | PASS | PASS | PASS | LOSSY: Float(1.4E-45) -> Double(1.4E-45) | PASS |
| Float MAX_VALUE | PASS | PASS | PASS | LOSSY: Float(3.4028235E38) -> Double(3.4028235E38) | PASS |
| Double 0.1 | PASS | PASS | PASS | PASS | PASS |
| Double 1.0 | PASS | PASS | PASS | PASS | PASS |
| Double 1e20 | PASS | PASS | PASS | PASS | PASS |
| Double NaN | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was NaN | PASS |
| Double +Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was Infinity | PASS |
| Double -Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was -Infinity | PASS |
| Double -0.0 | PASS | PASS | LOSSY: Double(-0.0) -> Double(0.0) | PASS | LOSSY: Double(-0.0) -> Double(0.0) |
| Double MIN_VALUE | PASS | PASS | PASS | PASS | PASS |
| Double MAX_VALUE | PASS | PASS | PASS | PASS | PASS |
| String empty | PASS | PASS | PASS | PASS | PASS |
| String ascii | PASS | PASS | PASS | PASS | PASS |
| String unicode | PASS | PASS | PASS | PASS | PASS |
| String with '.' | PASS | PASS | PASS | PASS | PASS |
| String 70k chars | PASS | PASS | LOSSY: String(xxxxxxxxxxxxxxxxxxxxxxxx...(70000)) -> String() | PASS | PASS |
| byte[] empty | N/A | PASS | PASS | PASS | PASS |
| byte[] | N/A | PASS | PASS | ERROR: ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Byte (java.l... | PASS |
| int[] | N/A | PASS | PASS | PASS | PASS |
| long[] small values | N/A | PASS | PASS | ERROR: ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Long (java.l... | PASS |
| long[] large values | N/A | PASS | PASS | PASS | PASS |
| short[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@60aef774 | ERROR: IllegalArgumentException: Unable to translate object to JSON: [S@6acc41d4 | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@7b0142e3 |
| double[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@71a724f9 | ERROR: IllegalArgumentException: Unable to translate object to JSON: [D@66a568ee | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@6eef470f |
| String[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@1f08166b | ERROR: IllegalArgumentException: Unable to translate object to JSON: [Ljava.lang.String;@57c0799f | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@1a6f8991 |
| List<Boolean> | N/A | PASS | LOSSY: [0] Boolean(true) -> Byte(1) | PASS | LOSSY: [0] Boolean(true) -> Byte(1) |
| List<Byte> | N/A | PASS | PASS | LOSSY: [0] Byte(1) -> Integer(1) | PASS |
| List<Short> | N/A | PASS | PASS | LOSSY: [0] Short(1) -> Integer(1) | PASS |
| List<Integer> | N/A | PASS | PASS | PASS | PASS |
| List<Long> | N/A | PASS | PASS | LOSSY: [0] Long(1) -> Integer(1) | PASS |
| List<Float> | N/A | PASS | PASS | LOSSY: [0] Float(1.5) -> Double(1.5) | PASS |
| List<Double> | N/A | PASS | PASS | PASS | PASS |
| List<String> | N/A | PASS | PASS | PASS | PASS |
| List empty | N/A | PASS | PASS | PASS | PASS |
| List mixed [1, "a"] (extra) | N/A | PASS | PASS | PASS | PASS |
| List<List<Integer>> (extra) | N/A | PASS | PASS | PASS | PASS |
| Map<String,?> | N/A | LOSSY: Map -> DataView([x, y]) | PASS | PASS | LOSSY: Map -> DataView([x, y]) |
| Map<String,?> key with '.' | N/A | LOSSY: Map -> DataView([a.b]) | PASS | PASS | LOSSY: Map -> DataView([a.b]) |
| Map<Integer,?> | N/A | LOSSY: Map -> DataView([1]) | PASS | PASS | LOSSY: Map -> DataView([1]) |
| Map<Long,?> | N/A | ERROR: UnsupportedOperationException: Unsupported map-key type class java.lang.Long | N/A (value cannot be stored) | N/A (value cannot be stored) | ERROR: UnsupportedOperationException: Unsupported map-key type class java.lang.Long |
| Map<UUID,?> | N/A | LOSSY: Map -> DataView([00000000-0000-0001-0000-000000000002]) | PASS | PASS | LOSSY: Map -> DataView([00000000-0000-0001-0000-000000000002]) |
| Map<Enum,?> | N/A | LOSSY: Map -> DataView([ALPHA]) | PASS | PASS | LOSSY: Map -> DataView([ALPHA]) |
| UUID | N/A | LOSSY: UUID(01234567-89ab-cdef-fedc-...(36)) -> DataView([UuidLeast, UuidMost]) | PASS | PASS | LOSSY: UUID(01234567-89ab-cdef-fedc-...(36)) -> DataView([UuidMost, UuidLeast]) |
| enum (plain, no translator) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to JSON: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA |
| enum DayOfWeek (no translator) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: MONDAY | ERROR: IllegalArgumentException: Unable to translate object to JSON: MONDAY | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: MONDAY |
| enum Month (has translator) | N/A | LOSSY: Month(MARCH) -> DataView([LocalDateMonth]) | PASS | PASS | LOSSY: Month(MARCH) -> DataView([LocalDateMonth]) |
| nested DataView | N/A | PASS | PASS | LOSSY: deep: l: Long(5) -> Integer(5) | PASS |
| nested DataView with boolean (extra) | N/A | PASS | PASS | PASS | LOSSY: b: Boolean(true) -> Byte(1) |
| Optional.empty() (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty | ERROR: IllegalArgumentException: Unable to translate object to JSON: Optional.empty | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty |
| null | ERROR: NullPointerException: value | ERROR: NullPointerException: value | N/A (value cannot be stored) | N/A (value cannot be stored) | ERROR: NullPointerException: value |
