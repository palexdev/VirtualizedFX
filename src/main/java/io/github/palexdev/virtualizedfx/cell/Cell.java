/*
 * Copyright (C) 2021 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.cell;

import javafx.scene.Node;

/**
 * Base API for all cells.
 */
public interface Cell<T> {

    /**
     * Returns the cell's node.
     * The ideal way to implement a cell would be to extend a JavaFX's pane/region
     * and override this method to return "this".
     */
    Node getNode();

    /**
     * Automatically called by the VirtualFlow
     * <p>
     * This method must be implemented to correctly
     * update the Cell's content on scroll.
     * <p>
     * <b>Note:</b> if the Cell's content is a Node this method should
     * also re-set the Cell's children because (quoting from JavaFX doc)
     * 'A node may occur at most once anywhere in the scene graph' and it's
     * possible that a Node may be removed from a Cell to be the content
     * of another Cell.
     */
    void updateItem(T item);

    /**
     * Automatically called by the VirtualFlow.
     * <p>
     * Cells are dumb, they have no logic, no state.
     * This method allow cells implementations to keep track of a cell's index.
     * <p>
     * Default implementation is empty.
     */
    default void updateIndex(int index) {
    }

    /**
     * Automatically called after the cell has been laid out.
     * <p>
     * Default implementation is empty.
     */
    default void afterLayout() {}

    /**
     * Automatically called before the cell is laid out.
     * <p>
     * Default implementation is empty.
     */
    default void beforeLayout() {}

    /**
     * Automatically called before the cell's node is removed from the container.
     * <p>
     * Default implementation is empty.
     */
    default void dispose() {}
}
