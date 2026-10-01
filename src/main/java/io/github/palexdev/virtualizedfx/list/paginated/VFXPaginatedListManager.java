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

package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListManager;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Orientation;
import javafx.scene.Parent;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// The manager of [VFXPaginatedList], extends [VFXListManager].
///
/// This is necessary to respond to the following property changes introduced by the paginated variant:
///
/// - cells per page changes, [#onCellsPerPageChanged()]
/// - max page changes, [#onMaxPageChanged()]
///
/// It also ties the list's position to the page, see [#installPagination()].
///
/// Finally, it modifies some of the computations already defined in [VFXListManager] because the paginated variant should
/// respond differently in some cases, and also to optimize performance as much as possible.
public class VFXPaginatedListManager<T, C extends VFXCell<T>> extends VFXListManager<T, C> {

    //================================================================================
    // Properties
    //================================================================================

    private DoubleBinding posBinding;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXPaginatedListManager(VFXPaginatedList<T, C> list) {
        super(list);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Completes the installation for the paginated variant, [#install()] handles the properties of [VFXList]. Called
    /// once by [VFXPaginatedList] at the end of its construction: [#install()] runs during [VFXList]'s construction,
    /// when the paginated list's own properties do not exist yet.
    ///
    /// Builds the binding that ties the list's position to the page, `page * cellsPerPage * (cellSize + spacing)`,
    /// binds the position along the orientation to it, [#swapPositionBinding()], and registers the listeners on the
    /// [VFXPaginatedList#cellsPerPageProperty()] and the [VFXPaginatedList#maxPageProperty()].
    protected void installPagination() {
        VFXPaginatedList<T, C> list = getList();
        posBinding = DoubleBindingBuilder.build()
            .setMapper(() -> list.getPage() * list.getCellsPerPage() * (list.getCellSize() + list.getSpacing()))
            .addSources(list.pageProperty(), list.cellsPerPageProperty(), list.cellSizeProperty(), list.spacingProperty())
            .get();
        swapPositionBinding();
        onInvalidated(list.cellsPerPageProperty()).then(_ -> onCellsPerPageChanged()).listen();
        onInvalidated(list.maxPageProperty()).then(_ -> onMaxPageChanged()).listen();
    }

    /// Only the position along the list's orientation is bound to the page. This binds it, and unbinds the other one.
    ///
    /// Called by [#installPagination()], and at the end of [#onOrientationChanged()].
    protected void swapPositionBinding() {
        VFXPaginatedList<T, C> list = getList();
        if (list.getOrientation() == Orientation.VERTICAL) {
            list.hPosProperty().unbind();
            list.vPosProperty().bind(posBinding);
        } else {
            list.vPosProperty().unbind();
            list.hPosProperty().bind(posBinding);
        }
    }

    /// A paginated container's size strictly depends on how many cells/rows/items is set to display per page, and this
    /// is enforced by the default skin [VFXPaginatedListSkin], see [VFXPaginatedListSkin#getLength()].
    ///
    /// This core method is called whenever the [VFXPaginatedList#cellsPerPageProperty()] changes and ensures that
    /// the layout bounds of the container become invalid ([Parent#isNeedsLayout()] becomes 'true'), by calling
    /// [Parent#requestLayout()].
    ///
    /// This way, computations that also rely on the container size become invalid too, thus leading to correct values.
    protected void onCellsPerPageChanged() {
        getList().requestLayout();
    }

    /// This core method ensures that the paginated container is always at a valid page/position when the
    /// [VFXPaginatedList#maxPageProperty()] changes.
    ///
    /// The only one case this needs to correct the position is when the current page is greater than the new max page.
    /// (both in terms of indexes ofc).
    ///
    /// **Important note:** the page change would trigger a position change (so [#onPositionChanged()]), but this
    /// method avoids it by setting the [#invalidatingPos] flag to 'true' before and immediately re-setting it to false
    /// after. This may sound counterintuitive, but there is a reason.
    ///
    /// You see, the max page property can change on two occasions:
    ///
    /// 1) the number of items changes
    ///
    /// 2) the cells per page changes
    ///
    /// **BUT...** in the first case, we have a change in the list which is handled by [#onItemsChanged()],
    /// and the second case is handled by [#onGeometryChanged()]. This last case is very peculiar, because it will work
    /// only if the container's skin is implemented to adapt to the number of items per page. It's the skin's implementation
    /// to trigger the geometry change, and this should be the intended default behavior.
    ///
    /// If you want to make a skin that doesn't follow this logic, then you probably want to change this method too.
    protected void onMaxPageChanged() {
        VFXPaginatedList<T, C> list = getList();
        int page = list.getPage();
        int max = list.getMaxPage();
        if (page > max) {
            invalidatingPos = true;
            list.moveBy(0);
            invalidatingPos = false;
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Overridden to just call [VFXList#requestViewportLayout()].
    ///
    /// For the paginated variant, the cells' size is irrelevant for its state. The default skin adapts the container's
    /// size to the size of each cell as well as the number of cells per page.
    @Override
    protected void onCellSizeChanged() {
        getList().requestViewportLayout();
    }

    /// As also described in the super method ([VFXListManager#onOrientationChanged()]), when the orientation changes
    /// the most reasonable behavior is to reset both the positions to 0.0. For the paginated variant this requires extra
    /// steps because, according to the previous orientation, the vPos or hPos properties are bound. The reset also
    /// requires setting the page to 0. Finally, the position along the new orientation is bound to the page,
    /// [#swapPositionBinding()].
    @Override
    protected void onOrientationChanged() {
        VFXPaginatedList<T, C> list = getList();
        list.vPosProperty().unbind();
        list.hPosProperty().unbind();
        list.setPage(0);
        super.onOrientationChanged();
        swapPositionBinding();
    }

    /// Overridden to cast to [VFXPaginatedList] since this manager only works with that type.
    @Override
    protected VFXPaginatedList<T, C> getList() {
        return (VFXPaginatedList<T, C>) super.getList();
    }
}
