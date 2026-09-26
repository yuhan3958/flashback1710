package me.yuhan8954.flashback.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayReverseScenarioTest {

    @Test
    fun `block change rewinds to previous block state`() {
        val scenario =
            Scenario(
                ScenarioState(
                    block = "stone",
                ),
            )

        scenario.forward(
            10L,
            ScenarioMutation.Block(
                before = "stone",
                after = "machine",
            ),
        )

        assertEquals(
            "machine",
            scenario.state.block,
        )

        scenario.reverseTo(
            0L,
        )

        assertEquals(
            "stone",
            scenario.state.block,
        )
    }

    @Test
    fun `tile entity nbt change rewinds exactly`() {
        val scenario =
            Scenario(
                ScenarioState(
                    tileValue = 4,
                ),
            )

        scenario.forward(
            10L,
            ScenarioMutation.TileEntity(
                before = 4,
                after = 9001,
            ),
        )

        scenario.reverseTo(
            0L,
        )

        assertEquals(
            4,
            scenario.state.tileValue,
        )
    }

    @Test
    fun `entity spawn despawn and movement rewind in reverse order`() {
        val scenario =
            Scenario(
                ScenarioState(),
            )

        scenario.forward(
            10L,
            ScenarioMutation.SpawnEntity(
                entityId = 7,
                x = 1.0,
            ),
        )
        scenario.forward(
            20L,
            ScenarioMutation.MoveEntity(
                entityId = 7,
                beforeX = 1.0,
                afterX = 8.0,
            ),
        )
        scenario.forward(
            30L,
            ScenarioMutation.RemoveEntity(
                entityId = 7,
                beforeX = 8.0,
            ),
        )

        assertFalse(
            scenario.state.entities
                .containsKey(
                    7,
                ),
        )

        scenario.reverseTo(
            25L,
        )

        assertEquals(
            8.0,
            scenario.state.entities[
                7,
            ],
        )

        scenario.reverseTo(
            15L,
        )

        assertEquals(
            1.0,
            scenario.state.entities[
                7,
            ],
        )

        scenario.reverseTo(
            0L,
        )

        assertFalse(
            scenario.state.entities
                .containsKey(
                    7,
                ),
        )
    }

    @Test
    fun `player inventory held slot sneak and sprint rewind together`() {
        val scenario =
            Scenario(
                ScenarioState(
                    inventory = listOf(
                        "wrench",
                        "scanner",
                    ),
                    heldSlot = 0,
                    sneaking = false,
                    sprinting = true,
                ),
            )

        scenario.forward(
            10L,
            ScenarioMutation.Player(
                beforeInventory =
                listOf(
                    "wrench",
                    "scanner",
                ),
                afterInventory =
                listOf(
                    "circuit",
                    "scanner",
                ),
                beforeHeldSlot = 0,
                afterHeldSlot = 1,
                beforeSneaking = false,
                afterSneaking = true,
                beforeSprinting = true,
                afterSprinting = false,
            ),
        )

        scenario.reverseTo(
            0L,
        )

        assertEquals(
            listOf(
                "wrench",
                "scanner",
            ),
            scenario.state.inventory,
        )
        assertEquals(
            0,
            scenario.state.heldSlot,
        )
        assertFalse(
            scenario.state.sneaking,
        )
        assertTrue(
            scenario.state.sprinting,
        )
    }

    @Test
    fun `weather and world time rewind together`() {
        val scenario =
            Scenario(
                ScenarioState(
                    worldTime = 6000L,
                    raining = false,
                ),
            )

        scenario.forward(
            10L,
            ScenarioMutation.World(
                beforeTime = 6000L,
                afterTime = 18000L,
                beforeRaining = false,
                afterRaining = true,
            ),
        )

        scenario.reverseTo(
            0L,
        )

        assertEquals(
            6000L,
            scenario.state.worldTime,
        )
        assertFalse(
            scenario.state.raining,
        )
    }

    @Test
    fun `repeated forward reverse traversal does not drift`() {
        val initial =
            ScenarioState(
                entities =
                mutableMapOf(
                    1 to 0.0,
                ),
            )

        repeat(20) {
            val scenario =
                Scenario(
                    initial.copyDeep(),
                )

            val mutations =
                mutations()

            mutations.forEach {
                    (
                        time,
                        mutation,
                    ),
                ->
                scenario.forward(
                    time,
                    mutation,
                )
            }

            val forwardState =
                scenario.state
                    .copyDeep()

            scenario.reverseTo(
                0L,
            )

            assertEquals(
                initial,
                scenario.state,
            )

            mutations.forEach {
                    (
                        time,
                        mutation,
                    ),
                ->
                scenario.forward(
                    time,
                    mutation,
                )
            }

            assertEquals(
                forwardState,
                scenario.state,
            )
        }
    }

    @Test
    fun `forward reverse forward converges on identical state`() {
        val scenario =
            Scenario(
                ScenarioState(),
            )

        val mutations =
            mutations()

        mutations.forEach {
                (
                    time,
                    mutation,
                ),
            ->
            scenario.forward(
                time,
                mutation,
            )
        }

        val expected =
            scenario.state
                .copyDeep()

        scenario.reverseTo(
            0L,
        )

        mutations.forEach {
                (
                    time,
                    mutation,
                ),
            ->
            scenario.forward(
                time,
                mutation,
            )
        }

        assertEquals(
            expected,
            scenario.state,
        )
    }

    private fun mutations(): List<Pair<Long, ScenarioMutation>> = listOf(
        10L to
            ScenarioMutation.Block(
                before = "air",
                after = "machine",
            ),
        20L to
            ScenarioMutation.TileEntity(
                before = 0,
                after = 42,
            ),
        30L to
            ScenarioMutation.SpawnEntity(
                entityId = 2,
                x = 1.0,
            ),
        40L to
            ScenarioMutation.MoveEntity(
                entityId = 2,
                beforeX = 1.0,
                afterX = 5.5,
            ),
        50L to
            ScenarioMutation.Player(
                beforeInventory = emptyList(),
                afterInventory = listOf(
                    "tool",
                ),
                beforeHeldSlot = 0,
                afterHeldSlot = 1,
                beforeSneaking = false,
                afterSneaking = true,
                beforeSprinting = false,
                afterSprinting = true,
            ),
        60L to
            ScenarioMutation.World(
                beforeTime = 0L,
                afterTime = 12000L,
                beforeRaining = false,
                afterRaining = true,
            ),
    )

    private class Scenario(
        initialState: ScenarioState,
    ) {

        val state =
            initialState.copyDeep()

        private val timeline =
            ReplayMutationTimeline<ScenarioMutation>(
                historyDurationNanos =
                1_000L,
            )

        init {
            timeline.start(
                0L,
            )
        }

        fun forward(
            timestampNanos: Long,
            mutation: ScenarioMutation,
        ) {
            mutation.apply(
                state,
            )
            timeline.append(
                timestampNanos,
                mutation,
            )
        }

        fun reverseTo(
            timestampNanos: Long,
        ) {
            timeline.removeAfter(
                timestampNanos,
            ).forEach {
                it.undo(
                    state,
                )
            }
        }
    }

    private data class ScenarioState(
        var block: String = "air",
        var tileValue: Int = 0,
        var entities: MutableMap<Int, Double> =
            mutableMapOf(),
        var inventory: List<String> =
            emptyList(),
        var heldSlot: Int = 0,
        var sneaking: Boolean = false,
        var sprinting: Boolean = false,
        var worldTime: Long = 0L,
        var raining: Boolean = false,
    ) {

        fun copyDeep(): ScenarioState = copy(
            entities =
            entities.toMutableMap(),
            inventory =
            inventory.toList(),
        )
    }

    private sealed interface ScenarioMutation {

        fun apply(
            state: ScenarioState,
        )

        fun undo(
            state: ScenarioState,
        )

        data class Block(
            val before: String,
            val after: String,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.block =
                    after
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.block =
                    before
            }
        }

        data class TileEntity(
            val before: Int,
            val after: Int,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.tileValue =
                    after
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.tileValue =
                    before
            }
        }

        data class SpawnEntity(
            val entityId: Int,
            val x: Double,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.entities[
                    entityId,
                ] =
                    x
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.entities.remove(
                    entityId,
                )
            }
        }

        data class RemoveEntity(
            val entityId: Int,
            val beforeX: Double,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.entities.remove(
                    entityId,
                )
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.entities[
                    entityId,
                ] =
                    beforeX
            }
        }

        data class MoveEntity(
            val entityId: Int,
            val beforeX: Double,
            val afterX: Double,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.entities[
                    entityId,
                ] =
                    afterX
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.entities[
                    entityId,
                ] =
                    beforeX
            }
        }

        data class Player(
            val beforeInventory: List<String>,
            val afterInventory: List<String>,
            val beforeHeldSlot: Int,
            val afterHeldSlot: Int,
            val beforeSneaking: Boolean,
            val afterSneaking: Boolean,
            val beforeSprinting: Boolean,
            val afterSprinting: Boolean,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.inventory =
                    afterInventory
                state.heldSlot =
                    afterHeldSlot
                state.sneaking =
                    afterSneaking
                state.sprinting =
                    afterSprinting
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.inventory =
                    beforeInventory
                state.heldSlot =
                    beforeHeldSlot
                state.sneaking =
                    beforeSneaking
                state.sprinting =
                    beforeSprinting
            }
        }

        data class World(
            val beforeTime: Long,
            val afterTime: Long,
            val beforeRaining: Boolean,
            val afterRaining: Boolean,
        ) : ScenarioMutation {

            override fun apply(
                state: ScenarioState,
            ) {
                state.worldTime =
                    afterTime
                state.raining =
                    afterRaining
            }

            override fun undo(
                state: ScenarioState,
            ) {
                state.worldTime =
                    beforeTime
                state.raining =
                    beforeRaining
            }
        }
    }
}
