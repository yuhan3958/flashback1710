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
}
