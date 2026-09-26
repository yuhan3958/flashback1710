# Replay File Format

This document describes the Flashback 1710 replay file format.

The replay format version is independent from the mod version. A format version changes only when the on-disk representation changes incompatibly.

## Compatibility Policy

The current writer emits format v8.

The reader currently accepts:

- v6,
- v7,
- v8.

Legacy formats remain readable for compatibility, but new recordings should always use the newest writer format.

## Common Header

Every replay begins with:

```text
int32 magic
int32 formatVersion
```

The magic value is:

```text
0x46425231
```

which identifies a Flashback replay.

## Format v8

Format v8 adds:

- replay metadata,
- framed records,
- per-record CRC32,
- bounded record sizes,
- an explicit clean-close record,
- graceful recovery from truncated or corrupted trailing records.

The high-level layout is:

```text
magic
formatVersion = 8

metadata
initialSnapshot

framedRecord
framedRecord
...
cleanCloseRecord
```

## Metadata

The metadata block is written immediately after the common header.

Fields are:

```text
int64 createdAtEpochMillis

string minecraftVersion
string flashbackVersion
nullableString gtnhVersion
nullableString modFingerprint
```

A string is encoded as:

```text
int32 utf8ByteLength
byte[utf8ByteLength] utf8Bytes
```

A nullable string uses:

```text
int32 utf8ByteLength
```

where `-1` means null. Non-null values then contain that many UTF-8 bytes.

Text fields are bounded by `ReplayWriter.MAX_TEXT_BYTES`.

## Initial Snapshot

The initial snapshot is written with `SnapshotWriter` immediately after the metadata block.

It is not stored as a framed record.

A replay cannot be meaningfully recovered without a valid initial snapshot, so corruption in the common header, metadata, or initial snapshot is treated as a load failure rather than as trailing-record recovery.

## Framed Records

Every v8 record after the initial snapshot has this shape:

```text
uint8 type
int32 payloadLength
int32 crc32
byte[payloadLength] payload
```

`payloadLength` must be within:

```text
0 .. ReplayWriter.MAX_RECORD_BYTES
```

The reader validates the length before allocating the payload buffer.

The CRC32 is computed over:

```text
record type byte
+
payload bytes
```

The framing intentionally separates record boundaries from the payload representation so a damaged trailing record does not require discarding all earlier valid records.

## Record Types

Current v8 record types are:

```text
0 = packet
1 = full checkpoint
2 = delta checkpoint
3 = clean close
```

### Packet Record

Payload:

```text
int64 timestampNanos
uint8 packetFlow

string packetClass
nullableString channel

int32 payloadLength
byte[payloadLength] payload
```

Packet payloads are separately bounded by `ReplayWriter.MAX_PACKET_BYTES`.

### Full Checkpoint Record

Payload:

```text
int64 timestampNanos
int32 packetIndex
snapshot
```

The snapshot uses the same snapshot encoding as the initial snapshot.

### Delta Checkpoint Record

Payload:

```text
int64 timestampNanos
int32 packetIndex
snapshotDelta
```

The delta uses `SnapshotDeltaWriter`.

### Clean Close Record

The clean-close record has:

```text
type = 3
payloadLength = 0
payload = empty
```

Its CRC is still validated.

A v8 file is considered cleanly closed only when this valid record is reached.

## Reader Recovery Status

`ReplayReader.status` reports one of:

```text
CLEAN
LEGACY
TRUNCATED
CORRUPT
```

### CLEAN

A valid v8 clean-close record was reached.

### LEGACY

The file is a supported v6 or v7 replay.

Legacy formats do not contain the v8 clean-close marker.

### TRUNCATED

The initial snapshot was valid and one or more complete framed records were read, but EOF occurred:

- before the next record type,
- inside the frame header,
- or inside the frame payload.

All records completed before the truncated record remain available.

A v8 file that simply ends without a clean-close record is also reported as truncated.

### CORRUPT

A trailing framed record is considered corrupt when, for example:

- its declared size exceeds the configured maximum,
- its CRC does not match,
- its type is unknown,
- its payload cannot be decoded,
- decoded payload bytes remain after a known record has been parsed,
- or the clean-close record has a non-empty payload.

Records decoded successfully before the corrupt frame remain available.

## Recovery Guarantees

Format v8 is designed around this rule:

> Never discard a valid prefix merely because the trailing record is incomplete or damaged.

This applies only after the initial snapshot has been decoded successfully.

The reader does not attempt to guess, resynchronize, or scan forward after a corrupt frame. The first invalid frame terminates record loading. This avoids accidentally interpreting arbitrary payload bytes as valid record boundaries.

## Defensive Limits

The reader must validate external lengths before allocating memory.

Current relevant limits are:

```text
MAX_TEXT_BYTES
MAX_PACKET_BYTES
MAX_RECORD_BYTES
```

Snapshot readers also enforce their own limits for:

- chunk counts,
- tile entity counts,
- entity counts,
- NBT sizes,
- inventory sizes,
- chunk run lengths.

Replay files should always be treated as untrusted input when they originate from another user.

## Legacy Format v7

Format v7 has:

```text
magic
formatVersion = 7
initialSnapshot

recordType
recordPayload
recordType
recordPayload
...
EOF
```

Its record types are:

```text
0 = packet
1 = full checkpoint
2 = delta checkpoint
```

There is no metadata header, record length framing, checksum, or clean-close marker.

## Legacy Format v6

Format v6 has:

```text
magic
formatVersion = 6
initialSnapshot
packet
packet
packet
...
EOF
```

It predates checkpoint records.

## Evolution Rules

Future changes should follow these rules:

1. Keep replay format versioning independent from mod versioning.
2. Increment the format version for incompatible binary changes.
3. Keep bounded backward-reading support where maintenance cost is reasonable.
4. Do not silently reinterpret an old format as a new one.
5. Document every new field and recovery rule here.
6. Validate all externally supplied lengths before allocation.
7. Prefer framed extensions over ambiguous EOF-based structures.

The format should remain sufficiently documented that another maintainer could implement a compatible reader without reverse engineering Flashback source code.
