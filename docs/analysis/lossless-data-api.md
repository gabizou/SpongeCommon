# Lossless data APIs (api-17)

Status: **verification and plan, waiting for review.** Nothing under `src/main` or in SpongeAPI has changed on
`fix/lossless-data-api` yet. Every FIXED/UNCHANGED/WONTFIX below is the *proposed* status; the "after" matrices,
fixing commits and their test flips get filled in once the plan is confirmed.

Scope: the existing data APIs as plugins use them today (`DataSerializable`, `DataBuilder`, `DataTranslator`,
`DataContentUpdater`, `DataView`/`DataContainer`, and `DataFormats.NBT`/`JSON`/`HOCON`). The `DataCodec` work on
`feature/data-codec` is not used.

## Summary

**Lossless today (api-17, verified):**
- In memory and through all three formats: `Boolean`, `Integer`, `Long` outside the int range, finite `Double`
  other than whole numbers in HOCON, `String` ≤ 65535 UTF-8 bytes in NBT, `List<Integer|Double|String>`, nested views
  of these.
- Every registered translator except `ZonedDateTime` with an offset zone and `Component` (vectors, quaternions,
  complex numbers, UUID, `LocalTime`, `LocalDate`, `LocalDateTime`, `Instant`, `Month`, `Direction`), read back with
  `getObject`. `-0.0` inside a vector loses its sign in NBT and HOCON.
- `ItemStack`, `ItemStackSnapshot`, `BlockState`, `ProfileProperty`, `PotionEffect`, `Color`, `EntityArchetype`,
  `BlockEntityArchetype`, `BlockSnapshot` and a plugin-style `DataSerializable` with a content updater: rebuilt
  through `DataManager.deserialize` they give the same `toContainer()` after every format, and the updater runs.
- Plugin custom data (`DataStore`) through `DataManager.deserialize(ItemStack.class, …)` and entity save/load, for
  scalars, translated types, registry values, `ResourceKey`, serializables, `List<Integer|String|UUID>`, sets and maps.

**Not lossless on api-17** (details and evidence in [Findings](#findings)):

| Area | Loss |
|---|---|
| `DataSerializable` | `GameProfile` drops properties (X23); `FireworkEffect` drops colors (X24); `ServerLocation` truncates the position to whole blocks (X28); `Enchantment` and `BannerPatternLayer` cannot be serialized at all (X25) |
| Translators | `Component` values are never translated (X10); `ZonedDateTime` with an offset zone cannot be read back (X26) |
| Custom data | `ItemStack.builder().fromContainer()` hides every custom key until the next write (X22); `List<Boolean>` reads back as `List<Byte>` and JSON/HOCON change list element widths (X29); plain-enum and `Component` keys cannot be stored, and a failed offer of one leaves the entity unsavable (X27); booleans in `CompoundTagDataView` read as `Byte` (X20) and copying such a view erases the boolean marker in storage (X20b) |
| NBT | strings over 65535 bytes become `""` (X1); `-0.0` becomes `0.0` (X2); `$Boolean` matched anywhere in a key (X3, X12); `List<Boolean>` → `List<Byte>` (X17) |
| JSON | cannot read back its own `byte[]`/small `long[]` (X7), cannot write NaN/∞ (X6), `_arraytype` user keys hijacked (X5), number widths collapse (X18) |
| HOCON (new) | number widths collapse like JSON; whole-number `Float`/`Double` (incl. `-0.0`) come back as `Integer`; `Float` widened with noise (`0.1f` → `0.10000000149011612`); NaN/∞ → `String`; arrays → `List`; enums → `String` (X21) |

**What the plan would make lossless:** X1 (as an error, not silent), X3/X12, X7, X10, X16 for enums, X20, X20b
for new writes, X22, X23, X24, X25, X26, X27 for enums, X28 and X29. **What stays lossy and why:** number widths in
JSON and HOCON (X18, X21; fixing needs a new on-disk shape), `-0.0` in NBT (X2; vanilla's `FloatTag.valueOf`
normalizes on read too), `List<Boolean>` in raw NBT (X17; NBT has no boolean type, only typed getters can recover
it), NaN/∞ in JSON (X6), and booleans already stored without their marker (X20b; the information is gone).

**What plugins would observe differently** (each needs your confirmation, see [Plan](#plan)): `CompoundTagDataView`
returns `Boolean` instead of `Byte` (P1); `DataFormats.NBT` throws `IOException` instead of writing `""` (P4);
`FireworkEffect.toContainer()` stores colors as views (P8); `set(path, component)` stores a `DataView` (P12); NBT and
JSON write enums as their name instead of throwing (P13). Everything else only turns a wrong or empty result, or an
exception, into the right value.

## Versions

| Item | Value |
|---|---|
| Sponge base | `upstream/api-17` = `5ecbb2441b87229188ef4ea9f50b5e086ccadf40` ("fix(neo): let test and gameTest run in the same build") |
| SpongeAPI | submodule pin `f7088f6a8baa661d788a536732e04372164b2c44` ("Make Mannequin extends ArmorEquipable"), which is exactly `upstream/api-17` of SpongeAPI; `version=17.1.0-SNAPSHOT`. Built as an included build (`settings.gradle.kts`, `includeBuild("SpongeAPI")` with `dependencySubstitution`). `.gitmodules` still says `branch = api-11`; it has no effect. |
| Minecraft | 1.21.10 (`gradle.properties`) |
| Gradle / JDK | wrapper 8.14.4; OpenJDK 21.0.12.1 (Ubuntu build) |
| Branches | `fix/lossless-data-api` in both clones, from `upstream/api-17`. SpongeAPI has no commits on it yet; nothing needs to change there for this plan. |
| Reproducers | cherry-picked with `-x` from `fork/analysis/dataview-lossless-api17`: `6631c46` → `a5df265`, `dc0ab26` → `1139434`, `e1313b0` → `1777144` |

**Repository setup.** Both clones: `origin` (SpongePowered) push URL `DISABLED`; `upstream` = SpongePowered, push
URL `DISABLED`; `fork` = `gabizou/SpongeCommon` / `gabizou/SpongeAPI`. `git ls-remote fork 'refs/heads/fix/*'` was
empty on both forks before starting.

**Dependency resolution.** `repo.maven.apache.org` still answers HTTP 429. This container already has a global init
script, `~/.gradle/init.d/sponge-maven.init.gradle`, put there by the environment's setup ("Generated by Sponge
cloud-setup.sh"). It rewrites `repo.maven.apache.org`, `repo1.maven.org`, `plugins.gradle.org` and
`libraries.minecraft.net` repositories to `https://repo.spongepowered.org/repository/maven-public/`. I relied on it
and did not add my own `--init-script`. Dependency verification still applies.

## Commands run and results

All from the Sponge clone root on `fix/lossless-data-api`.

| # | Command | Result |
|---|---|---|
| 1 | `git fetch upstream api-17` (both); `git fetch fork analysis/dataview-lossless-api17 analysis/dataview-lossless` | ok |
| 2 | `git checkout -b fix/lossless-data-api upstream/api-17` (both); `git cherry-pick -x 6631c46 dc0ab26 e1313b0` (Sponge) | clean |
| 3 | `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.lossless.*'` (cherry-picks only) | **BUILD SUCCESSFUL** in 7m 14s (first run, includes Minecraft setup): 71 tests, 0 failures. Every pinned api-17 characterization still holds. |
| 4 | same, with the new reproducers and empty pins | 77 tests, 6 failed (the new pinned tables, and the fixture reader before fixtures existed), as intended; observed statuses then pinned |
| 5 | `SPONGE_LOSSLESS_FIXTURES_OUT=$PWD/src/test/resources/org/spongepowered/common/data/lossless/api-17 ./gradlew --no-daemon :SpongeVanilla:test --tests '…lossless.Api17FixtureTest.write'` with `git diff upstream/api-17 -- src/main SpongeAPI` empty | BUILD SUCCESSFUL; fixtures written (see [Compatibility evidence](#compatibility-evidence)) |
| 6 | `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.lossless.*'` | **BUILD SUCCESSFUL**: 79 tests, 0 failures, 1 skipped (the env-gated fixture writer) |
| 7 | `./gradlew --no-daemon :SpongeVanilla:gameTest --tests '*LosslessGameTests*'` | first run 2 failed (empty pins, and one enum value hid the other keys); after splitting and pinning **BUILD SUCCESSFUL**: 3 tests, 0 failures |
| 8 | `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.*'` | **BUILD SUCCESSFUL**: 226 tests, 0 failures, 15 skipped (14 existing `@Disabled` in `EntityDataTest`/`ItemStackDataTest`, plus the fixture writer) |
| 9 | `./gradlew --no-daemon :spotlessJavaCheck :SpongeVanilla:spotlessJavaCheck` | BUILD SUCCESSFUL |
| 10 | `javap -c -p -cp ~/.gradle/caches/VanillaGradle/v2/jars/net/minecraft/joined/1.21.10/joined-1.21.10.jar 'net.minecraft.nbt.FloatTag$1' net.minecraft.nbt.FloatTag net.minecraft.nbt.DoubleTag` | `FloatTag$1.load` reads through `FloatTag.valueOf`, which returns `ZERO` when `fcmpl` against 0 is equal, so `-0.0` is lost on read as well as on write; both tags have a public record constructor |

## Verification of the earlier map on api-17

Line numbers are on `5ecbb24`, under `src/main/java/org/spongepowered/common/data/` unless a path is given.
`MemoryDataView`, `MemoryDataContainer`, `SpongeDataView`, `DataSerializer`, `Coerce`, `JsonDataFormat` and
`NBTDataFormat` are byte-identical to api-12 for the lines cited in the api-12 report; the rewritten
`NBTTranslator` and `CompoundTagDataView` lines are re-cited here. Every row is backed by a passing pinned test.

| # | api-17 verdict | Evidence (api-17) | Test |
|---|---|---|---|
| H1 set overload routing | CONFIRMED | `MemoryDataView.java:298-307` (`set0` → `DataSerializer.serialize` at `:304`); `DataSerializer.java:57-94` | `H1SetOverloadRoutingTest` |
| H2 null rejected | CONFIRMED | `MemoryDataView.java:168,181,242,251`; `CompoundTagDataView.java:127,221` | `H2NullRejectedTest` |
| H3 set replaces scalar intermediate | CONFIRMED | `MemoryDataView.java:232-237`; `CompoundTagDataView.java:134,357-359` | `H3SetChildReplacesScalarTest` |
| H4 createView over scalar | CONFIRMED, implementations disagree | `MemoryDataView.java:343` (`ClassCastException`); `CompoundTagDataView.java:323,327-333,357-359` (replaces) | `H4CreateViewOverScalarTest` |
| H5 get through scalar looks absent | CONFIRMED | `MemoryDataView.java:137-140`; `CompoundTagDataView.java:109,348-355` | `H5GetThroughScalarTest` |
| H6 copy re-serializes | CONFIRMED | `MemoryDataView.java:385-406`; `CompoundTagDataView.java:376-378` ignores `safety` | `H6CopyReserializesTest` |
| H7 equals/hashCode across classes | CONFIRMED | `MemoryDataView.java:414-430`; `SpongeDataView.java:501-507` (no `hashCode`); `CompoundTagDataView.java:386-401` | `H7EqualsHashCodeTest` |
| H8 array identity | CONFIRMED for `MemoryDataView`, REFUTED for `CompoundTagDataView` | as api-12 | `H8ArrayIdentityTest` |
| H9 Coerce leniency | CONFIRMED | `builder/Coerce.java:55-297`; list getters `SpongeDataView.java:262-404` | `H9CoerceTest` |
| X1 NBT long strings → `""` | CONFIRMED, also for custom data through vanilla saving (gametest) | `persistence/NBTDataFormat.java:69` → `NbtIo.write` (fallback writer) | `AdditionalFindingsTest#x1*`, matrix, `CustomDataRoundTripTest`, `LosslessGameTests` |
| X2 NBT `-0.0` | CONFIRMED; also lost on **read** (`FloatTag$1.load` → `valueOf`) | bytecode, command 10 | `AdditionalFindingsTest#x2NbtNegativeZero`, matrix, translators (`Vector3d -0.0`) |
| X3 `$Boolean` matched anywhere | CONFIRMED | `persistence/NBTTranslator.java:192-193` | `AdditionalFindingsTest#x3*` |
| X4 JSON `null` NPE | CONFIRMED | `persistence/JsonDataFormat.java:119,137-139` → `MemoryDataView.java:168` | `AdditionalFindingsTest#x4*` |
| X5 `_arraytype` hijack | CONFIRMED | `persistence/JsonDataFormat.java:114-117,132,149-173` | `AdditionalFindingsTest#x5*` |
| X6 JSON NaN/∞ | CONFIRMED | `persistence/JsonDataFormat.java:241-242` | `AdditionalFindingsTest#x6*`, matrix |
| X7 JSON own arrays unreadable | CONFIRMED; one such value makes the **whole document** unreadable (fixture writer) | `persistence/JsonDataFormat.java:161-169` | `AdditionalFindingsTest#x7*`, matrix, `Api17FixtureTest` (`single-e036.json`, `single-e038.json`) |
| X8 createView splits dotted map keys | CONFIRMED | `SpongeDataView.java:211,213` | `AdditionalFindingsTest#x8*` |
| X9 map key collisions | CONFIRMED | `DataSerializer.java:111-129` | `AdditionalFindingsTest#x9*` |
| X10 Component never translated | CONFIRMED, also blocks custom data keys | `registry/provider/DataTranslatorProvider.java:66,69,100-103` (`IdentityHashMap`, exact class); `DataSerializer.java:64` | `AdditionalFindingsTest#x10*`, `TranslatorRoundTripTest`, `CustomDataRoundTripTest` |
| X11 CompoundTagDataView hands out arrays | CONFIRMED | `CompoundTagDataView.java:112-121,361-368`; `persistence/NBTTranslator.java:272-273,285-288` | `AdditionalFindingsTest#x11*` |
| X12 CompoundTagDataView strips `$Boolean` anywhere | CONFIRMED | `CompoundTagDataView.java:62,68` | `AdditionalFindingsTest#x12*` |
| X13–X15, X18, X19 | CONFIRMED, unchanged | as api-12 | `AdditionalFindingsTest` |
| X16 accepted but unwritable | CONFIRMED; for custom data it fails at `offer` (ItemStack) or at entity save (X27) | `DataSerializer.java:88-91`; `persistence/NBTTranslator.java:175`; `persistence/JsonDataFormat.java:260`; `persistence/ConfigurateTranslator.java:154` | `AdditionalFindingsTest#x16*`, matrix, `CustomDataRoundTripTest`, `LosslessGameTests` |
| X17 NBT `List<Boolean>` → `List<Byte>` | CONFIRMED (mixed lists round-trip on 1.21.5+) | `persistence/NBTTranslator.java:146-153,260-261` | `AdditionalFindingsTest#x17*`, matrix |
| X20 CompoundTagDataView booleans → `Byte` | CONFIRMED; plus X20b below | `persistence/NBTTranslator.java:258-261` ignores `key`; `CompoundTagDataView.java:68,115,119,361-368` pass it | `AdditionalFindingsTest#x20*` |

## Findings

The table lists every finding that affects losslessness, with its proposed status. X1–X20 are from the earlier
analysis; X20b and X21–X30 are new in this pass. Paths: data classes as in the verification table (under
`src/main/java/org/spongepowered/common/data/`); `item/`, `profile/`, `world/` under
`src/main/java/org/spongepowered/common/`; `mixins/…` is `src/mixins/java/org/spongepowered/common/mixin/`. Proposed statuses: **FIXED** (in the plan), **UNCHANGED** (not
in this task, or needs your call), **WONTFIX** (cannot be made lossless without a new stored shape or a vanilla
behavior change).

| # | Severity | Finding | Evidence (api-17) | Test | Proposed |
|---|---|---|---|---|---|
| X1 | data loss | NBT strings > 65535 bytes saved as `""` | see verification | `AdditionalFindingsTest#x1*` | FIXED as an error (P4), draft 01 |
| X2 | silent corruption | `-0.0` → `0.0` in NBT | see verification | `#x2NbtNegativeZero` | WONTFIX (open question 4) |
| X3, X12 | silent corruption | `$Boolean` marker matched and stripped anywhere in a key | see verification | `#x3*`, `#x12*` | FIXED (P2), draft 03 step 1 |
| X4 | error path | JSON `null` → NPE | see verification | `#x4JsonNullInput` | UNCHANGED (not a loss of data Sponge wrote) |
| X5 | silent corruption | user key `_arraytype` hijacked by JSON reader | see verification | `#x5*` | UNCHANGED (open question 5) |
| X6 | data loss | JSON cannot write NaN/∞ | see verification | `#x6*` | WONTFIX (would need non-standard JSON) |
| X7 | data loss | JSON cannot read back its own `byte[]`/int-range `long[]`, and such a file is unreadable as a whole | see verification | `#x7*`, fixtures | FIXED (P3), draft 02 |
| X8, X9 | silent corruption | dotted keys split by `createView(path, map)`; `1`/`"1"` key collision | see verification | `#x8*`, `#x9*` | UNCHANGED (in-memory API shape, not a saved-data loss; changing it is observable) |
| X10 | data loss | `Component` never translated | see verification | `#x10*`, translators, custom data | FIXED (P12) |
| X11 | silent corruption | `CompoundTagDataView` hands out its internal arrays | see verification | `#x11*` | UNCHANGED (aliasing, not loss on save) |
| X16 | data loss | values `set` accepts but no format can write | see verification | `#x16*`, matrix | FIXED for enums (P13); UNCHANGED for `Optional`, `short[]`, `double[]`, `float[]`, `boolean[]`, `Object[]` |
| X17 | silent corruption | NBT `List<Boolean>` reads back as `List<Byte>` | see verification | `#x17*`, matrix | WONTFIX in raw NBT; typed reads fixed by P5 |
| X18 | width loss | JSON: `Byte`/`Short`/small `Long` → `Integer`, `Float` → `Double` | `persistence/JsonDataFormat.java:176-190` | `#x18*`, matrix | WONTFIX (open question 3) |
| X20 | silent corruption | `CompoundTagDataView` returns booleans as `Byte` (merge `3bbe5c5`) | see verification | `#x20*`, matrix | FIXED (P1), draft 04 |
| **X20b** | **data loss (permanent)** | Copying a `CompoundTagDataView` into another view, or translating it to NBT, writes its booleans as plain bytes **without the marker**, so the boolean is gone from storage. `streamRootValues` yields `("b", Byte)`; `DataSerializer.java:58-60` and `persistence/NBTTranslator.java:74-86` re-set what it yields. The api-17 fixture shows stored custom data already in this state: `customData/DataView` in `corpus.json` has `"b": 1`, not `"b$Boolean": 1`. | `CompoundTagDataView.java:66-69`, `persistence/NBTTranslator.java:258-261` | `DataHolderFindingsTest#x20bCopyDropsBooleanMarker`, fixtures | FIXED for new writes (P1); already-damaged data reads as `Byte` (WONTFIX) |
| **X21** | width/type loss | **HOCON** (`persistence/HoconDataFormat.java:97-108` → `ConfigurateTranslator.java:131-158`): `Byte`/`Short`/small `Long` → `Integer`; `Float` → `Double` widened with noise (`0.1f` → `0.10000000149011612`); whole-number `Float`/`Double` → `Integer`, so `-0.0` → `0`; NaN/∞ → `String`; `byte[]`/`int[]`/`long[]`/`short[]`/`double[]`/`String[]` → `List`; plain enums → `String`; `Optional` fails | matrix HOCON column | `LosslessMatrixTest` | WONTFIX for widths and arrays (open question 3); Float noise FIXED (P14) |
| **X22** | data hidden on read | `ItemStack.builder().fromContainer(c).build()` returns a stack whose custom data keys all read as empty, for every value type and with no data format involved. `fromContainer` builds through the codec (`item/SpongeItemStack.java:164-171`, `:324-360`), then `fromItemStack` copies the component patch (`:123-136`) and `build()` creates a fresh stack and calls `applyComponents` (`:265-266`). Sponge data is only read in the codec constructor hook and in the `set(CUSTOM_DATA)` hook (`mixins/…/core/world/item/ItemStackMixin.java:116-132`), and `applyComponents` goes through neither. The tag still holds the data, `toContainer()` is unchanged, and the next custom data `offer` brings it back. `DataManager.deserialize(ItemStack.class, c)` is not affected. | as cited | `CustomDataRoundTripTest` (builder column), `DataHolderFindingsTest#x22BuilderHidesCustomData` | FIXED (P6) |
| **X23** | data loss | `GameProfile` loses its properties: `toContainer` writes `Properties` (`profile/SpongeGameProfile.java:246-252`), the builder never reads it (`profile/SpongeGameProfileDataBuilder.java:44-53`) | as cited | `SerializableRoundTripTest` | FIXED (P7) |
| **X24** | data loss | `FireworkEffect` loses its colors: `toContainer` writes `Colors` as the raw `IntList` and `Fades` as `Color`s (`mixins/…/api/minecraft/world/item/component/FireworkExplosionMixin_API.java:92-93`); the builder reads both with `getSerializableList`, which keeps only views (`data/builder/item/SpongeFireworkEffectDataBuilder.java:62-63`, `SpongeDataView.java:397-404,421-426`) | as cited | `SerializableRoundTripTest` | FIXED (P8) |
| **X25** | unserializable | `Enchantment.toContainer()` and `BannerPatternLayer.toContainer()` throw `ValueNotFoundException`: they look up a server-scoped registry in the game root (`item/enchantment/SpongeEnchantment.java:76`, `mixins/…/api/minecraft/world/level/block/entity/BannerPatternLayers_LayerMixin_API.java:67`; both types are `minecraftKeyInServer` in `SpongeAPI/…/registry/RegistryTypes.java:243,322`). The same class uses the scoped holder at `SpongeEnchantment.java:64`. An enchanted `ItemStack` is not affected: it goes through the item codec. | as cited | `SerializableRoundTripTest` | FIXED (P9) |
| **X26** | unreadable | `ZonedDateTime` with an offset zone (`+02:00`; `Z` fails the same check) is written, but reading throws `InvalidDataException`, even from memory: the translator only accepts ids in `ZoneId.getAvailableZoneIds()`, which holds region ids only (`persistence/DataSerializers.java:864-867`) | as cited | `TranslatorRoundTripTest` (tested with `+02:00`) | FIXED (P10) |
| **X27** | holder poisoned | For a custom value no NBT translation exists for (plain enum, `Component`): on an ItemStack `offer` throws; on an entity (tested with a plain enum) `offer` throws but the value stays in the manipulator, and **every later save of that entity throws** (`DataUtil.java:206-210` → `persistence/NBTTranslator.java:175`) | as cited | `CustomDataRoundTripTest`, `LosslessGameTests#entityWithUnsavableCustomData` | FIXED for enums and `Component` (P13, P12); UNCHANGED for other unwritable types (open question 6) |
| **X28** | data loss | `ServerLocation` loses the fraction of its position, even in memory: `toContainer` writes doubles (`world/server/SpongeServerLocation.java:294-300`), the builder reads `getInt` (`world/server/SpongeServerLocationBuilder.java:46-52`) | as cited | `LosslessGameTests#worldSerializablesRoundTrip` | FIXED (P11) |
| **X29** | type loss | Custom data list keys are read with the raw `getList` (`DataDeserializer.java:163-178`, fallback `:177`), so `Key<ListValue<Boolean>>` returns `List<Byte>` (NBT) or `List<Integer>` (JSON/HOCON), and JSON/HOCON turn `List<Long>`/`List<Float>` into `Integer`/`Double` elements, which then fail with `ClassCastException` in plugin code. Scalars are read with typed getters (`:99-119`) and are fine. | as cited | `CustomDataRoundTripTest` | FIXED (P5) |
| **X30** | cosmetic | `EntitySnapshot` through JSON or HOCON stores the entity tag's bytes as ints (`Invulnerable: Byte(0) -> Integer(0)`), because the rebuilt tag is translated from the read view. Whether the restored entity behaves the same was not tested. | `persistence/NBTTranslator.java:98-99` | `LosslessGameTests#worldSerializablesRoundTrip` | UNCHANGED (consequence of X18/X21) |

Hypotheses checked and refuted along the way:
- **X22 "the next offer drops the hidden data"**: refuted. Offering any custom key re-reads the tag first, and the
  hidden value is kept (`DataHolderFindingsTest#x22BuilderHidesCustomData`).
- **"`DataManager.deserialize(ItemStack)` loses custom data"**: refuted; only the builder path does.

## Compatibility evidence

`Api17FixtureTest` (`src/test/java/org/spongepowered/common/data/lossless/Api17FixtureTest.java`) and the files in
`src/test/resources/org/spongepowered/common/data/lossless/api-17/` (commit "Add data fixtures written by
unmodified api-17").

- **How they were written.** Command 5 above, with `git diff upstream/api-17 -- src/main SpongeAPI` empty: the
  data code was exactly api-17 `5ecbb24` with SpongeAPI `f7088f6`.
- **What they hold.** Per format, one corpus file with every entry the format could write: all lossless matrix
  values, all translator values, all DataSerializable rows (including `PluginRecord` at content versions 1 and 2),
  and one `ItemStack` per custom data key. NBT 134 entries, JSON 128, HOCON 139. JSON's `byte[]` and int-range
  `long[]` are written but api-17 cannot read them back, and in the shared file they would make every entry
  unreadable (X7), so each is in its own `single-<key>.json`.
- **What is recorded.** `expected-<ext>.tsv` holds, per entry, the result api-17 got reading its own file: PASS,
  or the exact LOSSY/ERROR cell. On api-17: NBT 122 PASS / 11 LOSSY / 1 ERROR, JSON 102 / 23 / 3, HOCON 96 / 42 / 1.
- **What is asserted.** `Api17FixtureTest.read` reads the committed files with the current code; each entry must give
  the recorded result or PASS, otherwise it is listed as a regression and the test fails. It writes the per-entry
  comparison to `lossless-api17-fixtures.md` in the test working directory (`vanilla/build/test-run/`).
- **Now:** all 401 entries give the recorded result (no `src/main` change yet).

## Lossless matrix, before

Generated by `LosslessMatrixTest` on this branch before any fix (api-17 behavior). Comparison rules are as in the
api-12 report: the runtime class must match, NaN equals NaN, `-0.0` differs from `0.0`, a `Map` never equals a
`DataView`. The NBT, JSON and HOCON columns compare with what the container held before writing. Array hash codes in
error messages differ from run to run.

| Value | primitive overload | Object overload | NBT round trip | JSON round trip | HOCON round trip | CompoundTagDataView (extra) |
|---|---|---|---|---|---|---|
| Boolean true | PASS | PASS | PASS | PASS | PASS | LOSSY: Boolean(true) -> Byte(1) |
| Boolean false | PASS | PASS | PASS | PASS | PASS | LOSSY: Boolean(false) -> Byte(0) |
| Byte 0 | PASS | PASS | PASS | LOSSY: Byte(0) -> Integer(0) | LOSSY: Byte(0) -> Integer(0) | PASS |
| Byte MIN | PASS | PASS | PASS | LOSSY: Byte(-128) -> Integer(-128) | LOSSY: Byte(-128) -> Integer(-128) | PASS |
| Byte MAX | PASS | PASS | PASS | LOSSY: Byte(127) -> Integer(127) | LOSSY: Byte(127) -> Integer(127) | PASS |
| Short MIN | PASS | PASS | PASS | LOSSY: Short(-32768) -> Integer(-32768) | LOSSY: Short(-32768) -> Integer(-32768) | PASS |
| Short MAX | PASS | PASS | PASS | LOSSY: Short(32767) -> Integer(32767) | LOSSY: Short(32767) -> Integer(32767) | PASS |
| Integer 0 | PASS | PASS | PASS | PASS | PASS | PASS |
| Integer MIN | PASS | PASS | PASS | PASS | PASS | PASS |
| Integer MAX | PASS | PASS | PASS | PASS | PASS | PASS |
| Long 1 (fits int) | PASS | PASS | PASS | LOSSY: Long(1) -> Integer(1) | LOSSY: Long(1) -> Integer(1) | PASS |
| Long MIN | PASS | PASS | PASS | PASS | PASS | PASS |
| Long MAX | PASS | PASS | PASS | PASS | PASS | PASS |
| Float 0.1 | PASS | PASS | PASS | LOSSY: Float(0.1) -> Double(0.1) | LOSSY: Float(0.1) -> Double(0.10000000149011612) | PASS |
| Float 1.0 | PASS | PASS | PASS | LOSSY: Float(1.0) -> Double(1.0) | LOSSY: Float(1.0) -> Integer(1) | PASS |
| Float NaN | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was NaN | LOSSY: Float(NaN) -> String(NaN) | PASS |
| Float +Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was Infinity | LOSSY: Float(Infinity) -> String(Infinity) | PASS |
| Float -Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was -Infinity | LOSSY: Float(-Infinity) -> String(-Infinity) | PASS |
| Float -0.0 | PASS | PASS | LOSSY: Float(-0.0) -> Float(0.0) | LOSSY: Float(-0.0) -> Double(-0.0) | LOSSY: Float(-0.0) -> Integer(0) | LOSSY: Float(-0.0) -> Float(0.0) |
| Float MIN_VALUE | PASS | PASS | PASS | LOSSY: Float(1.4E-45) -> Double(1.4E-45) | LOSSY: Float(1.4E-45) -> Double(1.401298464324817E-45) | PASS |
| Float MAX_VALUE | PASS | PASS | PASS | LOSSY: Float(3.4028235E38) -> Double(3.4028235E38) | LOSSY: Float(3.4028235E38) -> Double(3.4028234663852886E38) | PASS |
| Double 0.1 | PASS | PASS | PASS | PASS | PASS | PASS |
| Double 1.0 | PASS | PASS | PASS | PASS | LOSSY: Double(1.0) -> Integer(1) | PASS |
| Double 1e20 | PASS | PASS | PASS | PASS | PASS | PASS |
| Double NaN | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was NaN | LOSSY: Double(NaN) -> String(NaN) | PASS |
| Double +Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was Infinity | LOSSY: Double(Infinity) -> String(Infinity) | PASS |
| Double -Inf | PASS | PASS | PASS | ERROR: IllegalArgumentException: Numeric values must be finite, but was -Infinity | LOSSY: Double(-Infinity) -> String(-Infinity) | PASS |
| Double -0.0 | PASS | PASS | LOSSY: Double(-0.0) -> Double(0.0) | PASS | LOSSY: Double(-0.0) -> Integer(0) | LOSSY: Double(-0.0) -> Double(0.0) |
| Double MIN_VALUE | PASS | PASS | PASS | PASS | PASS | PASS |
| Double MAX_VALUE | PASS | PASS | PASS | PASS | PASS | PASS |
| String empty | PASS | PASS | PASS | PASS | PASS | PASS |
| String ascii | PASS | PASS | PASS | PASS | PASS | PASS |
| String unicode | PASS | PASS | PASS | PASS | PASS | PASS |
| String with '.' | PASS | PASS | PASS | PASS | PASS | PASS |
| String 70k chars | PASS | PASS | LOSSY: String(xxxxxxxxxxxxxxxxxxxxxxxx...(70000)) -> String() | PASS | PASS | PASS |
| byte[] empty | N/A | PASS | PASS | PASS | LOSSY: byte[]([]) -> List([]) | PASS |
| byte[] | N/A | PASS | PASS | ERROR: ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Byte (java.l... | LOSSY: byte[]([1,-1,-128,127]) -> List([Integer(1),...]) | PASS |
| int[] | N/A | PASS | PASS | PASS | LOSSY: int[]([1,-1,-2147483648,2147483647]) -> List([Integer(1),...]) | PASS |
| long[] small values | N/A | PASS | PASS | ERROR: ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Long (java.l... | LOSSY: long[]([1,2]) -> List([Integer(1),...]) | PASS |
| long[] large values | N/A | PASS | PASS | PASS | LOSSY: long[]([-9223372036854775808,9223372036854775807]) -> List([Long(-9223372036854775808),...]) | PASS |
| short[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@10c8ed37 | ERROR: IllegalArgumentException: Unable to translate object to JSON: [S@17479eb4 | LOSSY: short[]([1,2]) -> List([Integer(1),...]) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@2ecde4e5 |
| double[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@79bb055e | ERROR: IllegalArgumentException: Unable to translate object to JSON: [D@52a286e3 | LOSSY: double[]([1.5,2.5]) -> List([Double(1.5),...]) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@10928926 |
| String[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@31a2ef9f | ERROR: IllegalArgumentException: Unable to translate object to JSON: [Ljava.lang.String;@77f1f2d6 | LOSSY: String[]([a,b]) -> List([String(a),...]) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@6fa2f685 |
| List<Boolean> | N/A | PASS | LOSSY: [0] Boolean(true) -> Byte(1) | PASS | PASS | LOSSY: [0] Boolean(true) -> Byte(1) |
| List<Byte> | N/A | PASS | PASS | LOSSY: [0] Byte(1) -> Integer(1) | LOSSY: [0] Byte(1) -> Integer(1) | PASS |
| List<Short> | N/A | PASS | PASS | LOSSY: [0] Short(1) -> Integer(1) | LOSSY: [0] Short(1) -> Integer(1) | PASS |
| List<Integer> | N/A | PASS | PASS | PASS | PASS | PASS |
| List<Long> | N/A | PASS | PASS | LOSSY: [0] Long(1) -> Integer(1) | LOSSY: [0] Long(1) -> Integer(1) | PASS |
| List<Float> | N/A | PASS | PASS | LOSSY: [0] Float(1.5) -> Double(1.5) | LOSSY: [0] Float(1.5) -> Double(1.5) | PASS |
| List<Double> | N/A | PASS | PASS | PASS | PASS | PASS |
| List<String> | N/A | PASS | PASS | PASS | PASS | PASS |
| List empty | N/A | PASS | PASS | PASS | PASS | PASS |
| List mixed [1, "a"] (extra) | N/A | PASS | PASS | PASS | PASS | PASS |
| List<List<Integer>> (extra) | N/A | PASS | PASS | PASS | PASS | PASS |
| Map<String,?> | N/A | LOSSY: Map -> DataView([x, y]) | PASS | PASS | PASS | LOSSY: Map -> DataView([x, y]) |
| Map<String,?> key with '.' | N/A | LOSSY: Map -> DataView([a.b]) | PASS | PASS | PASS | LOSSY: Map -> DataView([a.b]) |
| Map<Integer,?> | N/A | LOSSY: Map -> DataView([1]) | PASS | PASS | PASS | LOSSY: Map -> DataView([1]) |
| Map<Long,?> | N/A | ERROR: UnsupportedOperationException: Unsupported map-key type class java.lang.Long | N/A (value cannot be stored) | N/A (value cannot be stored) | N/A (value cannot be stored) | ERROR: UnsupportedOperationException: Unsupported map-key type class java.lang.Long |
| Map<UUID,?> | N/A | LOSSY: Map -> DataView([00000000-0000-0001-0000-000000000002]) | PASS | PASS | PASS | LOSSY: Map -> DataView([00000000-0000-0001-0000-000000000002]) |
| Map<Enum,?> | N/A | LOSSY: Map -> DataView([ALPHA]) | PASS | PASS | PASS | LOSSY: Map -> DataView([ALPHA]) |
| UUID | N/A | LOSSY: UUID(01234567-89ab-cdef-fedc-...(36)) -> DataView([UuidLeast, UuidMost]) | PASS | PASS | PASS | LOSSY: UUID(01234567-89ab-cdef-fedc-...(36)) -> DataView([UuidMost, UuidLeast]) |
| enum (plain, no translator) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to JSON: BETA | LOSSY: Plain(BETA) -> String(BETA) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA |
| enum DayOfWeek (no translator) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: MONDAY | ERROR: IllegalArgumentException: Unable to translate object to JSON: MONDAY | LOSSY: DayOfWeek(MONDAY) -> String(MONDAY) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: MONDAY |
| enum Month (has translator) | N/A | LOSSY: Month(MARCH) -> DataView([LocalDateMonth]) | PASS | PASS | PASS | LOSSY: Month(MARCH) -> DataView([LocalDateMonth]) |
| nested DataView | N/A | PASS | PASS | LOSSY: deep: l: Long(5) -> Integer(5) | LOSSY: deep: l: Long(5) -> Integer(5) | PASS |
| nested DataView with boolean (extra) | N/A | PASS | PASS | PASS | PASS | LOSSY: b: Boolean(true) -> Byte(1) |
| Optional.empty() (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty | ERROR: IllegalArgumentException: Unable to translate object to JSON: Optional.empty | ERROR: IllegalArgumentException: Provided type class java.util.Optional is a raw type, which is not acc... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty |
| null | ERROR: NullPointerException: value | ERROR: NullPointerException: value | N/A (value cannot be stored) | N/A (value cannot be stored) | N/A (value cannot be stored) | ERROR: NullPointerException: value |

The "after" matrix is added here once the fixes land.

## Translators, before

`TranslatorRoundTripTest`: `set(path, value)`, then `getObject(path, type)` from memory, after each format, and on
a `CompoundTagDataView`, compared with `equals`.

| Value | stored as | memory | NBT | JSON | HOCON | CompoundTagDataView |
|---|---|---|---|---|---|---|
| UUID | PASS: DataView [UuidLeast, UuidMost] | PASS | PASS | PASS | PASS | PASS |
| Vector2d | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Vector2f | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Vector2i | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Vector2l | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Vector3d | PASS: DataView [x, y, z] | PASS | PASS | PASS | PASS | PASS |
| Vector3d -0.0 | PASS: DataView [x, y, z] | PASS | LOSSY: (-0.0, 0.0, 0.0) -> (0.0, 0.0, 0.0) | PASS | LOSSY: (-0.0, 0.0, 0.0) -> (0.0, 0.0, 0.0) | LOSSY: (-0.0, 0.0, 0.0) -> (0.0, 0.0, 0.0) |
| Vector3f | PASS: DataView [x, y, z] | PASS | PASS | PASS | PASS | PASS |
| Vector3i | PASS: DataView [x, y, z] | PASS | PASS | PASS | PASS | PASS |
| Vector3l small values | PASS: DataView [x, y, z] | PASS | PASS | PASS | PASS | PASS |
| Vector3l large values | PASS: DataView [x, y, z] | PASS | PASS | PASS | PASS | PASS |
| Vector4d | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| Vector4f | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| Vector4i | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| Vector4l | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| Complexd | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Complexf | PASS: DataView [x, y] | PASS | PASS | PASS | PASS | PASS |
| Quaterniond | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| Quaternionf | PASS: DataView [x, y, z, w] | PASS | PASS | PASS | PASS | PASS |
| LocalTime | PASS: DataView [LocalTimeHour, LocalTimeMinute, LocalTimeSecond, LocalTimeNano] | PASS | PASS | PASS | PASS | PASS |
| LocalDate | PASS: DataView [LocalDateYear, LocalDateMonth, LocalDateDay] | PASS | PASS | PASS | PASS | PASS |
| LocalDateTime | PASS: DataView [LocalDateYear, LocalDateMonth, LocalDateDay, LocalTimeHour, LocalTimeMinute, LocalTimeSecond, LocalTimeNano] | PASS | PASS | PASS | PASS | PASS |
| Instant | PASS: DataView [LocalDateYear, LocalDateMonth, LocalDateDay, LocalTimeHour, LocalTimeMinute, LocalTimeSecond, LocalTimeNano] | PASS | PASS | PASS | PASS | PASS |
| ZonedDateTime offset | PASS: DataView [LocalDateYear, LocalDateMonth, LocalDateDay, LocalTimeHour, LocalTimeMinute, LocalTimeSecond, LocalTimeNano, ZoneDateTimeId] | ERROR: InvalidDataException: Unrecognized ZoneId: +02:00 | ERROR: InvalidDataException: Unrecognized ZoneId: +02:00 | ERROR: InvalidDataException: Unrecognized ZoneId: +02:00 | ERROR: InvalidDataException: Unrecognized ZoneId: +02:00 | ERROR: InvalidDataException: Unrecognized ZoneId: +02:00 |
| ZonedDateTime region | PASS: DataView [LocalDateYear, LocalDateMonth, LocalDateDay, LocalTimeHour, LocalTimeMinute, LocalTimeSecond, LocalTimeNano, ZoneDateTimeId] | PASS | PASS | PASS | PASS | PASS |
| Month | PASS: DataView [LocalDateMonth] | PASS | PASS | PASS | PASS | PASS |
| Direction NORTH | PASS: DataView [Direction] | PASS | PASS | PASS | PASS | PASS |
| Direction SOUTH_SOUTHWEST | PASS: DataView [Direction] | PASS | PASS | PASS | PASS | PASS |
| Component text | LOSSY: raw TextComponentImpl (no translator for TextComponentImpl) | LOSSY: absent after read | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hell... | ERROR: IllegalArgumentException: Unable to translate object to JSON: TextComponentImpl{content="hello",... | ERROR: IllegalArgumentException: DataView value type of class net.kyori.adventure.text.TextComponentImp... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hell... |
| Component translatable | LOSSY: raw TranslatableComponentImpl (no translator for TranslatableComponentImpl) | LOSSY: absent after read | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TranslatableComponentImpl{key="... | ERROR: IllegalArgumentException: Unable to translate object to JSON: TranslatableComponentImpl{key="blo... | ERROR: IllegalArgumentException: DataView value type of class net.kyori.adventure.text.TranslatableComp... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TranslatableComponentImpl{key="... |
| Component empty | LOSSY: raw TextComponentImpl (no translator for TextComponentImpl) | LOSSY: absent after read | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="", s... | ERROR: IllegalArgumentException: Unable to translate object to JSON: TextComponentImpl{content="", styl... | ERROR: IllegalArgumentException: DataView value type of class net.kyori.adventure.text.TextComponentImp... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="", s... |

## DataSerializable, before

`SerializableRoundTripTest`: `toContainer()`, the format, `DataManager.deserialize`, then the rebuilt object's
`toContainer()` compared with the original. `PASS (container: …)` means the container changed in the format but the
builder tolerated it.

| Value | memory | NBT | JSON | HOCON |
|---|---|---|---|---|
| ItemStack simple | PASS | PASS | PASS | PASS |
| ItemStack named, enchanted, damaged | PASS | PASS | PASS | PASS |
| ItemStack with unbreakable flag | PASS | PASS | PASS | PASS |
| ItemStackSnapshot | PASS | PASS | PASS | PASS |
| BlockState default | PASS | PASS | PASS | PASS |
| BlockState with properties | PASS | PASS | PASS | PASS |
| GameProfile name only | PASS | PASS | PASS | PASS |
| GameProfile with signed property | LOSSY: rebuilt keys [ContentVersion, UUID, Properties, Name] -> [ContentVersion, UUID, Name] | LOSSY: rebuilt keys [ContentVersion, UUID, Properties, Name] -> [ContentVersion, UUID, Name] | LOSSY: rebuilt keys [ContentVersion, UUID, Properties, Name] -> [ContentVersion, UUID, Name] | LOSSY: rebuilt keys [ContentVersion, UUID, Properties, Name] -> [ContentVersion, UUID, Name] |
| ProfileProperty unsigned | PASS | PASS | PASS | PASS |
| Enchantment | ERROR: toContainer: ValueNotFoundException: No 'minecraft:enchantment' registry has been defined in root 'minecraf... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:enchantment' registry has been defined in root 'minecraf... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:enchantment' registry has been defined in root 'minecraf... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:enchantment' registry has been defined in root 'minecraf... |
| PotionEffect | PASS | PASS | PASS | PASS |
| FireworkEffect | LOSSY: rebuilt Colors: size 1 -> 0 | LOSSY: rebuilt Colors: size 1 -> 0 | LOSSY: rebuilt Colors: size 1 -> 0 | LOSSY: rebuilt Colors: size 1 -> 0 |
| Color | PASS | PASS | PASS | PASS |
| BannerPatternLayer | ERROR: toContainer: ValueNotFoundException: No 'minecraft:banner_pattern' registry has been defined in root 'minec... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:banner_pattern' registry has been defined in root 'minec... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:banner_pattern' registry has been defined in root 'minec... | ERROR: toContainer: ValueNotFoundException: No 'minecraft:banner_pattern' registry has been defined in root 'minec... |
| EntityArchetype pig | PASS | PASS | PASS | PASS |
| BlockEntityArchetype chest | PASS | PASS | PASS | PASS |
| PluginRecord (test type, version 2) | PASS | PASS (container: Flags: [0] Boolean(true) -> Byte(1)) | PASS (container: B: Byte(-7) -> Integer(-7)) | PASS (container: B: Byte(-7) -> Integer(-7)) |

Content updater (`PluginRecord`, version 1 → 2 through `DataManager.registerContentUpdater`):

| Value | memory | NBT | JSON | HOCON |
|---|---|---|---|---|
| PluginRecord version 1 (updated to 2) | PASS | PASS | PASS | PASS |
| PluginRecord version 2 | PASS | PASS | PASS | PASS |

World-bound types, `LosslessGameTests#worldSerializablesRoundTrip` (columns memory, NBT, JSON, HOCON):

| Value | memory | NBT | JSON | HOCON |
|---|---|---|---|---|
| ServerLocation | LOSSY: rebuilt X: 7.25 -> 7.0 | LOSSY: same | LOSSY: same | LOSSY: same |
| BlockSnapshot chest | PASS | PASS | PASS | PASS |
| EntitySnapshot pig | PASS | PASS | LOSSY: rebuilt Data: Invulnerable: Byte(0) -> Integer(0) | LOSSY: same |

## Custom data, before

`CustomDataRoundTripTest`: a key registered with `DataRegistration.of(key, ItemStack.class)`, offered to an
`ItemStack`. **holder**: `get` after `offer`; **deserialize**: `DataManager.deserialize(ItemStack.class,
stack.toContainer())`; **builder**: `ItemStack.builder().fromContainer(…).build()`; NBT/JSON/HOCON: as deserialize,
through that format.

| Value | holder | deserialize | builder | NBT | JSON | HOCON |
|---|---|---|---|---|---|---|
| Boolean | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Byte | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Short | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Integer | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Long (fits int) | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Long MAX | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Float | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Double | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Double -0.0 | PASS | LOSSY: Double(-0.0) -> Double(0.0) | LOSSY: absent after read | LOSSY: Double(-0.0) -> Double(0.0) | LOSSY: Double(-0.0) -> Double(0.0) | LOSSY: Double(-0.0) -> Double(0.0) |
| String | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| String 70k chars | PASS | PASS | LOSSY: absent after read | LOSSY: String(xxxxxxxxxxxxxxxxxxxxxxxx...(70000)) -> String() | PASS | PASS |
| UUID | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Vector3d | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Vector3i | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Instant | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Month (translated enum) | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| enum (plain) | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: BETA |
| Component | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: TextComponentImpl{content="hi",... |
| ResourceKey | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| registry value (ItemType) | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| ItemStackSnapshot | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| DataView | PASS | LOSSY: b: Boolean(true) -> Byte(1) | LOSSY: absent after read | LOSSY: b: Boolean(true) -> Byte(1) | LOSSY: b: Boolean(true) -> Integer(1) | LOSSY: b: Boolean(true) -> Integer(1) |
| List<Boolean> | PASS | LOSSY: [0] Boolean(true) -> Byte(1) | LOSSY: absent after read | LOSSY: [0] Boolean(true) -> Byte(1) | LOSSY: [0] Boolean(true) -> Integer(1) | LOSSY: [0] Boolean(true) -> Integer(1) |
| List<Integer> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| List<Long> | PASS | PASS | LOSSY: absent after read | PASS | LOSSY: [0] Long(1) -> Integer(1) | LOSSY: [0] Long(1) -> Integer(1) |
| List<Float> | PASS | PASS | LOSSY: absent after read | PASS | LOSSY: [0] Float(1.5) -> Double(1.5) | LOSSY: [0] Float(1.5) -> Double(1.5) |
| List<String> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| List<UUID> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Set<String> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Map<String,Integer> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Map<String,Boolean> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |
| Map<UUID,String> | PASS | PASS | LOSSY: absent after read | PASS | PASS | PASS |

Entity custom data through the entity's own save and load (`LosslessGameTests#entityCustomDataSurvivesSaveAndLoad`,
saved tag written and read with `NbtIo`): Boolean, Byte, Long (fits int), Float, String and `Map<String,Boolean>`
PASS; `Double -0.0` LOSSY (`-0.0 -> 0.0`); `String 70k chars` LOSSY (`""`); `List<Boolean>` LOSSY
(`[0] Boolean(true) -> Byte(1)`). A plain enum: `offer` throws, and saving the entity afterwards throws
(`LosslessGameTests#entityWithUnsavableCustomData`).

## Plan

One finding per commit, in this order, each with its test flipped or added in the same commit. All are
**implementation-only** (Sponge `src/main` and `src/mixins`); **no SpongeAPI change is needed**, so the submodule
stays at `f7088f6`. Each commit re-runs `lossless.*`, the gametests it touches, and `Api17FixtureTest.read`, whose
entries may only go from their api-17 result to PASS.

| P | Finding | Change | Plugin-observable? | Compatibility with api-17 data | Tests flipped / added |
|---|---|---|---|---|---|
| P1 | X20, X20b (draft 04) | `NBTTranslator.fromTagBase`: a `ByteTag` whose key ends with `$Boolean` returns `Boolean` | **Yes:** `CompoundTagDataView.get`/`values` return `Boolean` instead of `Byte` (api-12 behavior); copies keep the marker | Marked booleans read as `Boolean`; bytes already stored without a marker still read as `Byte` | `#x20*` → Boolean; new `#x20b*` asserts the marker survives; matrix CompoundTagDataView cells for booleans L→P; custom data `DataView` row |
| P2 | X3, X12 (draft 03 step 1) | Match `$Boolean` only as a suffix and only on `ByteTag`; strip only the suffix (`NBTTranslator`, `CompoundTagDataView`) | Only for keys with `$Boolean` in the middle, which stop being renamed | Sponge only ever writes the suffix, so all Sponge-written data reads the same | `#x3*`, `#x12*` flipped |
| P3 | X7 (draft 02) | `JsonDataFormat.readArray`: convert through `Number` with range checks; out-of-range or non-numeric → `InvalidDataException` | Only previously unreadable files now read | `single-e036.json`, `single-e038.json` ERROR → PASS | `#x7*` flipped; matrix JSON `byte[]`, `long[] small` E→P |
| P4 | X1 (draft 01) | `NBTDataFormat.writeTo`: `NbtIo.writeUnnamedTag` instead of `NbtIo.write`, so an oversize string throws `UTFDataFormatException` (an `IOException`) | **Yes:** an exception instead of silently writing `""` | Reading unchanged | `#x1NbtLongStringsBecomeEmpty` → asserts the exception; matrix NBT `String 70k` L→E |
| P5 | X29 | `DataDeserializer.deserializeList`: boxed element types (`Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `String`) use the typed list getters | Custom data list keys return the declared element type | `List<Boolean>`, `List<Long>`, `List<Float>` custom data entries L→P | custom data table |
| P6 | X22 | `SpongeItemStack.Builder.build()`: after `applyComponents`, read Sponge data from the custom data tag (`DataUtil.syncTagToData`), as the codec constructor does | Builder-made stacks see their custom keys | toContainer output unchanged | custom data `builder` column L→P; `#x22*` flipped |
| P7 | X23 | `SpongeGameProfileDataBuilder`: read `Properties` with the `ProfileProperty` builder | Properties survive | api-17 containers already hold `Properties`, so fixtures L→P | serializable table |
| P8 | X24 | `FireworkExplosionMixin_API.toContainer`: write `colors()` like `fadeColors()`; `SpongeFireworkEffectDataBuilder`: also accept integer RGB elements | **Yes:** the stored shape of `Colors` changes from ints to `Color` views | api-17 data (ints) reads back its colors | serializable table; fixture `FireworkEffect` L→P; new test for an int-list container |
| P9 | X25 | `SpongeEnchantment.toContainer` and `BannerPatternLayers_LayerMixin_API.toContainer`: look the key up in the scoped holder | Serialization stops throwing | nothing was ever saved | serializable table E→P |
| P10 | X26 | `ZONED_DATE_TIME_DATA_SERIALIZER`: accept any id `ZoneId.of` parses; `DateTimeException` → `InvalidDataException` | Reading stops throwing | fixture `ZonedDateTime offset` E→P | translator table |
| P11 | X28 | `SpongeServerLocationBuilder`: read the position with `getDouble` | Fractional positions survive | api-17 data holds doubles, so it reads better | gametest `ServerLocation` L→P |
| P12 | X10 | `DataTranslatorProvider.getSerializer`: on a miss, look up registered superclasses and interfaces | **Yes:** `set(path, component)` stores the translator's `DataView`, so `get` returns a `DataView` and `getObject(path, Component.class)` the component; NBT/JSON/HOCON writes and custom `Component` keys stop failing. Also applies to `Schematic` values and to plugin translators registered for an interface. | nothing was ever saved | translator table; `#x10*`; custom data `Component` row E→P |
| P13 | X16 (enums), X27 | `NBTTranslator.getBaseFromObject` and `JsonDataFormat.write` write an enum without a translator as its `name()` (HOCON already does); `DataDeserializer` reads enum element and list-element types from a `String` | **Yes:** NBT and JSON writes of enums stop throwing, and the raw value read back is a `String` | nothing was ever saved | matrix enum rows E→L (raw `get` is a String; flagged); custom data enum row E→P; gametest `entityWithUnsavableCustomData` E→P |
| P14 | X21 (Float noise) | `ConfigurateTranslator.valueToNode`: write a `Float` as the `Double` with the same decimal digits (`Double.parseDouble(Float.toString(f))`) | `0.1f` saved to HOCON reads back as `0.1` instead of `0.10000000149011612`; `getFloat` gives the same value either way | api-17 files read the same | matrix HOCON Float rows change detail but stay L (width) |

**Not in the plan:** X2 and X6 (WONTFIX), X4, X5, X8, X9, X11, the rest of X16, X17 in raw NBT, X18/X21 widths and
arrays, H3/H4/H7/H8 (in-memory API behavior, not loss of saved data; changing any is observable).

## Issue drafts 01–04

I intend to fix all four in this task, as P4 (01), P3 (02), P2 (03, step 1 only: suffix matching; step 2, rejecting
colliding keys on write, turns a silent rename into an exception, so I'd leave it out unless you want it), and P1
(04). None is started.

## Open questions

1. **Confirm the observable changes:** P1 (`Byte` → `Boolean`), P4 (exception instead of `""`), P8 (new `Colors`
   shape), P12 (`Component` stored as a view; also schematics and interface translators), P13 (enums saved as
   names). Any you reject, I'll leave UNCHANGED and keep its pinned test.
2. **P4 alternative.** Instead of throwing, oversize strings could be stored losslessly with a new marker shape (for
   example split into a list under a `$LongString` key). That's a new on-disk shape that older builds would read as a
   list. I recommend the exception, as draft 01 does. Custom data saved by vanilla (`NbtIo` in chunk and player
   saving) would still write `""` either way; fixing that needs a Mixin on vanilla saving. In or out?
3. **Number widths in JSON and HOCON (X18, X21).** Preserving them needs type tags in the files, a new shape for
   every number. I propose WONTFIX: typed getters already recover the value, and P5 covers custom data lists.
4. **`-0.0` in NBT (X2).** Fixable only with a Mixin on vanilla's `FloatTag.valueOf`/`DoubleTag.valueOf`, which
   would change every NBT use in the game. I propose WONTFIX.
5. **`_arraytype` (X5).** Escaping user keys on write changes the JSON shape for such keys. Leave UNCHANGED?
6. **Other unwritable custom values (X27).** After P12/P13, `Optional`, `short[]`, `double[]` and the like can
   still poison an entity. Options: reject them at `offer` time (an exception from `offer`, which already happens on
   ItemStacks), or skip and log them at save time (silent loss, but the entity saves). Which, or neither for now?
7. **P13 read type.** Should `set(path, enum)` in memory also store the name, so memory and every format agree? That
   changes `get` from the enum to a `String` for in-memory views, which plugins may rely on; I'd leave memory as is.

## Test inventory

New on this branch (all under `src/test/java/org/spongepowered/common/data/lossless/` unless noted):

| Class | What it pins |
|---|---|
| `LosslessMatrixTest` (extended) | HOCON column; output through `PinnedTable` |
| `PinnedTable` | shared pinned-status table: writes markdown, prints the observed pins as Java on a mismatch |
| `TranslatorRoundTripTest` | 31 translator values × 6 columns |
| `SerializableRoundTripTest`, `PluginRecord` | 17 DataSerializable rows × 4 columns; content updater × 4 columns |
| `CustomDataRoundTripTest` | 32 custom data keys × 6 columns |
| `DataHolderFindingsTest` | X20b, X22 |
| `Api17FixtureTest` + `src/test/resources/…/lossless/api-17/` | compatibility with data written by api-17 |
| `src/gametest/java/org/spongepowered/common/test/gametest/data/LosslessGameTests` | entity custom data save/load, unsavable custom data, ServerLocation/BlockSnapshot/EntitySnapshot (registered in `SpongeGameTests.COMMON`) |

Run with:

```
./gradlew :SpongeVanilla:test --tests 'org.spongepowered.common.data.lossless.*'
./gradlew :SpongeVanilla:gameTest --tests '*LosslessGameTests*'
```
