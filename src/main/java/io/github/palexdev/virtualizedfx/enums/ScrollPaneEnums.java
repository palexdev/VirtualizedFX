/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.enums;

import javafx.geometry.HPos;
import javafx.geometry.VPos;

/// Convenience class that contains several enumerators for scroll panes.
public class ScrollPaneEnums {

    private ScrollPaneEnums() {}

    /// Enumeration to specify the alignment of the scroll bars within a scroll pane.
    public enum ScrollBarsAlignment {
        /// Bars are aligned to the edges of the scroll pane (top for the vertical bar, left for the horizontal bar).
        DEFAULT,

        /// Bars are centered along their main axis within the scroll pane.
        CENTERED
    }

    /// Enumeration to define the visibility of a scroll pane's scroll bars.
    public enum ScrollBarPolicy {
        /// The scroll bars will be visible when needed.
        DEFAULT,

        /// The scroll bars will never be visible.
        NEVER
    }

    /// Enumeration to specify the position of the vertical scroll bar
    /// in a scroll pane.
    public enum VBarPos {
        RIGHT, LEFT;

        /// Converts this enumeration to an [HPos] object.
        public HPos toHPos() {
            return (this == LEFT) ? HPos.LEFT : HPos.RIGHT;
        }
    }

    /// Enumeration to specify the position of the horizontal scroll bar
    /// in a scroll pane.
    public enum HBarPos {
        BOTTOM, TOP;

        /// Converts this enumeration to a [VPos] object.
        public VPos toVPos() {
            return (this == TOP) ? VPos.TOP : VPos.BOTTOM;
        }
    }
}
