package dev.sbs.minecraftapi.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@Getter
@RequiredArgsConstructor
public enum CraftingTable {

    TOP_LEFT(10),
    TOP_CENTER(11),
    TOP_RIGHT(12),

    MIDDLE_LEFT(19),
    MIDDLE_CENTER(20),
    MIDDLE_RIGHT(21),

    BOTTOM_LEFT(28),
    BOTTOM_CENTER(29),
    BOTTOM_RIGHT(30);

    private final int slot;

    public int getZeroSlot() {
        return this.getZeroSlot(9);
    }

    public int getZeroSlot(@Range(from = 3, to = 9) int rowSize) {
        return this.getSlot() - 10 - (rowSize - 3);
    }

    @Getter
    public enum Recipe {

        ALL(
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT,
            MIDDLE_LEFT,
            MIDDLE_CENTER,
            MIDDLE_RIGHT,
            BOTTOM_LEFT,
            BOTTOM_CENTER,
            BOTTOM_RIGHT
        ),
        BOX(
            TOP_LEFT,
            TOP_CENTER,
            MIDDLE_LEFT,
            MIDDLE_CENTER
        ),
        BUCKET(
            MIDDLE_LEFT,
            BOTTOM_CENTER,
            MIDDLE_RIGHT
        ),
        CENTER2(
            MIDDLE_CENTER,
            BOTTOM_CENTER
        ),
        CENTER3(
            TOP_CENTER,
            MIDDLE_CENTER,
            BOTTOM_CENTER
        ),
        DIAGONAL(
            BOTTOM_LEFT,
            MIDDLE_CENTER,
            TOP_RIGHT
        ),
        DOUBLE_ROW(
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT,
            MIDDLE_LEFT,
            MIDDLE_CENTER,
            MIDDLE_RIGHT
        ),
        ENCHANT(
            MIDDLE_CENTER,
            MIDDLE_RIGHT,
            BOTTOM_CENTER,
            BOTTOM_RIGHT
        ),
        ENCHANT_FISH(
            TOP_LEFT,
            MIDDLE_CENTER,
            MIDDLE_RIGHT,
            BOTTOM_CENTER,
            BOTTOM_RIGHT
        ),
        HOPPER(
            TOP_LEFT,
            MIDDLE_LEFT,
            BOTTOM_CENTER,
            MIDDLE_RIGHT,
            TOP_RIGHT,
            MIDDLE_CENTER
        ),
        MIDDLE2(
            MIDDLE_LEFT,
            MIDDLE_CENTER
        ),
        RING(
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT,
            MIDDLE_LEFT,
            MIDDLE_RIGHT,
            BOTTOM_LEFT,
            BOTTOM_CENTER,
            BOTTOM_RIGHT
        ),
        SINGLE(
            TOP_LEFT
        ),
        SINGLE_ROW(
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT
        ),
        STAR(
            TOP_CENTER,
            MIDDLE_LEFT,
            MIDDLE_RIGHT,
            BOTTOM_CENTER,
            MIDDLE_CENTER
        ),
        TOP_ROW(
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT
        ),
        TRIANGLE(
            MIDDLE_LEFT,
            MIDDLE_CENTER,
            BOTTOM_LEFT
        ),
        NONE;

        private final @NotNull ConcurrentList<CraftingTable> slots;

        Recipe(@NotNull CraftingTable... slots) {
            this.slots = Concurrent.newList(slots);
        }

    }

}
