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

package io.github.palexdev.virtualizedfx.list;

import java.util.Optional;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainerHelper;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * This interface is a utility API for {@link VFXList} which helps to avoid if checks that depend on the container's
 * orientation, {@link VFXList#orientationProperty()}. There are two concrete implementations: {@link VerticalHelper}
 * and {@link HorizontalHelper}
 * <p></p>
 * A little <b>note</b> on the virtual max X/Y properties.
 * <p></p>
 * The axis property which is the opposite of the current container's orientation ({@link VFXList#orientationProperty()}),
 * specifies the biggest cell size (width/height) so that if {@link VFXList#fitToViewportProperty()} is set to false we
 * know how much we can scroll the list in that direction.
 * <p>
 * This value, however, is dynamic, since the size of each node is computed only once it is laid out. This means that the absolute
 * maximum value is only found when all items have been displayed at least once.
 */
public interface VFXListHelper<T, C extends VFXCell<T>> extends VFXContainerHelper<T, VFXList<T, C>> {

    /**
     * @return the index of the first visible item
     */
    int firstVisible();

    /**
     * @return the index of the last visible item
     */
    int lastVisible();

    /**
     * @return the number of cells visible in the viewport. Not necessarily the same as {@link #totalNum()}
     */
    int visibleNum();

    /**
     * @return the total number of cells in the viewport which doesn't include only the number of visible cells but also
     * the number of buffer cells
     */
    int totalNum();

    /**
     * Specifies the range of items present in the list. This also takes into account buffer items, see {@link #visibleNum()}
     * and {@link #totalNum()}
     */
    ReadOnlyObjectProperty<NumberRange<Integer>> rangeProperty();

    /**
     * @return the range of items present in the list. This also takes into account buffer items, see {@link #visibleNum()}
     * and {@link #totalNum()}
     */
    default IntegerRange range() {
        return (IntegerRange) rangeProperty().get();
    }

    /**
     * Computes the width or height of the cell depending on the container's orientation.
     * <p> - VERTICAL -> width
     * <p> - HORIZONTAL -> height
     */
    double computeSize(Node node);

    /**
     * Lays out the given cell. The index parameter is necessary to identify the position of a cell compared to the others
     * (comes before or after).
     *
     * @param layoutIndex the absolute index of the given node/cell, see {@link VFXListSkin#layout()}
     */
    void layout(int layoutIndex, VFXCell<T> cell);


    /**
     * Scrolls in the viewport by the given number of pixels.
     */
    void scrollBy(double pixels);

    /**
     * Scrolls in the viewport to the given pixel value.
     */
    void scrollToPixel(double pixel);

    /**
     * Scrolls in the viewport to the given item's index.
     */
    void scrollToIndex(int index);

    /**
     * Converts the given index to a cell. Uses {@link #itemToCell(Object)}.
     */
    default C indexToCell(int index) {
        T item = indexToItem(index);
        return itemToCell(item);
    }

    /**
     * Converts the given item to a cell. The result is either on of the cells cached in {@link VFXCellsCache} that
     * is updated with the given item, or a totally new one created by the {@link VFXList#getCellFactory()}.
     */
    default C itemToCell(T item) {
        VFXCellsCache<T, C> cache = getContainer().getCache();
        Optional<C> opt = cache.tryTake();
        opt.ifPresent(c -> c.updateItem(item));
        return opt.orElseGet(() -> getContainer().create(item));
    }

    /**
     * Implementing the {@link VFXList#spacingProperty()} has been incredibly easy. It is enough to think at the
     * spacing as an extension of the {@link VFXList#cellSizeProperty()}. In other words, for the helper to still
     * produce valid ranges, it is enough to sum the spacing to the cell size when the latter is needed.
     * This is a shortcut for {@code getContainer().getCellSize() + getContainer().getSpacing()}.
     */
    default double getTotalCellSize() {
        return getContainer().getCellSize() + getContainer().getSpacing();
    }

    /**
     * Extension of {@link VFXContainerHelperBase} which also implements {@link VFXListHelper}.
     * Defines common properties and operations for the two concrete implementations {@link VerticalHelper} and
     * {@link HorizontalHelper}, such as:
     * <p> - the range of items to display as a {@link IntegerRangeProperty}
     * <p> - the total number of cells in the viewport
     */
    abstract class AbstractHelper<T, C extends VFXCell<T>> extends VFXContainerHelperBase<T, VFXList<T, C>> implements VFXListHelper<T, C> {
        protected final IntegerRangeProperty range = new IntegerRangeProperty();

        public AbstractHelper(VFXList<T, C> list) {
            super(list);
        }

        @Override
        public int totalNum() {
            int visible = visibleNum();
            return visible == 0 ? 0 : Math.min(visible + container.getBufferSize().val() * 2, container.size());
        }

        @Override
        public IntegerRangeProperty rangeProperty() {
            return range;
        }
    }

    /**
     * Concrete implementation of {@link AbstractHelper} for {@link Orientation#VERTICAL}. Here the range of items to
     * display and the viewport position are defined as follows:
     * <p> - the range is given by the {@link #firstVisible()} element minus the buffer size ({@link VFXList#bufferSizeProperty()}),
     * (cannot be negative) and the sum between this start index and the total number of needed cells given by {@link #totalNum()},
     * (cannot exceed the number of items - 1). It may happen the number of indexes given by the range {@code end - start + 1} is lesser
     * than the total number of cells we need. In such cases, the range start is corrected to be {@code end - needed + 1}.
     * A typical situation for this is when the list position reaches the max scroll.
     * The range computation has the following dependencies: the list's height, the virtual max y, the buffer size and
     * the vertical position.
     * <p> - the viewport position, a computation that is at the core of virtual scrolling. The viewport, which contains the cells,
     * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
     * The horizontal position is just the current {@link VFXList#hPosProperty()} but negative. The vertical
     * position is the one virtualized.
     * First we get the range of items to display and the total cell size given by {@link #getTotalCellSize()}, yes, the
     * spacing also affects the position. Then we compute the range to the first visible cell, which is given by
     * {@code IntegerRange.of(range.getMin(), firstVisible())}, in other words we limit the 'complete' range to the
     * top buffer including the first cell after the buffer. The number of indexes in this newfound range
     * (given by {@link IntegerRange#diff()}) is multiplied by the total cell size, this way we found the number of pixels to the
     * first visible cell, {@code pixelsToFirst}. We are missing only one last information, how much do we actually see
     * of the first visible cell? We call this amount {@code visibleAmountFirst} and it's given by {@code vPos % totalCellSize}.
     * Finally, the viewport's vertical position is given by {@code -(pixelsToFirst + visibleAmountFist}.
     * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
     * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
     * or bottom anymore.
     * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
     * the cell size and the spacing
     */
    class VerticalHelper<T, C extends VFXCell<T>> extends AbstractHelper<T, C> {

        public VerticalHelper(VFXList<T, C> list) {
            super(list);
            createBindings();
        }

        @Override
        protected void createBindings() {
            range.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getHeight() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalNum();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstVisible() - container.getBufferSize().val());
                    int end = Math.min(container.size() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.heightProperty())
                .addSources(container.bufferSizeProperty())
                .addSources(container.vPosProperty())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.spacingProperty())
                .get()
            );

            viewportPosition.bind(ObjectBindingBuilder.<Position>build()
                .setMapper(() -> {
                    if (container.isEmpty()) return Position.origin();
                    IntegerRange range = range();
                    if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

                    double size = getTotalCellSize();
                    IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
                    double pixelsToFirst = rangeToFirstVisible.diff() * size;
                    double visibleAmountFirst = container.getVPos() % size;

                    double x = -NumberUtils.clamp(container.getHPos(), 0.0, getMaxHScroll());
                    double y = -(pixelsToFirst + visibleAmountFirst);
                    return Position.of(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.hPosProperty(), container.vPosProperty())
                .addSources(container.cellSizeProperty(), container.spacingProperty())
                .get()
            );
            super.createBindings();
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return null; // null for vertical!
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> container.size() * getTotalCellSize() - container.getSpacing())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.spacingProperty())
                .get();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code vPos / totalCellSize}, clamped between 0 and itemsNum - 1.
         */
        @Override
        public int firstVisible() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getVPos() / getTotalCellSize()),
                0,
                container.size() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code (vPos + listHeight) / totalCellSize}, clamped between 0 and itemsNum - 1.
         */
        @Override
        public int lastVisible() {
            return NumberUtils.clamp(
                (int) Math.floor((container.getVPos() + container.getHeight()) / getTotalCellSize()),
                0,
                container.size() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(listHeight / totalCellSize)}.
         */
        @Override
        public int visibleNum() {
            double size = getTotalCellSize();
            return size > 0 ?
                (int) Math.ceil(container.getHeight() / size) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * If {@link VFXList#fitToViewportProperty()} is true, then the computation will always return the
         * list's width, otherwise the node width is computed by {@link LayoutUtils#boundWidth(Node)}.
         * Also, in the latter case, if the found width is greater than the current max x, then the property
         * {@link #virtualMaxXProperty()} is updated with the new value.
         */
        @Override
        public double computeSize(Node node) {
            boolean fitToViewport = container.isFitToViewport();
            if (fitToViewport) {
                double fW = container.getWidth();
                virtualMaxX.set(fW);
                return fW;
            }
            double nW = LayoutUtils.boundWidth(node);
            if (nW == 0) {
                node.applyCss();
                nW = LayoutUtils.boundWidth(node);
            }
            if (nW > virtualMaxX.get()) virtualMaxX.set(nW);
            return nW;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The x position is 0. The y position is the total cell size multiplied bu the given index. The width is
         * computed by {@link #computeSize(Node)}, and the height is given by the {@link VFXList#cellSizeProperty()}.
         */
        @Override
        public void layout(int layoutIndex, VFXCell<T> cell) {
            Node node = cell.toNode();
            double y = getTotalCellSize() * layoutIndex;
            double w = computeSize(node);
            double h = container.getCellSize();
            cell.beforeLayout();
            node.resizeRelocate(0, y, w, h);
            cell.afterLayout();
        }

        @Override
        public void scrollBy(double pixels) {
            container.setVPos(container.getVPos() + pixels);
        }

        @Override
        public void scrollToPixel(double pixel) {
            container.setVPos(pixel);
        }

        @Override
        public void scrollToIndex(int index) {
            scrollToPixel(getTotalCellSize() * index);
        }

        @Override
        public void dispose() {
            range.unbind();
            viewportPosition.unbind();
            super.dispose();
        }
    }

    /**
     * Concrete implementation of {@link AbstractHelper} for {@link Orientation#HORIZONTAL}. Here the range of items to
     * display and the viewport position are defined as follows:
     * <p> - the range is given by the {@link #firstVisible()} element minus the buffer size ({@link VFXList#bufferSizeProperty()}),
     * cannot be negative; and the sum between this start index and the total number of needed cells given by {@link #totalNum()}, cannot
     * exceed the number of items - 1. It may happen the number of indexes given by the range {@code end - start + 1} is lesser
     * than the total number of cells we need, in such cases, the range start is corrected to be {@code end - needed + 1}.
     * A typical situation for this is when the list position reaches the max scroll.
     * The range computation has the following dependencies: the list's width, the virtual max x, the buffer size and
     * the horizontal position.
     * <p> - the viewport position. This computation is at the core of virtual scrolling. The viewport, which contains the cell,
     * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
     * The vertical position is just the current {@link VFXList#vPosProperty()} but negative. The horizontal
     * position is the one virtualized.
     * First we get the range of items to display and the total cell size given by {@link #getTotalCellSize()}, yes, the
     * spacing also affects the position. Then we compute the range to the first visible cell, which is given by
     * {@code IntegerRange.of(range.getMin(), firstVisible())}, in other words we limit the 'complete' range to the
     * left buffer including the first cell after the buffer. The number of indexes in this newfound range, given by
     * {@link IntegerRange#diff()} is multiplied by the total cell size, this way we found the number of pixels to the
     * first visible cell, {@code pixelsToFirst}. We are missing only one last information, how much do we actually see
     * of the first visible cell? We call this amount {@code visibleAmountFirst} and it's given by {@code vPos % totalCellSize}.
     * Finally, the viewport's vertical position is given by {@code -(pixelsToFirst + visibleAmountFist}.
     * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
     * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the left
     * or right anymore.
     * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
     * the cell size and the spacing
     */
    class HorizontalHelper<T, C extends VFXCell<T>> extends AbstractHelper<T, C> {

        public HorizontalHelper(VFXList<T, C> list) {
            super(list);
            createBindings();
        }

        @Override
        protected void createBindings() {
            range.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getWidth() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalNum();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstVisible() - container.getBufferSize().val());
                    int end = Math.min(container.size() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.widthProperty())
                .addSources(container.bufferSizeProperty())
                .addSources(container.hPosProperty())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.spacingProperty())
                .get()
            );

            viewportPosition.bind(ObjectBindingBuilder.<Position>build()
                .setMapper(() -> {
                    if (container.isEmpty()) return Position.origin();
                    IntegerRange range = range();
                    if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

                    double size = getTotalCellSize();
                    IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
                    double pixelsToFirst = rangeToFirstVisible.diff() * size;
                    double visibleAmountFirst = container.getHPos() % size;

                    double x = -(pixelsToFirst + visibleAmountFirst);
                    double y = -NumberUtils.clamp(container.getVPos(), 0.0, getMaxVScroll());
                    return Position.of(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.hPosProperty(), container.vPosProperty())
                .addSources(container.cellSizeProperty(), container.spacingProperty())
                .get()
            );
            super.createBindings();
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> container.size() * getTotalCellSize() - container.getSpacing())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.spacingProperty())
                .get();
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return null; // null for horizontal!
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code hPos / totalCellSize}, clamped between 0 and itemsNum - 1.
         */
        @Override
        public int firstVisible() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getHPos() / getTotalCellSize()),
                0,
                container.size()
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code (hPos + listWidth) / totalCellSize}, clamped between 0 and itemsNum - 1.
         */
        @Override
        public int lastVisible() {
            return NumberUtils.clamp(
                (int) Math.floor((container.getHPos() + container.getWidth()) / getTotalCellSize()),
                0,
                container.size()
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(listWidth / totalCellSize)}.
         */
        @Override
        public int visibleNum() {
            double size = getTotalCellSize();
            return size > 0 ?
                (int) Math.ceil(container.getWidth() / size) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * If {@link VFXList#fitToViewportProperty()} is true, then the computation will always return the
         * list's height, otherwise the node height is computed by {@link LayoutUtils#boundHeight(Node)}.
         * Also, in the latter case, if the found height is greater than the current max y, then the property
         * {@link #virtualMaxYProperty()} is updated with the new value.
         */
        @Override
        public double computeSize(Node node) {
            boolean fitToViewport = container.isFitToViewport();
            if (fitToViewport) {
                double fH = container.getHeight();
                virtualMaxY.set(fH);
                return fH;
            }
            double nH = LayoutUtils.boundHeight(node);
            if (nH == 0) {
                node.applyCss();
                nH = LayoutUtils.boundHeight(node);
            }
            if (nH > virtualMaxY.get()) virtualMaxY.set(nH);
            return nH;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The y position is 0. The x position is the total cell size multiplied bu the given index. The height is
         * computed by {@link #computeSize(Node)}, and the width is given by the {@link VFXList#cellSizeProperty()}.
         */
        @Override
        public void layout(int layoutIndex, VFXCell<T> cell) {
            Node node = cell.toNode();
            double x = getTotalCellSize() * layoutIndex;
            double w = container.getCellSize();
            double h = computeSize(node);
            cell.beforeLayout();
            node.resizeRelocate(x, 0, w, h);
            cell.afterLayout();
        }

        @Override
        public void scrollBy(double pixels) {
            container.setHPos(container.getHPos() + pixels);
        }

        @Override
        public void scrollToPixel(double pixel) {
            container.setHPos(pixel);
        }

        @Override
        public void scrollToIndex(int index) {
            scrollToPixel(getTotalCellSize() * index);
        }

        @Override
        public void dispose() {
            range.unbind();
            viewportPosition.unbind();
            super.dispose();
        }
    }
}
