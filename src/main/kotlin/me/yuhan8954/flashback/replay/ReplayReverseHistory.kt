package me.yuhan8954.flashback.replay

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import java.util.ArrayDeque

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

    fun clear() {
        frames.clear()
    }

    fun capture(
        session: ReplaySession,
        timestampNanos: Long,
        packetIndex: Int,
    ) {
        val latest =
            frames.peekLast()

        if (
            latest != null &&
            timestampNanos <
            latest.timestampNanos
        ) {
            return
        }

        if (
            latest != null &&
            timestampNanos ==
            latest.timestampNanos
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
        while (
            frames.size > 1 &&
            frames.peekLast()
                .timestampNanos >
            targetTimeNanos
        ) {
            frames.removeLast()
        }

        val frame =
            frames.peekLast()
                ?: return null

        if (
            frame.timestampNanos >
            targetTimeNanos
        ) {
            return null
        }

        frame.restore(
            session,
        )

        return frame
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

    fun restore(session: ReplaySession) {
        val world =
            session.world

        val recordedPlayer =
            session.recordedPlayer

        playerState.restore(
            recordedPlayer,
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
                    entity,
                ),
            ->
            val targetState =
                entityStates[
                    entityId,
                ]

            if (targetState == null) {
                world.removeEntityFromWorld(
                    entityId,
                )
            } else if (
                targetState.entity !==
                entity
            ) {
                world.removeEntityFromWorld(
                    entityId,
                )

                targetState.restoreIntoWorld(
                    world,
                )
            }
        }

        entityStates.forEach {
                (
                    entityId,
                    state,
                ),
            ->
            val current =
                world.getEntityByID(
                    entityId,
                )

            if (current == null) {
                state.restoreIntoWorld(
                    world,
                )
            } else {
                state.restore(
                    current,
                )
            }
        }

        world.setWorldTime(
            worldTime,
        )

        world.func_82738_a(
            totalWorldTime,
        )

        world.worldInfo.setRaining(
            raining,
        )

        world.worldInfo.setThundering(
            thundering,
        )

        world.setRainStrength(
            rainStrength,
        )

        world.setThunderStrength(
            thunderStrength,
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
                    }.associate {
                        it.entityId to
                            ReplayReverseEntityState.capture(
                                it,
                            )
                    }

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
    val prevX: Double,
    val prevY: Double,
    val prevZ: Double,
    val lastTickX: Double,
    val lastTickY: Double,
    val lastTickZ: Double,
    val yaw: Float,
    val pitch: Float,
    val prevYaw: Float,
    val prevPitch: Float,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double,
    val onGround: Boolean,
    val currentItem: Int,
    val sneaking: Boolean,
    val sprinting: Boolean,
    val mainInventory: List<ItemStack?>,
    val armorInventory: List<ItemStack?>,
) {

    fun restore(
        player: EntityReplayPlayer,
    ) {
        player.setPositionAndRotation(
            x,
            y,
            z,
            yaw,
            pitch,
        )

        player.prevPosX =
            prevX

        player.prevPosY =
            prevY

        player.prevPosZ =
            prevZ

        player.lastTickPosX =
            lastTickX

        player.lastTickPosY =
            lastTickY

        player.lastTickPosZ =
            lastTickZ

        player.prevRotationYaw =
            prevYaw

        player.prevRotationPitch =
            prevPitch

        player.motionX =
            motionX

        player.motionY =
            motionY

        player.motionZ =
            motionZ

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
        ): ReplayReversePlayerState = ReplayReversePlayerState(
            x =
            player.posX,
            y =
            player.posY,
            z =
            player.posZ,
            prevX =
            player.prevPosX,
            prevY =
            player.prevPosY,
            prevZ =
            player.prevPosZ,
            lastTickX =
            player.lastTickPosX,
            lastTickY =
            player.lastTickPosY,
            lastTickZ =
            player.lastTickPosZ,
            yaw =
            player.rotationYaw,
            pitch =
            player.rotationPitch,
            prevYaw =
            player.prevRotationYaw,
            prevPitch =
            player.prevRotationPitch,
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
        )
    }
}

data class ReplayReverseEntityState(
    val entity: Entity,
    val x: Double,
    val y: Double,
    val z: Double,
    val prevX: Double,
    val prevY: Double,
    val prevZ: Double,
    val lastTickX: Double,
    val lastTickY: Double,
    val lastTickZ: Double,
    val yaw: Float,
    val pitch: Float,
    val prevYaw: Float,
    val prevPitch: Float,
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
    val prevRotationYawHead: Float?,
) {

    fun restoreIntoWorld(
        world: ReplayWorld,
    ) {
        entity.isDead =
            false

        restore(
            entity,
        )

        world.addEntityToWorld(
            entity.entityId,
            entity,
        )
    }

    fun restore(
        target: Entity,
    ) {
        target.setPositionAndRotation(
            x,
            y,
            z,
            yaw,
            pitch,
        )

        target.prevPosX =
            prevX

        target.prevPosY =
            prevY

        target.prevPosZ =
            prevZ

        target.lastTickPosX =
            lastTickX

        target.lastTickPosY =
            lastTickY

        target.lastTickPosZ =
            lastTickZ

        target.prevRotationYaw =
            prevYaw

        target.prevRotationPitch =
            prevPitch

        target.motionX =
            motionX

        target.motionY =
            motionY

        target.motionZ =
            motionZ

        target.serverPosX =
            serverPosX

        target.serverPosY =
            serverPosY

        target.serverPosZ =
            serverPosZ

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
            rotationYawHead != null &&
            prevRotationYawHead != null
        ) {
            target.rotationYawHead =
                rotationYawHead

            target.prevRotationYawHead =
                prevRotationYawHead
        }
    }

    companion object {

        fun capture(
            entity: Entity,
        ): ReplayReverseEntityState = ReplayReverseEntityState(
            entity =
            entity,
            x =
            entity.posX,
            y =
            entity.posY,
            z =
            entity.posZ,
            prevX =
            entity.prevPosX,
            prevY =
            entity.prevPosY,
            prevZ =
            entity.prevPosZ,
            lastTickX =
            entity.lastTickPosX,
            lastTickY =
            entity.lastTickPosY,
            lastTickZ =
            entity.lastTickPosZ,
            yaw =
            entity.rotationYaw,
            pitch =
            entity.rotationPitch,
            prevYaw =
            entity.prevRotationYaw,
            prevPitch =
            entity.prevRotationPitch,
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
            prevRotationYawHead =
            (
                entity as?
                    EntityLivingBase
                )?.prevRotationYawHead,
        )
    }
}
