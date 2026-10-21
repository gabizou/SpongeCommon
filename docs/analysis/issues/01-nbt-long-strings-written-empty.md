# DataFormats.NBT writes strings over 65535 bytes as "" without throwing

**Labels (suggested):** `type: bug`, `system: data`

**Affected Product(s):** SpongeVanilla, SpongeForge, SpongeNeo (common code; tested on SpongeVanilla)
**Version:** built from `api-12` `69a889c` (Minecraft 1.21.1)
**Operating System:** Linux
**Java Version:** OpenJDK 21.0.12.1
**Plugins/Mods:** none (JUnit in a booted SpongeVanilla server)

## Describe the bug

Writing a `DataView` with `DataFormats.NBT` succeeds even when a string is longer than 65535 bytes in modified
UTF-8. Reading the result back gives `""` for that string. The caller gets no exception, so the data is lost; the
only sign is an `ERROR` log line from Minecraft's `Util`.

The limit is in **bytes**, not characters. 21846 copies of a three-byte character such as `✓` (65538 bytes) are
already over it.

```java
final DataContainer container = DataContainer.createNew()
    .set(DataQuery.of("ascii"), "x".repeat(65_536))
    .set(DataQuery.of("threeByte"), "✓".repeat(21_846))
    .set(DataQuery.of("fits"), "x".repeat(65_535));

final ByteArrayOutputStream out = new ByteArrayOutputStream();
DataFormats.NBT.get().writeTo(out, container);            // no exception
final DataContainer read = DataFormats.NBT.get().readFrom(new ByteArrayInputStream(out.toByteArray()));

read.getString(DataQuery.of("ascii")).get();      // ""
read.getString(DataQuery.of("threeByte")).get();  // ""
read.getString(DataQuery.of("fits")).get();       // 65535 chars, intact
```

**Expected:** `writeTo` throws an `IOException`, as `DataFormat.writeTo` declares, so the caller knows the data
can't be saved.

## Cause

`NBTDataFormat.writeTo` (`src/main/java/org/spongepowered/common/data/persistence/NBTDataFormat.java:61-75`) calls
`NbtIo.write(CompoundTag, DataOutput)`. In 1.21.1 that delegates to `NbtIo.writeUnnamedTagWithFallback`, which
wraps the output in `NbtIo$StringFallbackDataOutput`. That wrapper's `writeUTF` catches `UTFDataFormatException`,
calls `Util.logAndPauseIfInIde("Failed to write NBT String", e)`, and writes `""` instead (checked with `javap` on
`joined-1.21.1.jar`).

## Suggested fix

In `NBTDataFormat.writeTo`, call `NbtIo.writeUnnamedTag(tag, dos)` instead of `NbtIo.write(tag, dos)`.
- **Format:** this writes the same bytes; `writeUnnamedTagWithFallback` is just that wrapper plus
  `writeUnnamedTag`.
- **Errors:** it lets the `UTFDataFormatException`, an `IOException` that `writeTo` already declares, reach the
  caller.
- **Evidence:** `AdditionalFindingsTest#x1NoFallbackWriteThrows` shows `writeUnnamedTag` throws for the same tag.

**Scope:** this changes only `DataFormats.NBT`. Vanilla world and player saving uses its own `NbtIo` calls, keeps
Mojang's fallback, and is out of scope here.

**Compatibility:** callers that currently "succeed" with oversize strings will start getting an `IOException`.
That is the intent, but it is a visible behavior change.

## Link to logs

The test server log contains, once per oversize string:
```
[Test worker/ERROR] [n.min.Util]: Failed to write NBT String
java.io.UTFDataFormatException: encoded string (xxxxxxxx...xxxxxxxx) too long: 65536 bytes
	at java.base/java.io.DataOutputStream.writeUTF(DataOutputStream.java:363) ~[?:?]
```

**Reproducers:** `AdditionalFindingsTest#x1NbtLongStringsBecomeEmpty` and `#x1NoFallbackWriteThrows`, and the
`String 70k chars` row of `LosslessMatrixTest`, on `gabizou/SpongeCommon` branch `analysis/dataview-lossless`.
