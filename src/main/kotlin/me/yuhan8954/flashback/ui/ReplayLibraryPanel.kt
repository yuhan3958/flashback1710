package me.yuhan8954.flashback.ui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.CustomModularScreen
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import me.yuhan8954.flashback.Flashback1710
import me.yuhan8954.flashback.io.ReplayLibraryEntry
import me.yuhan8954.flashback.io.ReplayReadStatus

class ReplayLibraryPanel(
    private val entries: List<ReplayLibraryEntry>,
    private val onPlay: (ReplayLibraryEntry) -> Unit,
) : CustomModularScreen(
    Flashback1710.MODID,
) {

    private var page =
        0

    init {
        drawDarkBackground(
            false,
        )
    }

    override fun buildUI(
        context: ModularGuiContext,
    ): ModularPanel {
        val panel =
            ModularPanel(
                PANEL_NAME,
            ).fullScreenInvisible()
                .background(
                    ReplayUiStyle.panelBackground(),
                )

        panel.child(
            ReplayTextWidget(
                "REPLAY LIBRARY",
            ).left(10)
                .top(8)
                .color(
                    ReplayUiStyle.TEXT_COLOR,
                ),
        )

        panel.child(
            ReplayTextWidget(
                IKey.dynamic {
                    val pageCount =
                        pageCount()

                    "Page " +
                        (
                            page +
                                1
                            ) +
                        " / " +
                        pageCount
                },
            ).right(10)
                .top(8)
                .width(90)
                .height(10)
                .color(
                    ReplayUiStyle.MUTED_TEXT_COLOR,
                ),
        )

        repeat(
            PAGE_SIZE,
        ) { row ->
            val top =
                28 +
                    row *
                    ROW_HEIGHT

            panel.child(
                ReplayTextWidget(
                    IKey.dynamic {
                        rowLabel(
                            row,
                        )
                    },
                ).left(10)
                    .top(top)
                    .right(74)
                    .height(18)
                    .color(
                        ReplayUiStyle.TEXT_COLOR,
                    ),
            )

            panel.child(
                ReplayButtonWidget()
                    .right(10)
                    .top(top - 3)
                    .size(56, 18)
                    .background(
                        ReplayUiStyle.buttonBackground(),
                    )
                    .overlay(
                        IKey.dynamic {
                            if (
                                entryForRow(
                                    row,
                                )?.playable ==
                                true
                            ) {
                                "Play"
                            } else {
                                "-"
                            }
                        },
                    ).onMousePressed {
                        if (it != 0) {
                            false
                        } else {
                            entryForRow(
                                row,
                            )?.takeIf { entry ->
                                entry.playable
                            }?.let(
                                onPlay,
                            )

                            true
                        }
                    },
            )
        }

        panel.child(
            ReplayButtonWidget()
                .left(10)
                .bottom(10)
                .size(60, 18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.str(
                        "< Prev",
                    ),
                ).onMousePressed {
                    if (
                        it == 0 &&
                        page > 0
                    ) {
                        page--
                    }

                    it == 0
                },
        )

        panel.child(
            ReplayButtonWidget()
                .left(76)
                .bottom(10)
                .size(60, 18)
                .background(
                    ReplayUiStyle.buttonBackground(),
                )
                .overlay(
                    IKey.str(
                        "Next >",
                    ),
                ).onMousePressed {
                    if (
                        it == 0 &&
                        page <
                        pageCount() -
                        1
                    ) {
                        page++
                    }

                    it == 0
                },
        )

        return panel
    }

    private fun entryForRow(
        row: Int,
    ): ReplayLibraryEntry? = entries.getOrNull(
        page *
            PAGE_SIZE +
            row,
    )

    private fun rowLabel(
        row: Int,
    ): String {
        val entry =
            entryForRow(
                row,
            ) ?: return ""

        val fileName =
            if (
                entry.file.name.length <=
                MAX_DISPLAY_NAME
            ) {
                entry.file.name
            } else {
                entry.file.name
                    .take(
                        MAX_DISPLAY_NAME -
                            3,
                    ) +
                    "..."
            }

        return statusPrefix(
            entry.status,
        ) +
            entry.status.name +
            " §f" +
            fileName +
            " §7" +
            ReplayTimeFormatter.format(
                entry.durationNanos,
            )
    }

    private fun pageCount(): Int = maxOf(
        1,
        (
            entries.size +
                PAGE_SIZE -
                1
            ) /
            PAGE_SIZE,
    )

    private fun statusPrefix(
        status: ReplayReadStatus,
    ): String = when (status) {
        ReplayReadStatus.CLEAN ->
            "§a"

        ReplayReadStatus.TRUNCATED ->
            "§e"

        ReplayReadStatus.CORRUPT ->
            "§c"

        ReplayReadStatus.LEGACY ->
            "§7"
    }

    companion object {

        const val PANEL_NAME =
            "replay_library"

        private const val PAGE_SIZE =
            8

        private const val ROW_HEIGHT =
            24

        private const val MAX_DISPLAY_NAME =
            42
    }
}
