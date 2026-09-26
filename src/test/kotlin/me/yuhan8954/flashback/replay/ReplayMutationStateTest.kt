package me.yuhan8954.flashback.replay

import net.minecraft.nbt.NBTTagCompound
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayMutationStateTest {

    @Test
    fun `journal player comparison ignores transform-only changes`() {
        val first =
            playerState(
                x = 1.0,
                yaw = 10.0f,
            )

        val second =
            playerState(
                x = 40.0,
                yaw = 160.0f,
            )

        assertTrue(
            first.sameJournalState(
                second,
            ),
        )
    }

    @Test
    fun `journal player comparison detects selected slot changes`() {
        val first =
            playerState(
                currentItem = 0,
            )

        val second =
            playerState(
                currentItem = 5,
            )

        assertFalse(
            first.sameJournalState(
                second,
            ),
        )
    }

    @Test
    fun `journal player comparison detects stance changes`() {
        val first =
            playerState()

        val second =
            playerState(
                sneaking = true,
                sprinting = true,
            )

        assertFalse(
            first.sameJournalState(
                second,
            ),
        )
    }

    @Test
    fun `world mutation state equality covers replay metadata`() {
        val first =
            ReplayWorldMutationState(
                worldTime = 100L,
                totalWorldTime = 200L,
                raining = false,
                thundering = false,
                rainStrength = 0.0f,
                thunderStrength = 0.0f,
            )

        assertTrue(
            first ==
                first.copy(),
        )

        assertFalse(
            first ==
                first.copy(
                    raining = true,
                ),
        )

        assertFalse(
            first ==
                first.copy(
                    worldTime = 101L,
                ),
        )
    }

    @Test
    fun `player journal comparison ignores transform nbt but detects persistent nbt`() {
        val first =
            playerState().copy(
                nbt =
                NBTTagCompound().apply {
                    setTag(
                        "Pos",
                        net.minecraft.nbt.NBTTagList(),
                    )
                    setInteger(
                        "HealthMarker",
                        10,
                    )
                },
            )

        val transformOnly =
            playerState().copy(
                nbt =
                NBTTagCompound().apply {
                    setTag(
                        "Pos",
                        net.minecraft.nbt.NBTTagList().apply {
                            appendTag(
                                net.minecraft.nbt.NBTTagDouble(
                                    50.0,
                                ),
                            )
                        },
                    )
                    setInteger(
                        "HealthMarker",
                        10,
                    )
                },
            )

        val persistentChange =
            transformOnly.copy(
                nbt =
                NBTTagCompound().apply {
                    setTag(
                        "Pos",
                        net.minecraft.nbt.NBTTagList(),
                    )
                    setInteger(
                        "HealthMarker",
                        11,
                    )
                },
            )

        assertTrue(
            first.sameJournalState(
                transformOnly,
            ),
        )
        assertFalse(
            first.sameJournalState(
                persistentChange,
            ),
        )
    }

    @Test
    fun `entity journal comparison leaves movement to reverse frames`() {
        val first =
            entityState(
                x = 1.0,
                marker = 10,
            )

        val moved =
            entityState(
                x = 20.0,
                marker = 10,
            )

        val changed =
            entityState(
                x = 20.0,
                marker = 11,
            )

        assertTrue(
            first.sameJournalState(
                moved,
            ),
        )
        assertFalse(
            first.sameJournalState(
                changed,
            ),
        )
    }

    private fun playerState(
        x: Double = 0.0,
        yaw: Float = 0.0f,
        currentItem: Int = 0,
        sneaking: Boolean = false,
        sprinting: Boolean = false,
    ): ReplayReversePlayerState = ReplayReversePlayerState(
        x = x,
        y = 64.0,
        z = 0.0,
        yaw = yaw,
        pitch = 0.0f,
        motionX = 0.0,
        motionY = 0.0,
        motionZ = 0.0,
        onGround = true,
        currentItem = currentItem,
        sneaking = sneaking,
        sprinting = sprinting,
        mainInventory = emptyList(),
        armorInventory = emptyList(),
        nbt = NBTTagCompound(),
    )

    private fun entityState(
        x: Double,
        marker: Int,
    ): ReplayReverseEntityState = ReplayReverseEntityState(
        entityId = 2,
        entityType = "Pig",
        entityClass = "example.Entity",
        playerProfileId = null,
        playerProfileName = null,
        x = x,
        y = 64.0,
        z = 0.0,
        yaw = 0.0f,
        pitch = 0.0f,
        motionX = 0.0,
        motionY = 0.0,
        motionZ = 0.0,
        serverPosX = 0,
        serverPosY = 0,
        serverPosZ = 0,
        onGround = true,
        sneaking = false,
        sprinting = false,
        rotationYawHead = null,
        nbt =
        NBTTagCompound().apply {
            setTag(
                "Pos",
                net.minecraft.nbt.NBTTagList().apply {
                    appendTag(
                        net.minecraft.nbt.NBTTagDouble(
                            x,
                        ),
                    )
                },
            )
            setInteger(
                "PersistentMarker",
                marker,
            )
        },
    )
}
