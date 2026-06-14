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
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import javafx.scene.Node;

/// Public, base API for all cells used by any virtualized container. All cells need these three main capabilities:
///
/// - A way to convert themselves to a [Node], can be implemented in various ways see [#toNode()]
/// - A way to keep track of its index
/// - A way to keep track of the displayed item
///
/// Aside from these core functionalities, the API also offers other hooks:
///
/// - Virtualized containers that make use of a cache (to avoid creating new cells every time one is needed) should
/// make use of [#onCache()] and [#onDeCache()]. Implementations can track cache/de-cache operations by overriding
/// these methods
///
/// - When cells are not needed anymore, [#dispose()] should be called (automatically handled by the framework
/// don't worry). Here implementations can specify operations to do before the cell is GCd.
///
/// @param <T> the type of item to display
public interface VFXCell<T> {

    /// Converts the cell to a [Node].
    ///
    /// Implementations can check these examples:
    /// ```
    /// // Example 1
    /// public class SimpleCell<T> extends Label implements VFXCell<T> {
    ///     ...
    ///     ...
    ///          Node toNode() {
    ///         return this;
    ///     }
    /// }
    /// // Example 2
    /// public class SimpleCell<T> implements VFXCell<T> {
    ///     private final Label label = ...;
    ///     ...
    ///     ...
    ///          Node toNode() {
    ///         return label;
    ///     }
    /// }
    /// ```
    Node toNode();

    /// Automatically called by the framework when the cell needs to update its index.
    ///
    /// Note, though, that there is no 100% guarantee the new given index is different from the current one. If you have some
    /// operations happen when the index changes, for performance reasons, I recommend you to first ensure the new index really
    /// is different.
    ///
    /// See [VFXCellBase] and read how this is handled.
    void updateIndex(int index);

    /// Automatically called by the framework when the cell needs to update its item.
    ///
    /// Note though, that there is no 100% guarantee the new given item is different from the current one. If you have some
    /// operations happen when the item changes, for performance reasons, I recommend you to first ensure the new item really
    /// is different.
    ///
    /// See [VFXCellBase] and read how this is handled.
    void updateItem(T item);

    /// The system automatically calls this before the cell is laid out.
    default void beforeLayout() {}

    /// The system automatically calls this after the cell is laid out.
    default void afterLayout() {}

    /// Called when a cell is created and associated with a [VFXContainer]. This method provides the cell with
    /// a reference to its container, along with additional services that the cell may use to extend its functionalities.
    ///
    /// _It's up to the implementation to decide how to use it._
    ///
    /// **Note:** This method should only be called by the container that created this cell. Calling it
    /// with a different or incorrect container instance may lead to inconsistent behavior or errors.
    ///
    /// @see VFXContext
    default void onCreated(VFXContext<T> context) {}

    /// Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
    /// should call this when adding the cell to the cache.
    ///
    /// Users can intercept/hook to this operation by overriding this method.
    default void onCache() {}

    /// Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
    /// should call this when removing the cell from the cache.
    ///
    /// Users can intercept/hook to this operation by overriding this method.
    default void onDeCache() {}

    /// Automatically called by the framework when the cell is not needed anymore. The user can override this to perform
    /// some operations before the cell is GCd.
    default void dispose() {}
}
