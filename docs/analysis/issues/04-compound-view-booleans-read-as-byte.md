# CompoundTagDataView returns booleans as Byte on api-17+ (lost in api-12 → api-17 merge)

**Labels (suggested):** `type: bug`, `system: data`

**Affected Product(s):** SpongeVanilla, SpongeForge, SpongeNeo (common code; tested on SpongeVanilla)
**Version:** built from `api-17` `5ecbb24` (Minecraft 1.21.10, SpongeAPI `17.1.0-SNAPSHOT` `f7088f6`); same code on api-18 through api-21. api-12 is **not** affected.
**Operating System:** Linux
**Java Version:** OpenJDK 21.0.12.1
**Plugins/Mods:** none (JUnit in a booted SpongeVanilla server)

## Describe the bug

`CompoundTagDataView` backs DataHolder custom data (`DataUtil.java:62,199`). On api-17 and later, a boolean
stored in it reads back as a `Byte`:

```java
final DataContainer c = new CompoundTagDataContainer(new CompoundTag()).set(DataQuery.of("v"), true);
c.get(DataQuery.of("v"));            // Optional[1]     (Byte; api-12 returns Optional[true])
c.values(false).get(DataQuery.of("v")); // 1 (Byte)
c.getBoolean(DataQuery.of("v"));     // Optional[true]  (Coerce.asBoolean accepts Byte, so this hides it)
c.equals(DataContainer.createNew().set(DataQuery.of("v"), true)); // false (api-12: true)
```

The same happens to booleans inside nested views. Booleans *inside lists* were already bytes on api-12, because
NBT has no boolean list type.

**Expected:** `get` returns `Boolean`, as on api-12 and as `MemoryDataView` does.

**Impact:**
- **`getBoolean` callers are unaffected**, because `Coerce.asBoolean` accepts a `Byte`.
- **Anything that reads the raw value sees a `Byte`:** `get`, `values()`, `streamRootValues()`, equality with an
  in-memory view, and code that copies custom data into another view or format. For example, copying it into a
  `MemoryDataContainer` and writing JSON gives `1` instead of `true`.

## Cause

`CompoundTagDataView.get(String)` (`src/main/java/org/spongepowered/common/data/CompoundTagDataView.java:112-122`)
looks up `key + "$Boolean"` and passes that key to `NBTTranslator.fromTagBase(Tag, String, BiFunction)`.

- **api-12:** `fromTagBase` turned a `ByteTag` under a `$Boolean` key into a `Boolean`. That check was added with
  `CompoundTagDataView` in `e83bc72` (2026-06-03).
- **api-17:** `fromTagBase` returns `bt.byteValue()` unconditionally
  (`src/main/java/org/spongepowered/common/data/persistence/NBTTranslator.java:258-261`). api-17 had already
  rewritten the method as a pattern-matching `switch` for the newer tag API (`25278b3`).
- **The merge:** when api-12 was merged into api-17 (`3bbe5c5`, "Merge remote-tracking branch 'origin/api-12' into
  api-17", 2026-06-05), the resolution kept api-17's version. Checked: the api-12 parent of `3bbe5c5` contains the
  check, while the api-17 parent and the merge result do not.
- **Later lines:** api-18 (`c20d2c2`), api-19 (`8474aad`), api-20 (`2e3e945`) and api-21 (`37b3c7b`) all lack the
  check.

## Suggested fix

Restore the check in the `ByteTag` case of `fromTagBase(Tag, String, BiFunction)`:

```java
case ByteTag bt:
    if (key != null && key.contains(NBTTranslator.BOOLEAN_IDENTIFIER)) {   // or endsWith, see draft 03
        return bt.byteValue() != 0;
    }
    return bt.byteValue();
```

**Where to fix:** in api-17, so it merges up to api-18 through api-21. If draft 03 is fixed as well, use `endsWith`
here too.

**Compatibility:** nothing is stored differently. Reads go back to returning `Boolean`, as on api-12.

## Link to logs

N/A. Nothing is logged.

**Reproducers:** `AdditionalFindingsTest#x20CompoundBooleanReadsAsByte`, and the `Boolean true`, `Boolean false`
and `nested DataView with boolean` rows of `LosslessMatrixTest` (CompoundTagDataView column), on
`gabizou/SpongeCommon` branch `analysis/dataview-lossless-api17`.
