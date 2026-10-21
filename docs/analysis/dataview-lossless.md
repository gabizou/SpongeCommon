# DataView lossless analysis (api-12)

Which Java values survive a round trip through Sponge's `DataView`, in memory and through the NBT and JSON formats.
This feeds the proposed `DataCodec` / `VersionedCodec` API for SpongeAPI.

Every claim below is backed by a test in
`src/test/java/org/spongepowered/common/data/lossless/` and a file:line citation. Tests run inside a booted
SpongeVanilla server (`:SpongeVanilla:test`, ModLauncher + `TestInvocationInterceptor`), so `DataSerializer`, the
translator registry and `DataFormats` are real and nothing under test is mocked. Each test asserts the behavior
*as observed*: a passing test means the observation still holds, and a behavior change fails it.

Unless stated otherwise, line numbers refer to `src/main/java/org/spongepowered/common/data/` at the Sponge commit
below.

## Summary

### Findings that change the DataCodec design

1. **There is no null (H2).** Every `set` overload throws `NullPointerException`, and `get` returns `Optional.empty()`
   both for a missing key and for a path that goes through a scalar (H5). A codec needs its own convention for
   absent vs. null, probably "key omitted". `Optional.empty()` is accepted, but it is stored as a raw `Optional` that
   neither NBT nor JSON can write (matrix, X16).
2. **`set(DataQuery, Object)` normalizes values instead of storing them (H1).** Maps become `DataView`s, collections
   become `ImmutableList`s, types with a registered translator (UUID, Month, vectors, ...) become views, and enums
   without a translator, `Optional`, `Component` and `short[]`/`double[]`/`Object[]` are stored raw. The raw ones fail
   later, when the data is saved (X10, X16). A codec should write only the subset every backend preserves (below)
   and encode UUIDs, enums and maps itself, instead of relying on `DataSerializer`.
3. **The lossless subset is small, and it depends on the format.** Here "lossless" means the same runtime class and
   value comes back:
   - **Lossless in memory, NBT and JSON:** `Boolean`, `Integer`, `Long` outside the int range, `Double` (finite, not
     `-0.0`), `String` (≤ 65535 UTF-8 bytes), `int[]`, `long[]` with values outside the int range, `List<Integer>`,
     `List<Double>`, `List<String>`, empty `List`, `List<List<..>>`, and a nested `DataView` whose leaves are in this
     subset.
   - **JSON collapses number widths:** `Byte`, `Short` and small `Long` come back as `Integer`; `Float` comes back as
     `Double`.
   - **JSON cannot write NaN or ±Infinity at all.** One such value fails the whole document (X6).
   - **JSON writes `byte[]` and small-valued `long[]` that its own reader cannot read back** (X7).
   - **NBT turns `-0.0` into `0.0`** (X2).
   - **NBT silently writes strings over 65535 bytes as `""`** (X1).
   - **NBT reads a `List<Boolean>` back as `List<Byte>`, and cannot write mixed lists** (X17).

   **Design consequence:** decoders must accept any integral `Number` and range-check it, and any `Number` for
   floating point. They must not rely on the exact boxed type. A `VersionedCodec` cannot assume `decode(encode(x))`
   is identical across formats unless it restricts itself to the subset above.
4. **Typed getters are not strict (H9).** `getInt`, `getLong` and so on narrow numbers, parse strings (`""` becomes
   `0`, `"1-2"` becomes `0`, `"(9, 10)"` becomes `9`) and turn anything into a `String`. List getters drop elements
   that fail to convert. A codec that needs to report type errors cannot use them; it must use `get()` plus
   `instanceof` and its own range checks. **Decided:** SpongeAPI gains new strict getters alongside the existing
   ones (see Decisions).
5. **Path shape errors are invisible or inconsistent (H3, H4, H5).**
   - `set` through a scalar intermediate silently replaces the scalar.
   - `createView` through it throws `ClassCastException` (MemoryDataView) or replaces it (CompoundTagDataView).
   - `get` through it looks like absence.

   A decoder that reads `a.b` when `a` is accidentally a scalar sees "missing", not "wrong type". To give
   path-accurate errors, codecs need to walk the path one level at a time.
6. **Keys have hidden reserved substrings and aliasing.**
   - **NBT `$Boolean`:** any key *containing* `$Boolean` is rewritten by the NBT translator and by
     `CompoundTagDataView` (X3, X12).
   - **JSON `_arraytype`:** a map containing the key `_arraytype` is hijacked by the JSON reader (X5).
   - **Dotted map keys:** `createView(path, map)` splits them into nested paths, while `set(path, map)` does not (X8).
   - **Key collisions:** map keys `1` and `"1"` collapse into one entry (X9).
   - **Unsupported key types:** `Map` keys that are not String, Integer, UUID, enum, ResourceKey, DataQuery or a
     registry value throw (matrix: `Map<Long,?>`).

   A codec should take only `String` keys and reject or escape `$Boolean` and `_arraytype`.
7. **`DataView.equals` cannot verify round trips (H7, H8).**
   - **Arrays:** `MemoryDataView` compares arrays by identity, so a view is not equal to its own `copy()`.
   - **Across implementations:** `MemoryDataContainer.equals(CompoundTagDataContainer)` can be `true` while their
     `hashCode`s differ.
   - **Paths:** equality includes the view's path, so a copied subtree never equals its source.

   Codec tests and migration checks need a structural comparator. `LosslessSupport.difference` in the test sources
   is one.

### Findings that do not change the design

- **H6:** `copy()` re-runs `set` and therefore `DataSerializer`. For data that is already normalized this has no
  effect; only array aliasing under `NO_DATA_CLONED` is observable.
- **Fixable implementation bugs:**
  - `CompoundTagDataView.copy(safety)` ignores its argument (H6).
  - `CompoundTagDataView` hands out internal arrays despite `ALL_DATA_CLONED` (X11).
  - `Component` values are never translated because translators are looked up by exact class (X10).
  - JSON `null` input causes an NPE (X4), and JSON exponent integers are not parsed (X15).
  - An empty key is accepted for scalars but rejected for views (X14).
- **Javadoc errors**, fixed in the SpongeAPI commit:
  - The typed getter and list getter contracts describe behavior the implementation doesn't have.
  - `SafetyMode.CLONED_ON_SET` repeats the `ALL_DATA_CLONED` description (X19).

## Versions and setup

| Item | Value |
|---|---|
| Sponge | `upstream/api-12` = `69a889ce4aec6b3433b3c4e9ed0905c4c7d1c7db` ("Merge pull request #4318 from SpongePowered/feat/game-tests-api-12") |
| SpongeAPI | `fb073c867e3f8d9da87acba15a6178bc261badad` ("Add CAN_PICK_UP_LOOT key"), `version=12.1.0-SNAPSHOT` |
| Pairing | Sponge's `SpongeAPI` git submodule is pinned to `fb073c8`, which is also the `upstream/api-12` tip of SpongeAPI. Sponge builds it as a composite included build, not a published artifact: `settings.gradle.kts:60-62` substitutes `org.spongepowered:spongeapi` with the included project, and `apiVersion` is read from `SpongeAPI/gradle.properties` (`settings.gradle.kts:48`). `.gitmodules` still says `branch = api-11`, but that is stale and has no effect. |
| Minecraft | 1.21.1 (`gradle.properties:14`) |
| Gradle | wrapper 8.14.4 |
| JDK | OpenJDK 21.0.12.1 (Ubuntu build) |
| JUnit | 5.10.2 (`SpongeAPI/gradle/libs.versions.toml:15`) |
| Test harness | Root `test` is disabled (`build.gradle.kts:404-407`). Tests run through `:SpongeVanilla:test` (`vanilla/build.gradle.kts:505-531`), which boots a dedicated server through ModLauncher; `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` wraps each test in a server tick phase. |

The SpongeAPI branch `analysis/dataview-lossless` adds two documentation-only commits on top of `fb073c8`:
`c0270fa` and `a0b15b7`. All results in this report were first produced against `fb073c8`. After the decisions
below, the Sponge branch's submodule was bumped to `a0b15b7` in a separate commit, and the data test suite was
re-run against it with the same result (205 tests, 0 failures, 11 skipped).

**Submodule caveat:** `.gitmodules` still points at `https://github.com/SpongePowered/SpongeAPI.git`, and `a0b15b7`
exists only on `gabizou/SpongeAPI`. A fresh clone of this Sponge branch has to fetch the submodule commit from the
fork:

```
git -C SpongeAPI fetch https://github.com/gabizou/SpongeAPI analysis/dataview-lossless
git submodule update
```

## Files read (step 1)

| File | Role |
|---|---|
| `MemoryDataView.java` | **Storage.** A `LinkedHashMap<String,Object>` (`:51`). `get` walks the path one part at a time (`:119-144`) and, under `ALL_DATA_CLONED`, clones arrays on read (`:146-163`). The primitive `set` overloads go through `setScalar` and store directly (`:179-230`, `:249-296`). `set(DataQuery|String, Object)` goes through `set0`, which stores String and Integer directly and sends everything else to `DataSerializer` (`:165-177`, `:239-247`, `:298-307`). Nested writes go through `setChild` (`:232-237`). `createView` is at `:328-356`, `copy` at `:384-406`, and `equals`/`hashCode` at `:413-430`. |
| `MemoryDataContainer.java` | **Root `MemoryDataView`.** Default `ALL_DATA_CLONED` (`:46-48`), `parent()` is empty (`:62-64`), and the `set` overrides only narrow the return type (`:66-159`). |
| `SpongeDataView.java` | **Shared base.** `values(deep)` (`:137-158`); typed getters through `Coerce` (`:164-202`); `createView(path, map)` splits keys on `.` (`:205-217`); `getMap` (`:220-234`); list getters through `getUnsafeList` and per-element `Coerce` with drops (`:262-404`); `equals` on `values(true)` and path, with no `hashCode` (`:500-507`). |
| `CompoundTagDataView.java` / `CompoundTagDataContainer.java` | **A `DataView` backed directly by a Minecraft `CompoundTag`.** This is what DataHolder custom data persistence uses (`DataUtil.java:67,190`). Recent: added on api-12 in `e83bc72` ("Introduce CompoundTagDataView", aromaa, 2026-06-03), and switched into `DataUtil` in `08d894b` the same day. These were direct commits on api-12, not a merge, and they were carried forward to api-17. `SpongeDataView` itself was extracted from `MemoryDataView` just before, in `4ca0005` (2026-06-03). Before that series `MemoryDataView` was the only implementation, so the cross-implementation inconsistencies (H4's two behaviors, H7, H8, X11, X12) are about 4.5 months old. Booleans are stored under `key + "$Boolean"` (`:150-157`, `:234-237`) and the suffix is stripped on read (`:63`, `:69`, `:113-123`). Other `set(Object)` values go through `DataSerializer`, then `NBTTranslator.getBaseFromObject` (`:240-243`). `copy(safety)` is at `:377-379`, and `equals`/`hashCode` on compound and path at `:386-402`. |
| `DataSerializer.java` | **Decides what an Object becomes.** `DataView` → copied into a child view (`:58-60`); `DataSerializable` → `toView` (`:61-62`); a translator registered for the exact class → view (`:64-70`); registry value → key string (`:72-81`); `ResourceKey` → string (`:82-83`); `Collection` → `ImmutableList` of recursively serialized elements (`:84-85`, `:103-109`); `Map` → child view, with keys through `serializeMapKey` (`:86-87`, `:96-101`, `:111-135`); arrays → cloned unless `NO_DATA_CLONED` (`:88-89`, `:137-168`); anything else stored raw (`:91`). |
| `registry/provider/DataTranslatorProvider.java` | **Translator table** (UUID, vectors, `java.time` types, Month, Direction, CompoundTag, Component, ...), held in an `IdentityHashMap` keyed by exact class. |
| `builder/Coerce.java` | **Lenient conversion.** `asString` = `toString()` (`:55-63`); `asBoolean` accepts only Boolean or Byte (`:71-78`); the number methods use `Number.xValue()` narrowing, then string parsing with `sanitiseNumber` (`:88-243`, `:275-297`); `asChar` takes the first character (`:251-264`). |
| `persistence/NBTTranslator.java` | **DataView ↔ CompoundTag.** Write: booleans get a `$Boolean` key suffix (`:83`); scalars, `byte[]`/`int[]`/`long[]` and their boxed arrays, `List` → `ListTag`, `Map`, `DataSerializable`, `DataView`; anything else throws `IllegalArgumentException` (`:91-168`). Read: `setInternal`, where a key *containing* `$Boolean` becomes a boolean (`:178-243`); list elements via `fromTagBase` (`:254-293`); output container is `NO_DATA_CLONED` (`:170-175`). |
| `persistence/NBTDataFormat.java` | **Binary NBT.** `NbtIo.read`/`NbtIo.write` around `NBTTranslator` (`:44-75`). In 1.21.1, `NbtIo.write` uses `writeUnnamedTagWithFallback`, and `NbtIo$StringFallbackDataOutput.writeUTF` catches `UTFDataFormatException` and writes `""` (checked with `javap` on `joined-1.21.1.jar`). |
| `persistence/JsonDataFormat.java` | **JSON.** Write: `values(false)`, keys via `asString('.')`, numbers via strict Gson `JsonWriter.value(Number)`, arrays as `{"_arraytype":..,"value":[..]}` (`:225-325`). Read: keys via `DataQuery.of(name)` with no splitting (`:107`); nested objects go into `createView` and `_arraytype` is detected (`:109-117`, `:132`); numbers become Double if they contain `.`, else int or long (`:176-190`); `null` is passed to `set` (`:119`, `:137-139`); arrays are rebuilt with casts (`:149-174`). |
| `persistence/DataViewJsonWriter.java` | **Gson `JsonWriter` that builds a DataView.** Used only by `JsonDataFormat.serialize(Gson, Object)` and not on the `DataFormats.JSON` path; not exercised here. |
| `SpongeAPI/.../persistence/DataView.java` | **Contracts.** `get`: absent if the path is not defined (`:161-168` before the change). The `set` Javadoc was silent on null and conversion. The typed getters (`:458-551`) and list getters (`:603-745`) promised "not an instance of X → absent". `createView` says existing data at the path is overwritten (`:386-419`), and is silent on intermediates. `SafetyMode` is at `:930-946`. |
| `SpongeAPI/.../persistence/DataQuery.java` | **Paths.** `of(String...)` keeps each argument as a single part with no splitting (`:119-124`); `of(char, String)` splits (`:109-111`); `parts()` is immutable (`:139-146`). Nothing in the Javadoc says a part may not contain the separator. |

## Hypothesis verdicts

| # | Verdict | Evidence | Test class |
|---|---|---|---|
| H1 | **CONFIRMED** | Primitive overloads store the boxed value (`MemoryDataView.java:205-229`). The Object overload stores String/Integer directly (`:299-301`) and routes the rest through `DataSerializer.serialize` (`:304`). Boxed `Long`, `Float` and so on pass through unchanged (`DataSerializer.java:91`), so for scalars both overloads give the same result. The difference shows for collections (copied into `ImmutableList`), maps, translated types and arrays. The String-key overloads behave the same (`:239-307`). | `H1SetOverloadRoutingTest` (6 tests) |
| H2 | **CONFIRMED** | `requireNonNull(value)` in every overload (`MemoryDataView.java:168,181,242`; `CompoundTagDataView.java:128,222`). A nested null is rejected before any intermediate view is created. `Optional.empty()` is stored as a raw Optional. | `H2NullRejectedTest` (7) |
| H3 | **CONFIRMED** | `setChild` uses `getUnsafeView(..).orElseGet(createView)` (`MemoryDataView.java:232-237`), and `createView(String)` overwrites (`:352-356`). The primitive overloads take the same path (`:224-229`), and so does `CompoundTagDataView` (`:135`, `:358-360`). Lists at the intermediate key are replaced too. | `H3SetChildReplacesScalarTest` (4) |
| H4 | **CONFIRMED** (inconsistent with H3) | `(DataView) this.map.get(key)` (`MemoryDataView.java:343`) throws `ClassCastException`, and the scalar survives. `createView(path, map)` inherits this (`SpongeDataView.java:207`). Single-part `createView` overwrites as documented. **`CompoundTagDataView` does not throw**: it replaces (`:324`, `:358-360`). So the two implementations disagree, and so do `set` and `createView` within `MemoryDataView`. | `H4CreateViewOverScalarTest` (5) |
| H5 | **CONFIRMED** | `MemoryDataView.java:137-140`, `CompoundTagDataView.java:110`. `get`, `getInt`, `getView` and `contains` return the same values as for an empty container. `remove` through a scalar is a silent no-op (`MemoryDataView.java:316-319`). | `H5GetThroughScalarTest` (3) |
| H6 | **CONFIRMED** | `copy()` calls `container.set(query, get(query))` for each root key (`MemoryDataView.java:384-406`), so `DataSerializer` runs again under the **copy's** safety mode. Observable effects: under `NO_DATA_CLONED` the copy shares arrays with the original (a write through the copy changes the original); `copy(CLONED_ON_SET)` clones them; nested views take the copy's safety mode. For already-normalized data a second pass changes nothing. Also: `CompoundTagDataView.copy(safety)` ignores `safety` (`:378`), and a copied child is never `equals` to its source because of the path. | `H6CopyReserializesTest` (5) |
| H7 | **CONFIRMED** | **Same class:** comparison uses the raw entry set (`MemoryDataView.java:428-429`) and is consistent with `hashCode` (`:415`). **Different classes:** it falls back to `SpongeDataView.equals` on `values(true)` (`MemoryDataView.java:423-424` → `SpongeDataView.java:500-507`), and `SpongeDataView` defines no `hashCode`. `MemoryDataContainer` and `CompoundTagDataContainer` with the same content compare **equal in both directions** but have **different hashCodes** whenever the content has a boolean (stored as `v$Boolean`) or a nested view. For flat int/String content the hashes match only by coincidence, because `IntTag`/`StringTag` hash like `Integer`/`String`. A `HashSet` then holds two "equal" views. The cross-class fallback can never match arrays under `ALL_DATA_CLONED`, because `get` clones them. `equals` ignores safety mode. | `H7EqualsHashCodeTest` (6) |
| H8 | **CONFIRMED for `MemoryDataView`, REFUTED for `CompoundTagDataView`** | **In memory:** arrays live in the raw map and are compared with `Object.equals`, so two views with equal `int[]` contents are unequal, a view is unequal to its own `copy()`, and nested arrays make the parents unequal. **NBT-backed:** `CompoundTagDataView` compares `ByteArrayTag`/`IntArrayTag`, which use `Arrays.equals`/`Arrays.hashCode`, so equal contents **are** equal there. | `H8ArrayIdentityTest` (4) |
| H9 | **CONFIRMED** | Getters at `SpongeDataView.java:164-202`, list getters at `:262-404`, `Coerce.java` as characterized in the next table. | `H9CoerceTest` (9) |

### H9: Coerce behavior

| Stored value | Getter | Result | Cause |
|---|---|---|---|
| `1L << 32` | `getInt` | `0` | `Number.intValue()` (`Coerce.java:93-95`) |
| `1.9d` / `NaN` | `getInt` | `1` / `0` | same |
| `300` / `100000` | `getByte` / `getShort` | `44` / `-31072` | `:208-210`, `:183-185` |
| `+Infinity` | `getLong` | `Long.MAX_VALUE` | `:233-235` |
| `1e300d` | `getFloat` | `+Infinity` | `:155-157` |
| `"12"`, `"1.5"` | `getInt` | `12`, `1` | `:97-110` |
| `""`, `"   "`, `"1-2"` | `getInt`/`getLong`/`getShort` | `0` | `sanitiseNumber` returns `"0"` for empty input or an inner `-` (`:276-279`, `:292-294`) |
| `"1,000"`, `"(9, 10)"`, `"[9.5, 10.6]"` | `getInt`/`getDouble` | `1000`, `9`, `9.5` | `:281-296` |
| `"abc"` / `true` | `getInt` | absent | |
| `"NaN"` | `getDouble` | `NaN` | `Double.valueOf` (`:131`) |
| `(byte) 1`, `(byte) -5` | `getBoolean` | `true` | only `Boolean` or `Byte` accepted (`:71-78`) |
| `"true"`, `1` | `getBoolean` | absent | |
| `5`, `List.of(1,2)`, a `DataView` | `getString` | `"5"`, `"[1, 2]"`, `view.toString()` | `toString()` (`:55-63`) |
| `[1, "2", "x", 3.7, true]` | `getIntegerList` / `getBooleanList` / `getStringList` / `getCharacterList` / `getViewList` | `[1,2,3]` / `[true]` / all five stringified / `['1','2','x','3','t']` / `[]` | per-element `Coerce`, failures dropped |
| `List.of(Map, Map)` | `getMapList` | `[]` | `set` converted the maps to views (`DataSerializer.java:103-109`); `getMapList` filters `instanceof Map` (`SpongeDataView.java:387-394`) |

## Lossless matrix

**Columns:**
- **primitive overload:** `set(DataQuery, boolean|byte|short|int|long|float|double|String)` followed by `get`,
  compared with the input. N/A for other types.
- **Object overload:** `set(DataQuery, Object)` followed by `get`, compared with the input.
- **NBT round trip:** the Object-overload container is written with `DataFormats.NBT` to bytes and read back. The
  result is compared with **what the container held before writing**, so this column isolates the translator; the
  Object column already shows any in-memory conversion.
- **JSON round trip:** the same with `DataFormats.JSON` (`write` to a string, then `read`).
- **CompoundTagDataView (extra column):** `set(DataQuery, Object)` on a `CompoundTagDataContainer`, the view Sponge
  uses for DataHolder custom data, compared with the input.

**Comparison** (`LosslessSupport.difference`) is strict:
- The runtime class must match. `Long(1)` read back as `Integer(1)` is LOSSY.
- `Float`/`Double` use boxed `equals`: NaN equals NaN, and `-0.0` differs from `0.0`.
- Arrays are compared element by element.
- Lists and views are compared structurally; a view's path is ignored.
- A `Map` is never "the same" as a `DataView`.

**Rows:** those marked "(extra)" were added beyond the requested list.

**Pinning:** `LosslessMatrixTest` writes this table to `vanilla/build/test-run/lossless-matrix.md` and pins every
cell's status, so a behavior change fails the test.

| Value | primitive overload | Object overload | NBT round trip | JSON round trip | CompoundTagDataView (extra) |
|---|---|---|---|---|---|
| Boolean true | PASS | PASS | PASS | PASS | PASS |
| Boolean false | PASS | PASS | PASS | PASS | PASS |
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
| short[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@710f0d61 | ERROR: IllegalArgumentException: Unable to translate object to JSON: [S@5e49f917 | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [S@7083802a |
| double[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@a34d50a | ERROR: IllegalArgumentException: Unable to translate object to JSON: [D@3d9b528 | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [D@12c96505 |
| String[] (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@7e2735a6 | ERROR: IllegalArgumentException: Unable to translate object to JSON: [Ljava.lang.String;@521723ed | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: [Ljava.lang.String;@1c29bc72 |
| List<Boolean> | N/A | PASS | LOSSY: [0] Boolean(true) -> Byte(1) | PASS | LOSSY: [0] Boolean(true) -> Byte(1) |
| List<Byte> | N/A | PASS | PASS | LOSSY: [0] Byte(1) -> Integer(1) | PASS |
| List<Short> | N/A | PASS | PASS | LOSSY: [0] Short(1) -> Integer(1) | PASS |
| List<Integer> | N/A | PASS | PASS | PASS | PASS |
| List<Long> | N/A | PASS | PASS | LOSSY: [0] Long(1) -> Integer(1) | PASS |
| List<Float> | N/A | PASS | PASS | LOSSY: [0] Float(1.5) -> Double(1.5) | PASS |
| List<Double> | N/A | PASS | PASS | PASS | PASS |
| List<String> | N/A | PASS | PASS | PASS | PASS |
| List empty | N/A | PASS | PASS | PASS | PASS |
| List mixed [1, "a"] (extra) | N/A | PASS | ERROR: UnsupportedOperationException: Trying to add tag of type 8 to list of 3 | PASS | ERROR: UnsupportedOperationException: Trying to add tag of type 8 to list of 3 |
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
| nested DataView with boolean (extra) | N/A | PASS | PASS | PASS | PASS |
| Optional.empty() (extra) | N/A | PASS | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty | ERROR: IllegalArgumentException: Unable to translate object to JSON: Optional.empty | ERROR: IllegalArgumentException: Unable to translate object to NBTBase: Optional.empty |
| null | ERROR: NullPointerException: value | ERROR: NullPointerException: value | N/A (value cannot be stored) | N/A (value cannot be stored) | ERROR: NullPointerException: value |

### Notes on the matrix

- **UUID, Month, Map:** the NBT and JSON cells are PASS relative to the stored `DataView`, not the original object.
  Use `getObject(path, UUID.class)` / `getObject(path, Month.class)` to recover the value (X13). A map comes back as
  a `DataView`, and `getMap` returns `String` keys: the `Integer` key `1` comes back as `"1"`.
- **UUID key order:** the in-memory view lists the UUID fields as `UuidLeast, UuidMost`, the insertion order of the
  translator's `addTo` (`DataSerializers.java:156-159`). The CompoundTag view lists them as `UuidMost, UuidLeast`,
  in `CompoundTag`'s hash order. This is harmless, but key order is not preserved across implementations.
- **`nested DataView` in the Object column is PASS by content only.** The stored child is a new view with path
  `v`, so `DataView.equals` against the original container is `false` (H6, H7).
- **NBT `-0.0`:** `FloatTag.valueOf` and `DoubleTag.valueOf` compare with `fcmpl`/`dcmpl` against `0`, and both
  `0.0` and `-0.0` return the cached `ZERO` (X2). The same happens in `CompoundTagDataView`.
- **NBT 70k-char string:** no error is raised. The binary write logs "Failed to write NBT String" and writes `""`
  (X1). `CompoundTagDataView` in memory keeps the string; it would be emptied the same way when the chunk or player
  file is written with `NbtIo`.
- **`short[]`, `double[]`, `String[]`, plain enums, `Optional`** are all accepted by `set(Object)` and then make the
  **entire** NBT or JSON write fail with `IllegalArgumentException` (X16). The failure is reported far from where
  the value was set.

## Additional issues (beyond H1–H9)

All are reproduced in `AdditionalFindingsTest`.

| # | Severity | Issue | Evidence |
|---|---|---|---|
| X1 | **data loss** | **NBT binary write turns any string over 65535 modified-UTF-8 bytes into `""` without failing.** This includes 21846 three-byte characters. Keys are written with the same `writeUTF`, so in principle the same applies to keys. | `NBTDataFormat.java:61-75` → `NbtIo.write` → `NbtIo$StringFallbackDataOutput.writeUTF` (catches `UTFDataFormatException`, writes `""`) |
| X2 | silent corruption | **NBT loses the sign of `-0.0`** for both Float and Double. | `FloatTag.valueOf` / `DoubleTag.valueOf` return `ZERO` for `-0.0` (bytecode) |
| X3 | **silent corruption** | **NBT treats any key that *contains* `$Boolean` as a boolean marker.** A `Byte` at `flag$Boolean` comes back as `Boolean` at `flag`. A boolean `x` and a byte `x$Boolean` collide, and one of them is lost. | `NBTTranslator.java:83`, `:184-185` (`contains`, then `replace`) |
| X4 | error path | **JSON input containing `null` throws an NPE** instead of skipping the key or reporting a format error. | `JsonDataFormat.java:119`, `:137-139` → `MemoryDataView.java:168` |
| X5 | **silent corruption** | **A user map with the key `_arraytype` is read back from JSON as a primitive array.** With an unknown type name, the document becomes unreadable (`IllegalArgumentException`). | `JsonDataFormat.java:114-117`, `:132`, `:149-173` |
| X6 | **data loss** | **One NaN or ±Infinity anywhere makes `DataFormats.JSON.write` fail for the whole document.** | `JsonDataFormat.java:241-242`; Gson `JsonWriter` is strict by default |
| X7 | **data loss** | **JSON writes `byte[]` and `long[]` with int-range values successfully, but its own reader cannot read them back** (`ClassCastException`). The reader casts `Integer` to `Byte`/`Long`. | `JsonDataFormat.java:161-169`, `:184-188` |
| X8 | **silent corruption** | **`createView(path, map)` splits map keys on `.`**, so `{"a.b":1}` becomes `a → b`. `set(path, map)` keeps `a.b` as a single key. | `SpongeDataView.java:211,213` (`DataQuery.of('.', ..)`) vs `DataSerializer.java:124-126` (`DataQuery.of(..)`) |
| X9 | silent corruption | **Map keys `1` and `"1"` serialize to the same key, and the later one wins.** | `DataSerializer.java:111-129` |
| X10 | error path | **`Component` values are never translated.** The translator is registered for `Component.class`, but the lookup uses the exact runtime class (`TextComponentImpl`). Components are stored raw, and NBT and JSON writes then fail. | `DataTranslatorProvider` (`IdentityHashMap` lookup), `DataSerializer.java:64` |
| X11 | silent corruption | **`CompoundTagDataView` returns internal arrays even under `ALL_DATA_CLONED`.** Mutating the result changes the stored tag. `MemoryDataView` clones. | `CompoundTagDataView.java:113-117`, `NBTTranslator.java:271-289` (no clone) |
| X12 | silent corruption | **`CompoundTagDataView` strips `$Boolean` from any key.** An int stored at `x$Boolean` is listed and returned as `x`. | `CompoundTagDataView.java:63`, `:69`, `:113-123` |
| X13 | info | **`getObject` recovers translated types; `getMap` returns `String` keys.** | `SpongeDataView.java:482-487`, `:220-234` |
| X14 | error path | **An empty key `""` is accepted for scalars, but storing a map there throws `IllegalArgumentException`.** | `MemoryDataView.java:169` checks only for an empty *path*; `SpongeDataView.java:76` checks the child key |
| X15 | error path | **JSON numbers without `.` but with an exponent (`1E5`) throw `NumberFormatException`.** This only affects JSON from other producers; Sponge's writer never emits them. | `JsonDataFormat.java:180-184` |
| X16 | **data loss** | **Values that neither NBT nor JSON can write are accepted in memory and fail the whole save later:** enums without a translator, `Optional`, `Component`, `short[]`, `double[]` and `Object[]` (tested), and `float[]` and `boolean[]` (from the same code branches, not tested). | `DataSerializer.java:88-91`; `NBTTranslator.java:167`; `JsonDataFormat.java:260` |
| X17 | silent corruption / error path | **An NBT `List<Boolean>` reads back as `List<Byte>`, and mixed-type lists fail to write** (`UnsupportedOperationException` from `ListTag`). | `NBTTranslator.java:138-145`, `:256-260` |
| X18 | cosmetic (for consumers using typed getters) | **JSON collapses number widths:** `Byte`, `Short` and int-range `Long` come back as `Integer`; `Float` comes back as `Double`, so `0.1f` reads back as the double `0.1`, not `(double) 0.1f`. `getLong`/`getFloat` hide this. | `JsonDataFormat.java:176-190` |
| X19 | cosmetic (API docs) | **`CLONED_ON_SET` clones arrays on set only; `ALL_DATA_CLONED` clones on set and on every get.** The Javadoc gave both the same description. Only arrays are cloned: collections become `ImmutableList`s and views are copied in every mode. | `DataSerializer.java:139-142`, `MemoryDataView.java:147` |

## Fixes suggested (not made; `src/main` is unchanged)

- **`JsonDataFormat.readArray`** (`:161-169`): convert elements with `((Number) n).byteValue()`, `.intValue()` and
  `.longValue()` instead of casting. This fixes X7.
- **`JsonDataFormat.readView`** (`:119`): skip `null` values, or throw `InvalidDataException`. **`readNumber`**
  (`:180-189`): parse with `BigDecimal` and pick the narrowest exact type. **Number width:** to preserve it, either
  tag numbers the way arrays are tagged, or accept that JSON is lossy for widths and say so.
- **`JsonDataFormat.write`** (`:241-242`): reject non-finite values with a clear `InvalidDataException`, or encode
  them as tagged strings. **`_arraytype`:** escape user keys that collide with it.
- **`NBTTranslator`** (`:83`, `:184-185`) and **`CompoundTagDataView`** (`:63`, `:69`): use
  `endsWith(BOOLEAN_IDENTIFIER)` and strip only the suffix. Reject user keys that end with `$Boolean` on write.
- **`MemoryDataView.createView(DataQuery)`** (`:343`): handle a non-view intermediate the same way `setChild` does
  (replace it), or make both throw `IllegalStateException`. Do the same in `CompoundTagDataView`, so the two
  implementations match H3/H4.
- **Equality:** define `equals` and `hashCode` once in `SpongeDataView`, compare arrays by content, and remove the
  per-class overrides. Alternatively, make cross-class `equals` return `false`. Either way the `equals`/`hashCode`
  contract is restored (H7, H8).
- **`CompoundTagDataView.copy(safety)`** (`:378`): pass `safety`. Clone arrays on read under `ALL_DATA_CLONED`
  (X11).
- **`DataTranslatorProvider.getSerializer`:** fall back to supertype and interface lookup, so `Component` and other
  interface-typed translators are found (X10).
- **`SpongeDataView.createView(path, map)`** (`:211`, `:213`): use `DataQuery.of(key.toString())` to match
  `DataSerializer` (X8).
- **`DataSerializer.serializeMap`** (`:96-101`): throw on a key collision (X9).
- **Fail at set time:** `NBTTranslator.getBaseFromObject` and `JsonDataFormat.write` reject values that `set` accepted
  (X16). Rejecting them in `DataSerializer` instead would report the error where the value was set.

## SpongeAPI Javadoc commit

Two documentation-only commits on SpongeAPI branch `analysis/dataview-lossless`, both touching only
`src/main/java/org/spongepowered/api/data/persistence/DataView.java`:

- **`c0270fa`** (+117 / −59) corrects the getter contracts and documents current `set`/`get` behavior.
- **`a0b15b7`** (−3) removes the paragraph that promised `set` replaces a scalar intermediate. You chose "throw" as
  the intended behavior, so the Javadoc now leaves the case unspecified until the implementation changes.

What the Javadoc says now (line numbers as of `c0270fa`; after `a0b15b7`, lines from 186 on move up by 3):

- **`get(DataQuery)`** (`:161-172`): absent is also returned when an intermediate path part is not a view (H5).
- **`set(DataQuery, Object)`** (`:174-197`): null is rejected, with an `@throws NullPointerException` (H2); the
  stored value may be a converted form (H1). Intermediate replacement (H3) was documented in `c0270fa` and
  removed again in `a0b15b7`.
- **`getMap`** (`:466-478`): a `DataView` is returned as a `Map` with String keys.
- **Typed getters** (`:480-598`): replaced "not an instance of X → absent" with "cannot be converted → absent;
  conversion may be lossy" (H9).
- **List getters** (`:662-803`): replaced the absent promise with "elements that cannot be converted are left out"
  (H9).
- **`SafetyMode.CLONED_ON_SET`** (`:993-997`): now says data is cloned on set only (X19).

These commits **record current behavior** and do not choose what the behavior should be. The one exception is that
the H3 wording was removed to match decision 6.

## Commands run and results

All commands were run from the repository root on branch `analysis/dataview-lossless`.

| Command | Result |
|---|---|
| `git remote add upstream …; git remote add fork …; git remote set-url --push upstream DISABLED` (both repos); also `git remote set-url --push origin DISABLED` | `upstream` and `origin` push URLs are `DISABLED` |
| `git fetch upstream api-12` (both repos) | ok |
| `git ls-remote --heads fork` (both repos) | no `analysis/dataview-lossless` on either fork (17 and 37 heads) |
| `git checkout -b analysis/dataview-lossless upstream/api-12` (Sponge) / `… fb073c8` (SpongeAPI) | ok |
| `git config submodule.SpongeAPI.url /home/user/SpongeAPI && git -c protocol.file.allow=always submodule update --init SpongeAPI` | submodule checked out at `fb073c8`. Local config only; the direct GitHub submodule clone had failed in this container. |
| `./gradlew --no-daemon :SpongeVanilla:testClasses` | BUILD SUCCESSFUL in 4m 20s. First run; downloaded Minecraft 1.21.1 through VanillaGradle. |
| `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.lossless.*'` | **BUILD SUCCESSFUL**: 69 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.*'` | **BUILD SUCCESSFUL**: 205 tests, 0 failures, 0 errors, 11 skipped. The skips are the existing `@Disabled` tests: 9 in `EntityDataTest`, 2 in `ItemStackDataTest`. |
| Mutation check: flipped one pinned matrix cell and one H3 assertion, re-ran the lossless tests, then restored both | BUILD FAILED, `68 tests completed, 2 failed` (the two mutated tests). This shows the harness reports failures. |
| `./gradlew --no-daemon :spotlessJavaCheck` (Sponge; covers test sources) | BUILD SUCCESSFUL |
| `javap -c -p -cp ~/.gradle/caches/VanillaGradle/v2/jars/net/minecraft/joined/1.21.1/joined-1.21.1.jar net.minecraft.nbt.{FloatTag,DoubleTag,StringTag,CompoundTag,NbtIo,NbtIo\$StringFallbackDataOutput}` | confirmed X1 and X2 in Minecraft bytecode |
| `./gradlew --no-daemon javadoc checkstyleMain spotlessCheck` (SpongeAPI, after `c0270fa`, and again after `a0b15b7`) | BUILD SUCCESSFUL both times. No warnings in `DataView.java`; the warnings shown are pre-existing ones in other files. |
| `javap -c -p -cp ~/.gradle/caches/modules-2/files-2.1/com.mojang/datafixerupper/8.0.16/…/datafixerupper-8.0.16.jar com.mojang.serialization.codecs.OptionalFieldCodec` and `… com.mojang.serialization.Codec` | confirmed Mojang's optional-field behavior (decision 2) |
| `git log --first-parent upstream/api-12 -- …/CompoundTagDataView.java`; `git log -1 e83bc72` (not a shallow boundary) | history of `CompoundTagDataView` (decision 8) |
| Sponge submodule bumped to `a0b15b7`, then `./gradlew --no-daemon :SpongeVanilla:test --tests 'org.spongepowered.common.data.*'` | **BUILD SUCCESSFUL**: 205 tests, 0 failures, 0 errors, 11 skipped (unchanged) |

## INCONCLUSIVE or out of scope

- **No hypothesis is INCONCLUSIVE.** All ran in the existing booted-server harness.
- **Not exercised:**
  - `DataViewJsonWriter`, which is used only by `JsonDataFormat.serialize(Gson, Object)`.
  - HOCON and SNBT (`HoconDataFormat`, `SNBTDataFormat`, `ConfigurateTranslator`).
  - Forge and NeoForge platforms.

  The matrix logic in `LosslessMatrixTest.evaluate` can take extra columns for any of these.
- **X1 for very long keys** is inferred from `CompoundTag.writeNamedTag` calling the same `writeUTF`; it has no
  reproducer.

## Decisions (answered 2026-10-21)

Numbering follows the earlier open-questions list; the answers were given by topic.

| # | Topic | Decision | What it means for the DataCodec design |
|---|---|---|---|
| 1 | Encoding subset | **Portable subset only** | Codecs write only the values that survive memory, NBT and JSON: `Boolean`, `Integer`, `Long`, finite `Double`, `String` ≤ 65535 UTF-8 bytes, `int[]`, `long[]`, lists of these, nested views. Other values fail **at encode time**. UUID, enum, map and similar types get explicit codecs instead of going through `DataSerializer`. |
| 2 | Null / absence | **Omitted key, and no general null**; `Optional` fields are modelled by a codec | Matches Mojang DFU 8.0.16 (the version used by 1.21.1): `OptionalFieldCodec.encode` writes nothing for `Optional.empty()`; `decode` returns empty only when the key is **missing**; a key that is **present but invalid** is an error, unless `lenientOptionalFieldOf` is used. `optionalFieldOf(name, default)` also leaves the field out when the value equals the default, and decodes a missing field to the default (`Codec.lambda$optionalFieldOf$11/$12`). Because of H5, a DataCodec decoder has to check the parent's shape itself to tell "missing" apart from "wrong shape". |
| 3 | Number widths | **Lenient decoding** | Integral fields accept any whole-number `Number` and range-check it; floating-point fields accept any `Number`. `JsonDataFormat` on-disk output stays as it is. |
| 4 | NaN/±Infinity in JSON, strings > 65535 bytes in NBT | **Codec validates and rejects at encode time** | The error names the field. The formats are unchanged; the silent `""` fallback (X1) still affects code that doesn't use codecs. |
| 5 | Strict getters | **Add new strict getters to SpongeAPI alongside the existing ones** | The existing `Coerce`-based getters stay as they are for compatibility. The API addition is not done here; its shape is follow-up F1. |
| 6 | Scalar intermediates | **Throw** (for both `set` and `createView`, in both implementations) | Today's behavior differs: `set` replaces, `MemoryDataView.createView` throws `ClassCastException`, and `CompoundTagDataView` replaces. Implementing this is a behavior change in `src/main` and is not done here (follow-up F2). The Javadoc no longer promises replacement (`a0b15b7`). |
| 7 | Equality | **No API change; ship a structural comparison helper for codec tests** | `LosslessSupport.difference` is the prototype. Fixing `equals`/`hashCode` (H7, H8) remains an implementation bug to fix separately. |
| 8 | `CompoundTagDataView` in scope | **Yes** | Codec tests must run against both `MemoryDataContainer` and `CompoundTagDataContainer`. History: see the `CompoundTagDataView` row in "Files read". |
| 9 | Reserved keys | **Document `$Boolean` and `_arraytype` as reserved; codecs reject them** | No format change. |
| 10 | Submodule bump | **Bumped** to `a0b15b7` in a separate Sponge commit | See the submodule caveat under "Versions and setup". |

### Follow-ups raised by these decisions

- **F1, strict getter shape:** pick one of per-type methods (`getExactInt`, …), a single `get(path, Class)` /
  `get(path, Type)`, or a `DataResult`-style return that carries the error and path, as Mojang's codecs do. The
  third fits decisions 2 and 6 best, because the error can say "expected int at `a.b`, found String" or "`a` is not
  a view".
- **F2, when "throw on scalar intermediate" lands:** this changes `set` behavior for existing plugins. Options are
  api-12 (a behavior change in a stable line) or a later API version, with a deprecation note in api-12 Javadoc.
- **F3, lenient optional fields:** DFU offers both strict and lenient optional fields. Should DataCodec have a
  lenient variant (treat malformed as absent), or strict only?
- **F4, `src/main` fixes independent of the codec:** X1 (silent `""`), X7 (JSON can't read its own arrays) and X3/X12
  (`contains("$Boolean")`) affect all `DataView` users, not just codecs. Should they be filed or fixed separately?
