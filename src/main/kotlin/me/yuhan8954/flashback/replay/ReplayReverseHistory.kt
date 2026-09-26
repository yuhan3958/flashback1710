package me.yuhan8954.flashback.replay

import com.mojang.authlib.GameProfile
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.UUID

class ReplayReverseHistory {

    private val frames =
        ArrayDeque<ReplayReverseFrame>()

    val earliestTimeNanos: Long?
        get() =
            frames.peekFirst()
                ?.timestampNanos

    val latestTimeNanos: Long?
        get() =
            frames.peekLast()
                ?.timestampNanos

    val coverage: ReplayReverseCoverage?
        get() {
            val first =
                frames.peekFirst()
                    ?: return null

            val last =
                frames.peekLast()
                    ?: return null

            return ReplayReverseCoverage(
                startTimeNanos =
                first.timestampNanos,
                endTimeNanos =
                last.timestampNanos,
            )
        }

    fun clear() {
        frames.clear()
    }

    fun capture(
        session: ReplaySession,
        timestampNanos: Long,
        packetIndex: Int,
    ) {
        while (
            frames.isNotEmpty() &&
            frames.peekLast()
                .timestampNanos >=
            timestampNanos
        ) {
            frames.removeLast()
        }

        frames.addLast(
            ReplayReverseFrame.capture(
                session,
                timestampNanos,
                packetIndex,
            ),
        )

        trim(
            timestampNanos,
        )
    }

    fun restoreAtOrBefore(
        session: ReplaySession,
        targetTimeNanos: Long,
    ): ReplayReverseFrame? {
        val first =
            frames.peekFirst()
                ?: return null

        if (
            targetTimeNanos <
            first.timestampNanos
        ) {
            return null
        }

        var before =
            first

        var after:
            ReplayReverseFrame? =
            null

        frames.forEach { frame ->
            if (
                frame.timestampNanos <=
                targetTimeNanos
            ) {
                before =
                    frame
            } else if (
                after == null
            ) {
                after =
                    frame
            }
        }

        val newer =
            after

        val interpolation =
            if (
                newer == null ||
                newer.timestampNanos ==
                before.timestampNanos
            ) {
                0.0
            } else {
                (
                    targetTimeNanos -
                        before.timestampNanos
                    ).toDouble() /
                    (
                        newer.timestampNanos -
                            before.timestampNanos
                        ).toDouble()
            }.coerceIn(
                0.0,
                1.0,
            )

        before.restore(
            session,
            newer,
            interpolation,
        )

        return before
    }

    private fun trim(
        currentTimeNanos: Long,
    ) {
        val minimumTime =
            (
                currentTimeNanos -
                    HISTORY_DURATION_NANOS
                ).coerceAtLeast(
                0L,
            )

        while (
            frames.size > 1 &&
            frames.peekFirst()
                .timestampNanos <
            minimumTime
        ) {
            frames.removeFirst()
        }
    }

    companion object {

        const val HISTORY_DURATION_NANOS =
            30_000_000_000L
    }
}

data class ReplayReverseFrame(
    val timestampNanos: Long,
    val packetIndex: Int,
    private val playerState: ReplayReversePlayerState,
    private val entityStates: Map<Int, ReplayReverseEntityState>,
    private val worldTime: Long,
    private val totalWorldTime: Long,
    private val raining: Boolean,
    private val thundering: Boolean,
    private val rainStrength: Float,
    private val thunderStrength: Float,
) {

    fun restore(
        session: ReplaySession,
        newerFrame: ReplayReverseFrame?,
        interpolation: Double,
    ) {
        val world =
            session.world

        val recordedPlayer =
            session.recordedPlayer

        playerState.restore(
            recordedPlayer,
            newerFrame
                ?.playerState,
            interpolation,
        )

        val currentEntities =
            world.loadedEntityList
                .filterIsInstance<Entity>()
                .filter {
                    it !== recordedPlayer &&
                        it !== session.player
                }.associateBy {
                    it.entityId
                }

        currentEntities.forEach {
                (
                    entityId,
                    _,
                ),
            ->
            if (
                entityId !in
                entityStates
            ) {
                world.removeEntityFromWorld(
                    entityId,
                )
            }
        }

        entityStates.forEach {
                (
                    entityId,
                    state,
                ),
            ->
            val newerState =
                newerFrame
                    ?.entityStates
                    ?.get(
                        entityId,
                    )

            val current =
                world.getEntityByID(
                    entityId,
                )

            val target =
                if (
                    current != null &&
                    current.javaClass.name ==
                    state.entityClass
                ) {
                    current
                } else {
                    if (current != null) {
                        world.removeEntityFromWorld(
                            entityId,
                        )
                    }

                    state.create(
                        world,
                    )?.also {
                        world.addEntityToWorld(
                            entityId,
                            it,
                        )
                    }
                }

            if (target != null) {
                state.restore(
                    target,
                    newerState,
                    interpolation,
                )
            }
        }

        world.setWorldTime(
            interpolateLong(
                worldTime,
                newerFrame
                    ?.worldTime,
                interpolation,
            ),
        )

        world.func_82738_a(
            interpolateLong(
                totalWorldTime,
                newerFrame
                    ?.totalWorldTime,
                interpolation,
            ),
        )

        world.worldInfo.setRaining(
            raining,
        )

        world.worldInfo.setThundering(
            thundering,
        )

        world.setRainStrength(
            interpolateFloat(
                rainStrength,
                newerFrame
                    ?.rainStrength,
                interpolation,
            ),
        )

        world.setThunderStrength(
            interpolateFloat(
                thunderStrength,
                newerFrame
                    ?.thunderStrength,
                interpolation,
            ),
        )
    }

    companion object {

        fun capture(
            session: ReplaySession,
            timestampNanos: Long,
            packetIndex: Int,
        ): ReplayReverseFrame {
            val world =
                session.world

            val recordedPlayer =
                session.recordedPlayer

            val entities =
                world.loadedEntityList
                    .filterIsInstance<Entity>()
                    .asSequence()
                    .filter {
                        it !== recordedPlayer &&
                            it !== session.player
                    }.mapNotNull { entity ->
                        ReplayReverseEntityState
                            .capture(
                                entity,
                            )?.let {
                                entity.entityId to
                                    it
                            }
                    }.toMap()

            return ReplayReverseFrame(
                timestampNanos =
                timestampNanos,
                packetIndex =
                packetIndex,
                playerState =
                ReplayReversePlayerState.capture(
                    recordedPlayer,
                ),
                entityStates =
                entities,
                worldTime =
                world.worldTime,
                totalWorldTime =
                world.totalWorldTime,
                raining =
                world.worldInfo.isRaining,
                thundering =
                world.worldInfo.isThundering,
                rainStrength =
                world.rainingStrength,
                thunderStrength =
                world.thunderingStrength,
            )
        }
    }
}

data class ReplayReversePlayerState(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
    val onGround: Boolean,
    val currentItem: Int,
    val sneaking: Boolean,
    val sprinting: Boolean,
    val mainInventory: List<ItemStack?>,
    val armorInventory: List<ItemStack?>,
    val nbt: NBTTagCompound,
) {

    fun sameJournalState(
        other: ReplayReversePlayerState,
    ): Boolean = onGround ==
        other.onGround &&
        currentItem ==
        other.currentItem &&
        sneaking ==
        other.sneaking &&
        sprinting ==
        other.sprinting &&
        sameInventory(
            mainInventory,
            other.mainInventory,
        ) &&
        sameInventory(
            armorInventory,
            other.armorInventory,
        )

    fun restore(
        player: EntityReplayPlayer,
        newerState: ReplayReversePlayerState?,
        interpolation: Double,
    ) {
        val previousX =
            player.posX

        val previousY =
            player.posY

        val previousZ =
            player.posZ

        val previousYaw =
            player.rotationYaw

        val previousPitch =
            player.rotationPitch

        player.readFromNBT(
            nbt.copy() as
                NBTTagCompound,
        )

        val targetX =
            interpolateDouble(
                x,
                newerState?.x,
                interpolation,
            )

        val targetY =
            interpolateDouble(
                y,
                newerState?.y,
                interpolation,
            )

        val targetZ =
            interpolateDouble(
                z,
                newerState?.z,
                interpolation,
            )

        val targetYaw =
            interpolateAngle(
                yaw,
                newerState?.yaw,
                interpolation,
            )

        val targetPitch =
            interpolateFloat(
                pitch,
                newerState?.pitch,
                interpolation,
            )

        player.setPositionAndRotation(
            targetX,
            targetY,
            targetZ,
            targetYaw,
            targetPitch,
        )

        player.prevPosX =
            previousX

        player.prevPosY =
            previousY

        player.prevPosZ =
            previousZ

        player.lastTickPosX =
            previousX

        player.lastTickPosY =
            previousY

        player.lastTickPosZ =
            previousZ

        player.prevRotationYaw =
            previousYaw

        player.prevRotationPitch =
            previousPitch

        player.motionX =
            interpolateDouble(
                motionX,
                newerState?.motionX,
                interpolation,
            )

        player.motionY =
            interpolateDouble(
                motionY,
                newerState?.motionY,
                interpolation,
            )

        player.motionZ =
            interpolateDouble(
                motionZ,
                newerState?.motionZ,
                interpolation,
            )

        player.onGround =
            onGround

        player.setSneaking(
            sneaking,
        )

        player.setSprinting(
            sprinting,
        )

        player.inventory.currentItem =
            currentItem

        player.inventory.mainInventory
            .indices
            .forEach {
                player.inventory
                    .mainInventory[it] =
                    mainInventory
                        .getOrNull(
                            it,
                        )
                        ?.copy()
            }

        player.inventory.armorInventory
            .indices
            .forEach {
                player.inventory
                    .armorInventory[it] =
                    armorInventory
                        .getOrNull(
                            it,
                        )
                        ?.copy()
            }

        player.inventory.markDirty()
    }

    companion object {

        fun capture(
            player: EntityReplayPlayer,
        ): ReplayReversePlayerState {
            val nbt =
                NBTTagCompound()

            player.writeToNBT(
                nbt,
            )

            return ReplayReversePlayerState(
                x =
                player.posX,
                y =
                player.posY,
                z =
                player.posZ,
                yaw =
                player.rotationYaw,
                pitch =
                player.rotationPitch,
                motionX =
                player.motionX,
                motionY =
                player.motionY,
                motionZ =
                player.motionZ,
                onGround =
                player.onGround,
                currentItem =
                player.inventory
                    .currentItem,
                sneaking =
                player.isSneaking,
                sprinting =
                player.isSprinting,
                mainInventory =
                player.inventory
                    .mainInventory
                    .map {
                        it?.copy()
                    },
                armorInventory =
                player.inventory
                    .armorInventory
                    .map {
                        it?.copy()
                    },
                nbt =
                nbt,
            )
        }
    }
}

data class ReplayReverseEntityState(
    val entityId: Int,
    val entityType: String?,
    val entityClass: String,
    val playerProfileId: String?,
    val playerProfileName: String?,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
    val serverPosX: Int,
    val serverPosY: Int,
    val serverPosZ: Int,
    val onGround: Boolean,
    val sneaking: Boolean,
    val sprinting: Boolean,
    val rotationYawHead: Float?,
    val nbt: NBTTagCompound,
) {

    fun create(
        world: ReplayWorld,
    ): Entity? {
        val restoredNbt =
            nbt.copy() as
                NBTTagCompound

        entityType?.let {
            restoredNbt.setString(
                "id",
                it,
            )
        }

        val entity =
            createOtherPlayer(
                world,
                restoredNbt,
            ) ?: if (
                entityType != null
            ) {
                EntityList.createEntityFromNBT(
                    restoredNbt,
                    world,
                )
            } else {
                createByClass(
                    world,
                    restoredNbt,
                )
            }

        entity?.setEntityId(
            entityId,
        )

        return entity
    }

    fun restore(
        target: Entity,
        newerState: ReplayReverseEntityState?,
        interpolation: Double,
    ) {
        val previousX =
            target.posX

        val previousY =
            target.posY

        val previousZ =
            target.posZ

        val previousYaw =
            target.rotationYaw

        val previousPitch =
            target.rotationPitch

        target.readFromNBT(
            nbt.copy() as
                NBTTagCompound,
        )

        target.setEntityId(
            entityId,
        )

        target.isDead =
            false

        val targetX =
            interpolateDouble(
                x,
                newerState?.x,
                interpolation,
            )

        val targetY =
            interpolateDouble(
                y,
                newerState?.y,
                interpolation,
            )

        val targetZ =
            interpolateDouble(
                z,
                newerState?.z,
                interpolation,
            )

        val targetYaw =
            interpolateAngle(
                yaw,
                newerState?.yaw,
                interpolation,
            )

        val targetPitch =
            interpolateFloat(
                pitch,
                newerState?.pitch,
                interpolation,
            )

        target.setPositionAndRotation(
            targetX,
            targetY,
            targetZ,
            targetYaw,
            targetPitch,
        )

        target.prevPosX =
            previousX

        target.prevPosY =
            previousY

        target.prevPosZ =
            previousZ

        target.lastTickPosX =
            previousX

        target.lastTickPosY =
            previousY

        target.lastTickPosZ =
            previousZ

        target.prevRotationYaw =
            previousYaw

        target.prevRotationPitch =
            previousPitch

        target.motionX =
            interpolateDouble(
                motionX,
                newerState?.motionX,
                interpolation,
            )

        target.motionY =
            interpolateDouble(
                motionY,
                newerState?.motionY,
                interpolation,
            )

        target.motionZ =
            interpolateDouble(
                motionZ,
                newerState?.motionZ,
                interpolation,
            )

        target.serverPosX =
            interpolateInt(
                serverPosX,
                newerState?.serverPosX,
                interpolation,
            )

        target.serverPosY =
            interpolateInt(
                serverPosY,
                newerState?.serverPosY,
                interpolation,
            )

        target.serverPosZ =
            interpolateInt(
                serverPosZ,
                newerState?.serverPosZ,
                interpolation,
            )

        target.onGround =
            onGround

        target.setSneaking(
            sneaking,
        )

        target.setSprinting(
            sprinting,
        )

        if (
            target is EntityLivingBase &&
            rotationYawHead != null
        ) {
            val targetHeadYaw =
                interpolateAngle(
                    rotationYawHead,
                    newerState
                        ?.rotationYawHead,
                    interpolation,
                )

            target.prevRotationYawHead =
                target.rotationYawHead

            target.rotationYawHead =
                targetHeadYaw
        }
    }

    private fun createOtherPlayer(
        world: ReplayWorld,
        restoredNbt: NBTTagCompound,
    ): EntityOtherPlayerMP? {
        val profileName =
            playerProfileName
                ?: return null

        val profileId =
            playerProfileId
                ?.let(
                    UUID::fromString,
                )

        return EntityOtherPlayerMP(
            world,
            GameProfile(
                profileId,
                profileName,
            ),
        ).also {
            it.readFromNBT(
                restoredNbt,
            )
        }
    }

    private fun createByClass(
        world: ReplayWorld,
        restoredNbt: NBTTagCompound,
    ): Entity? {
        val entityClass =
            Class.forName(
                entityClass,
            ).asSubclass(
                Entity::class.java,
            )

        val constructor =
            entityClass.getDeclaredConstructor(
                World::class.java,
            )

        constructor.isAccessible =
            true

        return constructor.newInstance(
            world,
        ).also {
            it.readFromNBT(
                restoredNbt,
            )
        }
    }

    companion object {

        fun capture(
            entity: Entity,
        ): ReplayReverseEntityState? = try {
            val nbt =
                NBTTagCompound()

            entity.writeToNBT(
                nbt,
            )

            val entityType =
                EntityList.getEntityString(
                    entity,
                )

            if (entityType != null) {
                nbt.setString(
                    "id",
                    entityType,
                )
            }

            ReplayReverseEntityState(
                entityId =
                entity.entityId,
                entityType =
                entityType,
                entityClass =
                entity.javaClass.name,
                playerProfileId =
                if (
                    entity is
                        EntityOtherPlayerMP
                ) {
                    entity.gameProfile.id
                        ?.toString()
                } else {
                    null
                },
                playerProfileName =
                if (
                    entity is
                        EntityOtherPlayerMP
                ) {
                    entity.gameProfile.name
                } else {
                    null
                },
                x =
                entity.posX,
                y =
                entity.posY,
                z =
                entity.posZ,
                yaw =
                entity.rotationYaw,
                pitch =
                entity.rotationPitch,
                motionX =
                entity.motionX,
                motionY =
                entity.motionY,
                motionZ =
                entity.motionZ,
                serverPosX =
                entity.serverPosX,
                serverPosY =
                entity.serverPosY,
                serverPosZ =
                entity.serverPosZ,
                onGround =
                entity.onGround,
                sneaking =
                entity.isSneaking,
                sprinting =
                entity.isSprinting,
                rotationYawHead =
                (
                    entity as?
                        EntityLivingBase
                    )?.rotationYawHead,
                nbt =
                nbt,
            )
        } catch (
            throwable: Throwable,
        ) {
            System.err.println(
                "[Flashback] Failed to capture reverse entity: " +
                    entity.javaClass.name,
            )

            throwable.printStackTrace()
            null
        }
    }
}

private fun sameInventory(
    first: List<ItemStack?>,
    second: List<ItemStack?>,
): Boolean {
    if (
        first.size !=
        second.size
    ) {
        return false
    }

    first.indices.forEach { index ->
        if (
            !ItemStack.areItemStacksEqual(
                first[index],
                second[index],
            )
        ) {
            return false
        }
    }

    return true
}

private fun interpolateDouble(
    older: Double,
    newer: Double?,
    interpolation: Double,
): Double {
    if (newer == null) {
        return older
    }

    return older +
        (
            newer -
                older
            ) *
        interpolation
}

private fun interpolateFloat(
    older: Float,
    newer: Float?,
    interpolation: Double,
): Float {
    if (newer == null) {
        return older
    }

    return (
        older +
            (
                newer -
                    older
                ) *
            interpolation
        ).toFloat()
}

private fun interpolateLong(
    older: Long,
    newer: Long?,
    interpolation: Double,
): Long {
    if (newer == null) {
        return older
    }

    return (
        older +
            (
                newer -
                    older
                ) *
            interpolation
        ).toLong()
}

private fun interpolateInt(
    older: Int,
    newer: Int?,
    interpolation: Double,
): Int {
    if (newer == null) {
        return older
    }

    return (
        older +
            (
                newer -
                    older
                ) *
            interpolation
        ).toInt()
}

private fun interpolateAngle(
    older: Float,
    newer: Float?,
    interpolation: Double,
): Float {
    if (newer == null) {
        return older
    }

    var difference =
        newer -
            older

    while (
        difference <
        -180.0f
    ) {
        difference +=
            360.0f
    }

    while (
        difference >=
        180.0f
    ) {
        difference -=
            360.0f
    }

    return (
        older +
            difference *
            interpolation
        ).toFloat()
}
