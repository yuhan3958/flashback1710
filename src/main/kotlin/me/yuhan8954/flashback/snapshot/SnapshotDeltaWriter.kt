package me.yuhan8954.flashback.snapshot

import java.io.DataOutput

object SnapshotDeltaWriter {

    fun write(
        output: DataOutput,
        delta: ReplaySnapshotDelta,
    ) {
        SnapshotWriter.write(
            output,
            delta.snapshot,
        )

        output.writeInt(
            delta.removedChunks.size,
        )

        delta.removedChunks.forEach {
            output.writeInt(
                it.chunkX,
            )

            output.writeInt(
                it.chunkZ,
            )
        }

        output.writeInt(
            delta.removedTileEntities.size,
        )

        delta.removedTileEntities.forEach {
            output.writeInt(
                it.x,
            )

            output.writeInt(
                it.y,
            )

            output.writeInt(
                it.z,
            )
        }

        output.writeInt(
            delta.removedEntityIds.size,
        )

        delta.removedEntityIds.forEach {
            output.writeInt(
                it,
            )
        }
    }
}
