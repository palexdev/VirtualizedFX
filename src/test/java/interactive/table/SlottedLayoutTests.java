/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package interactive.table;

import java.util.Collection;

import io.github.palexdev.virtualizedfx.table.defaults.VFXSimpleTableColumnSkin.Slot;
import io.github.palexdev.virtualizedfx.table.defaults.VFXSimpleTableColumnSkin.SlottedLayout;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static javafx.scene.control.ContentDisplay.LEFT;
import static javafx.scene.control.ContentDisplay.RIGHT;
import static org.junit.jupiter.api.Assertions.*;

/// No table and no scene here, the extension is only there to have the toolkit running.
///
/// Unless a test says otherwise, the area starts at 10 and is 100 wide (so it ends at 110), the text is 20 wide,
/// the graphic 16 and the gap 8. Text, gap and graphic together take 44.
@ExtendWith(ApplicationExtension.class)
public class SlottedLayoutTests {
    private static final double X = 10;
    private static final double Y = 0;
    private static final double W = 100;
    private static final double H = 32;
    private static final double GAP = 8;

    private Region text;
    private Region graphic;

    @BeforeEach
    void setup() {
        text = sized(20);
        graphic = sized(16);
    }

    //================================================================================
    // Attached
    //================================================================================

    @Test
    void testAttachedLeft() {
        // The row starts at 10: text at 10, graphic at 10 + 20 + 8
        Collection<Slot> slots = layout(Pos.CENTER_LEFT, RIGHT, false).compute(X, Y, W, H);
        assertSlot(slots, text, 10, 20, HPos.LEFT);
        assertSlot(slots, graphic, 38, 16, HPos.LEFT);
    }

    @Test
    void testAttachedCenter() {
        // The row starts at 10 + (100 - 44) / 2 = 38: text at 38, graphic at 38 + 20 + 8
        Collection<Slot> slots = layout(Pos.CENTER, RIGHT, false).compute(X, Y, W, H);
        assertSlot(slots, text, 38, 20, HPos.LEFT);
        assertSlot(slots, graphic, 66, 16, HPos.LEFT);
    }

    @Test
    void testAttachedRight() {
        // The row starts at 10 + (100 - 44) = 66: text at 66, graphic at 66 + 20 + 8, ending at 110
        Collection<Slot> slots = layout(Pos.CENTER_RIGHT, RIGHT, false).compute(X, Y, W, H);
        assertSlot(slots, text, 66, 20, HPos.LEFT);
        assertSlot(slots, graphic, 94, 16, HPos.LEFT);
    }

    @Test
    void testAttachedCenterGraphicFirst() {
        // The row starts at 38: graphic at 38, text at 38 + 16 + 8
        Collection<Slot> slots = layout(Pos.CENTER, LEFT, false).compute(X, Y, W, H);
        assertSlot(slots, text, 62, 20, HPos.LEFT);
        assertSlot(slots, graphic, 38, 16, HPos.LEFT);
    }

    //================================================================================
    // Detached
    //================================================================================

    @Test
    void testDetachedLeft() {
        // The graphic's slot is the whole area, with the graphic at its right edge. Text at the start
        Collection<Slot> slots = layout(Pos.CENTER_LEFT, RIGHT, true).compute(X, Y, W, H);
        assertSlot(slots, text, 10, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 100, HPos.RIGHT);
    }

    @Test
    void testDetachedCenter() {
        // Text at the center of the whole area, 10 + (100 - 20) / 2 = 50
        // It ends at 70, before the graphic (at 94) minus the gap
        Collection<Slot> slots = layout(Pos.CENTER, RIGHT, true).compute(X, Y, W, H);
        assertSlot(slots, text, 50, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 100, HPos.RIGHT);
    }

    @Test
    void testDetachedRight() {
        // Text would be at 110 - 20 = 90, the graphic and the gap stop it at 110 - 16 - 8 - 20 = 66
        Collection<Slot> slots = layout(Pos.CENTER_RIGHT, RIGHT, true).compute(X, Y, W, H);
        assertSlot(slots, text, 66, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 100, HPos.RIGHT);
    }

    @Test
    void testDetachedCenterGraphicFirst() {
        // The graphic is at the left edge of the area. Text at the center, 50, which is past 10 + 16 + 8 = 34
        Collection<Slot> slots = layout(Pos.CENTER, LEFT, true).compute(X, Y, W, H);
        assertSlot(slots, text, 50, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 100, HPos.LEFT);
    }

    @Test
    void testDetachedLeftGraphicFirst() {
        // Text would be at 10, the graphic and the gap push it to 10 + 16 + 8 = 34
        Collection<Slot> slots = layout(Pos.CENTER_LEFT, LEFT, true).compute(X, Y, W, H);
        assertSlot(slots, text, 34, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 100, HPos.LEFT);
    }

    @Test
    void testDetachedCenterNarrow() {
        // Area of 50, ending at 60, so the graphic is at 60 - 16 = 44
        // Text would be at 10 + (50 - 20) / 2 = 25 and end at 45, it's stopped at 60 - 16 - 8 - 20 = 16
        Collection<Slot> slots = layout(Pos.CENTER, RIGHT, true).compute(X, Y, 50, H);
        assertSlot(slots, text, 16, 20, HPos.LEFT);
        assertSlot(slots, graphic, 10, 50, HPos.RIGHT);
    }

    //================================================================================
    // Both modes
    //================================================================================

    @Test
    void testTruncated() {
        // Area of 30. The text gets what the graphic and the gap leave, 30 - 16 - 8 = 6
        // Attached: the row is as wide as the area, text at 10, graphic at 10 + 6 + 8
        Collection<Slot> slots = layout(Pos.CENTER, RIGHT, false).compute(X, Y, 30, H);
        assertSlot(slots, text, 10, 6, HPos.LEFT);
        assertSlot(slots, graphic, 24, 16, HPos.LEFT);

        // Detached: the text can only be at 10, where its slot starts and ends
        slots = layout(Pos.CENTER, RIGHT, true).compute(X, Y, 30, H);
        assertSlot(slots, text, 10, 6, HPos.LEFT);
        assertSlot(slots, graphic, 10, 30, HPos.RIGHT);
    }

    @Test
    void testNoRoom() {
        // Area of 20, less than the graphic plus the gap (24): the text gets nothing
        // Attached: the row starts at 10 whatever the alignment is, the graphic is at 10 + 0 + 8 and overflows
        Collection<Slot> slots = layout(Pos.CENTER, RIGHT, false).compute(X, Y, 20, H);
        assertSlot(slots, text, 10, 0, HPos.LEFT);
        assertSlot(slots, graphic, 18, 16, HPos.LEFT);

        // Detached: the text's slot can't start before 10, and should end by 10 + 20 - 24 = 6. The start wins
        slots = layout(Pos.CENTER, RIGHT, true).compute(X, Y, 20, H);
        assertSlot(slots, text, 10, 0, HPos.LEFT);
        assertSlot(slots, graphic, 10, 20, HPos.RIGHT);
    }

    @Test
    void testTextOnly() {
        // The gap doesn't count: 10 + (100 - 20) / 2 = 50, in both modes
        // Either there is no graphic, or the content display hides it
        for (boolean detached : new boolean[]{false, true}) {
            Collection<Slot> slots = layout(text, null, Pos.CENTER, RIGHT, detached).compute(X, Y, W, H);
            assertEquals(1, slots.size());
            assertSlot(slots, text, 50, 20, HPos.LEFT);

            slots = layout(Pos.CENTER, ContentDisplay.TEXT_ONLY, detached).compute(X, Y, W, H);
            assertEquals(1, slots.size());
            assertSlot(slots, text, 50, 20, HPos.LEFT);
        }
    }

    @Test
    void testGraphicOnly() {
        // The gap doesn't count: 10 + (100 - 16) / 2 = 52, in both modes
        // Either there is no text, on both sides, or the content display hides it
        for (boolean detached : new boolean[]{false, true}) {
            for (ContentDisplay display : new ContentDisplay[]{LEFT, RIGHT}) {
                Collection<Slot> slots = layout(null, graphic, Pos.CENTER, display, detached).compute(X, Y, W, H);
                assertEquals(1, slots.size());
                assertSlot(slots, graphic, 52, 16, HPos.LEFT);
            }

            Collection<Slot> slots = layout(Pos.CENTER, ContentDisplay.GRAPHIC_ONLY, detached).compute(X, Y, W, H);
            assertEquals(1, slots.size());
            assertSlot(slots, graphic, 52, 16, HPos.LEFT);
        }
    }

    @Test
    void testUnsupportedDisplays() {
        // Everything that is not LEFT, or one of the two "only", puts the graphic after the text, like RIGHT
        // Attached and centered: text at 38, graphic at 66, see testAttachedCenter()
        ContentDisplay[] displays = {ContentDisplay.TOP, ContentDisplay.BOTTOM, ContentDisplay.CENTER, null};
        for (ContentDisplay display : displays) {
            Collection<Slot> slots = layout(Pos.CENTER, display, false).compute(X, Y, W, H);
            assertSlot(slots, text, 38, 20, HPos.LEFT);
            assertSlot(slots, graphic, 66, 16, HPos.LEFT);
        }
    }

    @Test
    void testNothingShown() {
        // The only node there is, is hidden by the content display
        assertTrue(layout(text, null, Pos.CENTER, ContentDisplay.GRAPHIC_ONLY, false).compute(X, Y, W, H).isEmpty());
        assertTrue(layout(null, graphic, Pos.CENTER, ContentDisplay.TEXT_ONLY, false).compute(X, Y, W, H).isEmpty());
    }

    @Test
    void testNothingToLayOut() {
        assertThrows(IllegalArgumentException.class, () -> new SlottedLayout(null, null));
    }

    @Test
    void testVertical() {
        // Every slot is as tall as the area, the node is aligned inside it
        for (Slot slot : layout(Pos.TOP_CENTER, RIGHT, true).compute(X, 5, W, H)) {
            assertEquals(5, slot.y());
            assertEquals(H, slot.h());
            assertEquals(VPos.TOP, slot.vpos());
        }

        // Baseline is treated as center
        for (Slot slot : layout(Pos.BASELINE_CENTER, RIGHT, false).compute(X, Y, W, H)) {
            assertEquals(VPos.CENTER, slot.vpos());
        }
    }

    @Test
    void testDefaults() {
        // Nothing set: no gap, aligned at the start, graphic after the text, attached
        // Text at 10, graphic at 10 + 20
        Collection<Slot> slots = new SlottedLayout(text, graphic).compute(X, Y, W, H);
        assertSlot(slots, text, 10, 20, HPos.LEFT);
        assertSlot(slots, graphic, 30, 16, HPos.LEFT);
    }

    //================================================================================
    // Utils
    //================================================================================

    private SlottedLayout layout(Pos alignment, ContentDisplay display, boolean detached) {
        return layout(text, graphic, alignment, display, detached);
    }

    private static SlottedLayout layout(Node text, Node graphic, Pos alignment, ContentDisplay display, boolean detached) {
        return new SlottedLayout(text, graphic)
            .gap(GAP)
            .alignment(alignment)
            .contentDisplay(display)
            .detached(detached);
    }

    private static Region sized(double width) {
        Region region = new Region();
        region.setPrefSize(width, 10);
        return region;
    }

    private static void assertSlot(Collection<Slot> slots, Node node, double x, double w, HPos hpos) {
        Slot slot = slots.stream()
            .filter(s -> s.node() == node)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No slot for the node"));
        assertEquals(x, slot.x());
        assertEquals(w, slot.w());
        assertEquals(hpos, slot.hpos());
    }
}
