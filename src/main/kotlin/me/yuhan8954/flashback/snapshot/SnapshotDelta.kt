package me.yuhan8954.flashback.snapshot

object SnapshotDelta {

    fun create(
        previous: ReplaySnapshot,
        current: ReplaySnapshot,
    ): ReplaySnapshotDelta {
        val dimensionChanged =
            previous.dimensionId != current.dimensionId ||
                previous.seed != current.seed

        val previousChunks = previous.chunks.associateBy(::chunkKey)
        val currentChunks = current.chunks.associateBy(::chunkKey)

        val changedChunks = if (dimensionChanged) {
            current.chunks
        } else {
            current.chunks.filter { currentChunk ->
                val previousChunk = previousChunks[chunkKey(currentChunk)]

                previousChunk == null ||
                    !sameChunk(
                        previousChunk,
                        currentChunk,
                    )
            }
        }

        val removedChunks = previous.chunks
            .filter {
                dimensionChanged ||
                    chunkKey(it) !in currentChunks
            }.map {
                ReplayChunkPosition(
                    it.chunkX,
                    it.chunkZ,
                )
            }

        val previousTileEntities = previous.tileEntities.associateBy(::tileEntityKey)
        val currentTileEntities = current.tileEntities.associateBy(::tileEntityKey)

        val changedTileEntities = if (dimensionChanged) {
            current.tileEntities
        } else {
            current.tileEntities.filter { currentTileEntity ->
                val previousTileEntity =
                    previousTileEntities[
                        tileEntityKey(
                            currentTileEntity,
                        ),
                    ]

                previousTileEntity == null ||
                    previousTileEntity.nbt != currentTileEntity.nbt
            }
        }

        val removedTileEntities = previous.tileEntities
            .filter {
                dimensionChanged ||
                    tileEntityKey(it) !in currentTileEntities
            }.map {
                ReplayBlockPosition(
                    it.x,
                    it.y,
                    it.z,
                )
            }

        val previousEntities = previous.entities.associateBy {
            it.entityId
        }

        val currentEntities = current.entities.associateBy {
            it.entityId
        }

        val changedEntities = if (dimensionChanged) {
            current.entities
        } else {
            current.entities.filter { currentEntity ->
                val previousEntity =
                    previousEntities[
                        currentEntity.entityId,
                    ]

                previousEntity == null ||
                    !sameEntity(
                        previousEntity,
                        currentEntity,
                    )
            }
        }

        val removedEntityIds = previous.entities
            .asSequence()
            .filter {
                dimensionChanged ||
                    it.entityId !in currentEntities
            }.map {
                it.entityId
            }.toList()
            .toIntArray()

        return ReplaySnapshotDelta(
            snapshot = current.copy(
                chunks = changedChunks,
                tileEntities = changedTileEntities,
                entities = changedEntities,
            ),
            removedChunks = removedChunks,
            removedTileEntities = removedTileEntities,
            removedEntityIds = removedEntityIds,
        )
    }

    fun apply(
        snapshot: ReplaySnapshot,
        delta: ReplaySnapshotDelta,
    ): ReplaySnapshot {
        val chunks =
            snapshot.chunks.associateByTo(
                linkedMapOf(),
                ::chunkKey,
            )

        delta.removedChunks.forEach {
            chunks.remove(
                chunkKey(
                    it.chunkX,
                    it.chunkZ,
                ),
            )
        }

        delta.snapshot.chunks.forEach {
            chunks[chunkKey(it)] = it
        }

        val tileEntities =
            snapshot.tileEntities.associateByTo(
                linkedMapOf(),
                ::tileEntityKey,
            )

        delta.removedTileEntities.forEach {
            tileEntities.remove(
                tileEntityKey(
                    it.x,
                    it.y,
                    it.z,
                ),
            )
        }

        delta.snapshot.tileEntities.forEach {
            tileEntities[tileEntityKey(it)] = it
        }

        val entities =
            snapshot.entities.associateByTo(
                linkedMapOf(),
            ) {
                it.entityId
            }

        delta.removedEntityIds.forEach {
            entities.remove(
                it,
            )
        }

        delta.snapshot.entities.forEach {
            entities[it.entityId] = it
        }

        return delta.snapshot.copy(
            chunks = chunks.values.toList(),
            tileEntities = tileEntities.values.toList(),
            entities = entities.values.toList(),
        )
    }

    private fun sameChunk(
        first: ReplayChunkSnapshot,
        second: ReplayChunkSnapshot,
    ): Boolean = first.blockIds.contentEquals(
        second.blockIds,
    ) &&
        first.metadata.contentEquals(
            second.metadata,
        ) &&
        first.biomes.contentEquals(
            second.biomes,
        )

    private fun sameEntity(
        first: ReplayEntitySnapshot,
        second: ReplayEntitySnapshot,
    ): Boolean = first.entityType == second.entityType &&
        first.entityClass == second.entityClass &&
        first.playerProfileId == second.playerProfileId &&
        first.playerProfileName == second.playerProfileName &&
        first.serverPosX == second.serverPosX &&
        first.serverPosY == second.serverPosY &&
        first.serverPosZ == second.serverPosZ &&
        first.x == second.x &&
        first.y == second.y &&
        first.z == second.z &&
        first.yaw == second.yaw &&
        first.pitch == second.pitch &&
        first.motionX == second.motionX &&
        first.motionY == second.motionY &&
        first.motionZ == second.motionZ &&
        first.nbt == second.nbt

    private fun chunkKey(
        snapshot: ReplayChunkSnapshot,
    ): Long = chunkKey(
        snapshot.chunkX,
        snapshot.chunkZ,
    )

    private fun chunkKey(
        chunkX: Int,
        chunkZ: Int,
    ): Long = (chunkX.toLong() shl 32) xor
        (chunkZ.toLong() and 0xffffffffL)

    private fun tileEntityKey(
        snapshot: ReplayTileEntitySnapshot,
    ): ReplayBlockPosition = tileEntityKey(
        snapshot.x,
        snapshot.y,
        snapshot.z,
    )

    private fun tileEntityKey(
        x: Int,
        y: Int,
        z: Int,
    ): ReplayBlockPosition = ReplayBlockPosition(
        x,
        y,
        z,
    )
}
