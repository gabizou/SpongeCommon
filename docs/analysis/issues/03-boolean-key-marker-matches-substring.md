# NBT boolean key marker "$Boolean" is matched anywhere in a key, renaming and retyping data

**Labels (suggested):** `type: bug`, `system: data`

**Affected Product(s):** SpongeVanilla, SpongeForge, SpongeNeo (common code; tested on SpongeVanilla)
**Version:** built from `api-12` `69a889c` (Minecraft 1.21.1)
**Operating System:** Linux
**Java Version:** OpenJDK 21.0.12.1
**Plugins/Mods:** none (JUnit in a booted SpongeVanilla server)

## Describe the bug

NBT has no boolean type, so Sponge stores a boolean at key `k` as a byte tag under `k$Boolean`. On read, any key
that **contains** `$Boolean` anywhere is treated as that marker, and `$Boolean` is removed wherever it appears. A
key that merely contains the text is renamed, and a byte stored under it comes back as a `Boolean`. A boolean `x`
and a byte at `x$Boolean` also collide on the same NBT key, so one of them is lost.

Through `DataFormats.NBT` (`NBTTranslator`):

```java
DataContainer c = DataContainer.createNew().set(DataQuery.of("flag$Boolean"), (byte) 7);
DataContainer read = roundTripThroughNbt(c);
read.keys(false);                  // [flag]   (expected [flag$Boolean])
read.get(DataQuery.of("flag"));    // Optional[true]   (expected nothing at "flag")

DataContainer collide = DataContainer.createNew()
    .set(DataQuery.of("x"), true)
    .set(DataQuery.of("x$Boolean"), (byte) 0);
collide.keys(false).size();                  // 2
roundTripThroughNbt(collide).keys(false);    // 1 key, one value is lost
```

In `CompoundTagDataView`, which backs DataHolder custom data (`DataUtil.java:67,190`), the problem shows up without
any round trip:

```java
DataContainer c = new CompoundTagDataContainer(new CompoundTag()).set(DataQuery.of("x$Boolean"), 5);
c.keys(false);                 // [x]
c.get(DataQuery.of("x"));      // Optional[5]
```

**Expected:** only a trailing `$Boolean` marks a boolean, and only that suffix is stripped. A user key that would
collide with the marker is rejected (or escaped) on write, instead of silently changing meaning.

## Cause

- **`NBTTranslator`** (`src/main/java/org/spongepowered/common/data/persistence/NBTTranslator.java`):
  - **Write:** `containerToCompound` appends the suffix to boolean keys (`:83`, `:151`) and writes every other key
    unchanged, including user keys that already end in `$Boolean`.
  - **Read:** `setInternal` checks `key.contains(BOOLEAN_IDENTIFIER)` and `key.replace(BOOLEAN_IDENTIFIER, "")`
    (`:184-185`); `fromTagBase` also uses `contains` (`:257`).
- **`CompoundTagDataView`** (`src/main/java/org/spongepowered/common/data/CompoundTagDataView.java`):
  `streamRootKeys` and `streamRootValues` use `replace` (`:63`, `:69`), and `get` falls back to `key + "$Boolean"`
  (`:113-123`).
- **History:**
  - The `contains` check in `fromTagBase` (`:257`) arrived with `CompoundTagDataView` in `e83bc72` (2026-06-03).
  - The one in `setInternal` (`:184`) is older than the history available in the analysis clone, which is shallow
    at `2234afc`, 2026-05-27.
  - The `replace` on `:185` was last changed in `75458ad` (2026-05-27).

## Suggested fix

1. **Match only the suffix.** Use `key.endsWith(BOOLEAN_IDENTIFIER)` and strip only the trailing suffix
   (`key.substring(0, key.length() - BOOLEAN_IDENTIFIER.length())`) in `NBTTranslator` (`:184-185`, `:257`) and
   `CompoundTagDataView` (`:63`, `:69`). Also only treat it as a boolean when the tag is a `ByteTag`.
2. **Reject collisions on write.** In `containerToCompound` and in `CompoundTagDataView.set*`, reject (throw
   `IllegalArgumentException`) a non-boolean value whose key ends with `$Boolean`, and a boolean `k` when `k$Boolean`
   is also present.

**Compatibility:** Sponge itself only ever writes the marker as a suffix, so all data Sponge produced reads the same
with `endsWith`. Data with `$Boolean` in the *middle* of a key would stop being renamed; that is the bug being
fixed. Step 2 turns today's silent rename into an error; whether that belongs in api-12 or a later version is a
maintainer call.

## Link to logs

N/A. Nothing is logged.

**Reproducers:** `AdditionalFindingsTest#x3NbtBooleanSuffixKeys` and `#x12CompoundBooleanSuffixKeys`, on
`gabizou/SpongeCommon` branch `analysis/dataview-lossless`.
