# DataFormats.JSON cannot read back the byte[] / long[] it writes

**Labels (suggested):** `type: bug`, `system: data`

**Affected Product(s):** SpongeVanilla, SpongeForge, SpongeNeo (common code; tested on SpongeVanilla)
**Version:** built from `api-17` `5ecbb24` (Minecraft 1.21.10, SpongeAPI `17.1.0-SNAPSHOT` `f7088f6`); same code on api-18 through api-21
**Operating System:** Linux
**Java Version:** OpenJDK 21.0.12.1
**Plugins/Mods:** none (JUnit in a booted SpongeVanilla server)

## Describe the bug

`DataFormats.JSON.write` writes `byte[]` values, and `long[]` values whose elements fit in an `int`, without
error. `DataFormats.JSON.read` then fails on its own output with `ClassCastException`, so the **whole document**
can't be read. `int[]` and `long[]` with values outside the int range round-trip fine.

```java
final String bytes = DataFormats.JSON.get().write(DataContainer.createNew().set(DataQuery.of("v"), new byte[] {1}));
// {"v":{"_arraytype":"byte","value":[1]}}
DataFormats.JSON.get().read(bytes);
// ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Byte

final String longs = DataFormats.JSON.get().write(DataContainer.createNew().set(DataQuery.of("v"), new long[] {1L, 2L}));
DataFormats.JSON.get().read(longs);
// ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Long
```

**Expected:** `read(write(view))` returns the same `byte[]` / `long[]`.

## Cause

`JsonDataFormat.readNumber` (`src/main/java/org/spongepowered/common/data/persistence/JsonDataFormat.java:176-190`)
returns an `Integer` for every integral value that fits in an int. `JsonDataFormat.readArray(String, DataView)`
(`:149-174`) then rebuilds the array with reference casts:
- `(byte) list.get(i)` at `:165` casts `Object` to `Byte`.
- `(long) n` at `:169` casts `Object` to `Long`.

Both throw for an `Integer`. The `int` case (`:161`) works only because the elements happen to be `Integer`s.

## Suggested fix

In `readArray`, convert through `Number` and reject values outside the target range rather than truncating them:

```java
case INT:  return list.stream().mapToInt(n -> Math.toIntExact(((Number) n).longValue())).toArray();
case BYTE: /* for each: long l = ((Number) list.get(i)).longValue(); range-check to Byte.MIN_VALUE..MAX_VALUE */
case LONG: return list.stream().mapToLong(n -> ((Number) n).longValue()).toArray();
```

A value that is out of range, or not a number, should produce `InvalidDataException` rather than a
`ClassCastException` or `ArithmeticException`.

**Compatibility:** it is read-side only and the on-disk format is unchanged. Files that currently fail to load
would start loading.

## Link to logs

N/A. The exception is thrown to the caller.

**Reproducers:** `AdditionalFindingsTest#x7JsonWritesUnreadableArrays`, and the `byte[]` and `long[] small values`
rows of `LosslessMatrixTest`, on `gabizou/SpongeCommon` branch `analysis/dataview-lossless-api17`.
