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

package io.github.palexdev.virtualizedfx.cells.base;

import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import javafx.scene.Node;

/**
 * Public, base API for all cells used by any virtualized container. All cells need these three main capabilities:
 * <p> - A way to convert themselves to a {@link Node}, can be implemented in various ways see {@link #toNode()}
 * <p> - A way to keep track of its index
 * <p> - A way to keep track of the displayed item
 * <p></p>
 * Aside from these core functionalities, the API also offers other hooks:
 * <p> - Virtualized containers that make use of a cache (to avoid creating new cells every time one is needed) should
 * make use of {@link #onCache()} and {@link #onDeCache()}. Implementations can track cache/de-cache operations by overriding
 * these methods
 * <p> - When cells are not needed anymore, {@link #dispose()} should be called (automatically handled by the framework
 * don't worry). Here implementations can specify operations to do before the cell is GCd.
 *
 * @param <T> the type of item to display
 */
public interface VFXCell<T> {

    /**
     * Converts the cell to a {@link Node}.
     * <p>
     * Implementations can check these examples:
     * <pre>
     * {@code
     * // Example 1
     * public class SimpleCell<T> extends Label implements VFXCell<T> {
     *     ...
     *     ...
     *
     *     @Override
     *     public Node toNode() {
     *         return this;
     *     }
     * }
     *
     * // Example 2
     * public class SimpleCell<T> implements VFXCell<T> {
     *     private final Label label = ...;
     *     ...
     *     ...
     *
     *     @Override
     *     public Node toNode() {
     *         return label;
     *     }
     * }
     * }
     * </pre>
     */
    Node toNode();

    /**
     * Automatically called by the framework when the cell needs to update its index.
     * <p>
     * Note though, that there is no 100% guarantee the new given index is different from the current one. If you have some
     * operations happen when the index changes, for performance reasons, I recommend you to first ensure the new index really
     * is different.
     * <p>
     * See {@link VFXCellBase} and read how this is handled.
     */
    void updateIndex(int index);

    /**
     * Automatically called by the framework when the cell needs to update its item.
     * <p>
     * Note though, that there is no 100% guarantee the new given item is different from the current one. If you have some
     * operations happen when the item changes, for performance reasons, I recommend you to first ensure the new item really
     * is different.
     * <p>
     * See {@link VFXCellBase} and read how this is handled.
     */
    void updateItem(T item);

    /**
     * The system automatically calls this before the cell is laid out.
     */
    default void beforeLayout() {}

    /**
     * The system automatically calls this after the cell is laid out.
     */
    default void afterLayout() {}

    /**
     * Called when a cell is created and associated with a {@link VFXContainer}. This method provides the cell with
     * a reference to its container, allowing for any necessary initialization that depends on the container context.
     * <p>
     * By default, this method does nothing. Subclasses can override it to implement container-specific setup
     * or to access container properties and methods at the time of creation.
     * <p></p>
     * <b>Note:</b> This method should only be called by the container that created this cell. Calling it
     * with a different or incorrect container instance may lead to inconsistent behavior or errors.
     *
     * @param container the {@link VFXContainer} instance that owns this cell
     */
    default void onCreated(VFXContainer<T> container) {}

    /**
     * Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
     * should call this when adding the cell to the cache.
     * <p>
     * Users can intercept/hook to this operation by overriding this method.
     */
    default void onCache() {}

    /**
     * Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
     * should call this when removing the cell from the cache.
     * <p>
     * Users can intercept/hook to this operation by overriding this method.
     */
    default void onDeCache() {}

    /**
     * Automatically called by the framework when the cell is not needed anymore. The user can override this to perform
     * some operations before the cell is GCd.
     */
    default void dispose() {}
}
