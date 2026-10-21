# Draft issues from the DataView lossless analysis

These are the F4 implementation bugs from `../dataview-lossless.md`. They are to be fixed separately from the
DataCodec work. Each draft follows the fields of `.github/ISSUE_TEMPLATE/bug_report.yml`, and none has been filed
yet.

| Draft | Title | Report refs | Severity |
|---|---|---|---|
| [01](01-nbt-long-strings-written-empty.md) | `DataFormats.NBT` writes strings over 65535 bytes as `""` without throwing | X1 | data loss |
| [02](02-json-cannot-read-own-byte-long-arrays.md) | `DataFormats.JSON` cannot read back the `byte[]`/`long[]` it writes | X7 | data loss |
| [03](03-boolean-key-marker-matches-substring.md) | NBT boolean key marker `$Boolean` is matched anywhere in a key | X3, X12 | silent corruption |

**Searched for duplicates** on SpongePowered/Sponge on 2026-10-21 and found none. Related but distinct:
- #3126, "Data serialization inconsistencies in MemoryDataView" (2016, open): about `DataSerializer` conversion, not
  these format bugs.
- #1762, "NBTTranslator cannot handle Enums" (2018, open): the X16 "accepted but unwritable" family.

**Shared template fields:**
- **Affected Product(s):** SpongeVanilla, SpongeForge, SpongeNeo. The code is in the common module; only
  SpongeVanilla was tested.
- **Version:** built from source at `SpongePowered/Sponge` `api-12` `69a889c` (Minecraft 1.21.1, SpongeAPI
  `12.1.0-SNAPSHOT` at `fb073c8`); no released build tested.
- **Operating System:** Linux (Ubuntu 24.04 container).
- **Java Version:** OpenJDK 21.0.12.1.
- **Plugins/Mods:** none. Reproduced with JUnit tests in a booted SpongeVanilla server (`:SpongeVanilla:test`).
