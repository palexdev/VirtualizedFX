package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.PositionProperty;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import java.util.Optional;

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
public interface VFXListHelper<T, C extends Cell<T>> {

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
	 * @return the maximum amount of pixels the container can scroll on the vertical direction
	 */
	double maxVScroll();

	/**
	 * @return the maximum amount of pixels the container can scroll on the horizontal direction
	 */
	double maxHScroll();

	/**
	 * Specifies the total number of pixels on the x-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxXProperty();

	/**
	 * @return the total number of pixels on the x-axis.
	 */
	default double getVirtualMaxX() {
		return virtualMaxXProperty().get();
	}

	/**
	 * Specifies the total number of pixels on the y-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxYProperty();

	/**
	 * @return the total number of pixels on the y-axis.
	 */
	default double getVirtualMaxY() {
		return virtualMaxYProperty().get();
	}

	/**
	 * Cells are actually contained in a separate pane called 'viewport'. The scroll is applied on this pane.
	 * <p>
	 * This property specifies the translation of the viewport, the calculation depends on the implementation.
	 */
	ReadOnlyObjectProperty<Position> viewportPositionProperty();

	/**
	 * @return the position the viewport should be at in the container
	 */
	default Position getViewportPosition() {
		return viewportPositionProperty().get();
	}

	/**
	 * Computes the width or height of the cell depending on the container's orientation.
	 * <p> - VERTICAL -> width
	 * <p> - HORIZONTAL -> height
	 */
	double computeSize(Node node);

	/**
	 * Lays out the given node. The index parameter is necessary to identify the position of a cell compared to the others
	 * (comes before or after).
	 *
	 * @param absIndex the absolute index of the given node/cell, see {@link VFXListSkin#layout()}
	 */
	void layout(int absIndex, Node node);


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
	 * @return the {@link VFXList} instance associated to this helper
	 */
	VFXList<T, C> getList();

	/**
	 * Forces the {@link VFXList#vPosProperty()} and {@link VFXList#hPosProperty()} to be invalidated.
	 * This is simply done by calling the respective setters with their current respective values. Those two properties
	 * will automatically call {@link #maxVScroll()} and {@link #maxHScroll()} to ensure the values are correct. This is
	 * automatically invoked by the {@link VFXListManager} when needed.
	 */
	default void invalidatePos() {
		VFXList<T, C> list = getList();
		list.setVPos(list.getVPos());
		list.setHPos(list.getHPos());
	}

	/**
	 * Converts the given index to an item (shortcut for {@code getList().getItems().get(index)}).
	 */
	default T indexToItem(int index) {
		return getList().getItems().get(index);
	}

	/**
	 * Converts the given index to a cell. Uses {@link #itemToCell(Object)}.
	 */
	default C indexToCell(int index) {
		T item = indexToItem(index);
		return itemToCell(item);
	}

	/**
	 * Converts the given item to a cell. The result is either on of the cells cached in {@link VFXCellsCache} that
	 * is updated with the given item, or a totally new one created by the {@link VFXList#cellFactoryProperty()}.
	 */
	default C itemToCell(T item) {
		VFXCellsCache<T, C> cache = getList().getCache();
		Optional<C> opt = cache.tryTake();
		opt.ifPresent(c -> c.updateItem(item));
		return opt.orElseGet(() -> getList().getCellFactory().apply(item));
	}

	/**
	 * Implementing the {@link VFXList#spacingProperty()} has been incredibly easy. It is enough to think at the
	 * spacing as an extension of the {@link VFXList#cellSizeProperty()}. In other words, for the helper to still
	 * produce valid ranges, it is enough to sum the spacing to the cell size when the latter is needed.
	 * This is a shortcut for {@code getList().getCellSize() + getList().getSpacing()}.
	 */
	default double getTotalCellSize() {
		return getList().getCellSize() + getList().getSpacing();
	}

	/**
	 * Automatically called by {@link VFXList} when a helper is not needed anymore (changed).
	 * If the helper uses listeners/bindings that may lead to memory leaks, this is the right place to remove them.
	 */
	default void dispose() {}

	/**
	 * Abstract implementation of {@link VFXListHelper}, contains common members for the two concrete implementations
	 * {@link VerticalHelper} and {@link HorizontalHelper}, such as:
	 * <p> - the range of items to display as a {@link IntegerRangeProperty}
	 * <p> - the virtual max x as a {@link ReadOnlyDoubleWrapper}
	 * <p> - the virtual max y as a {@link ReadOnlyDoubleWrapper}
	 * <p> - the viewport's position, {@link #viewportPositionProperty()} as a {@link PositionProperty}
	 */
	abstract class AbstractHelper<T, C extends Cell<T>> implements VFXListHelper<T, C> {
		protected final VFXList<T, C> list;
		protected final IntegerRangeProperty range = new IntegerRangeProperty();
		protected final ReadOnlyDoubleWrapper virtualMaxX = new ReadOnlyDoubleWrapper();
		protected final ReadOnlyDoubleWrapper virtualMaxY = new ReadOnlyDoubleWrapper();
		protected final PositionProperty viewportPosition = new PositionProperty();

		public AbstractHelper(VFXList<T, C> list) {
			this.list = list;
		}

		@Override
		public int totalNum() {
			int visible = visibleNum();
			return visible == 0 ? 0 : Math.min(visible + list.getBufferSize().val() * 2, list.size());
		}

		@Override
		public IntegerRangeProperty rangeProperty() {
			return range;
		}

		@Override
		public ReadOnlyDoubleProperty virtualMaxXProperty() {
			return virtualMaxX.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyDoubleProperty virtualMaxYProperty() {
			return virtualMaxY.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyObjectProperty<Position> viewportPositionProperty() {
			return viewportPosition.getReadOnlyProperty();
		}

		@Override
		public VFXList<T, C> getList() {
			return list;
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
	class VerticalHelper<T, C extends Cell<T>> extends AbstractHelper<T, C> {

		public VerticalHelper(VFXList<T, C> list) {
			super(list);
			virtualMaxY.bind(DoubleBindingBuilder.build()
				.setMapper(() -> (list.size() * getTotalCellSize() - list.getSpacing()))
				.addSources(list.sizeProperty(), list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
			range.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					if (list.getHeight() <= 0) return Utils.INVALID_RANGE;
					int needed = totalNum();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstVisible() - list.getBufferSize().val());
					int end = Math.min(list.size() - 1, start + needed - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(list.heightProperty())
				.addSources(list.bufferSizeProperty())
				.addSources(list.vPosProperty())
				.addSources(list.sizeProperty(), list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
			viewportPosition.bind(ObjectBindingBuilder.<Position>build()
				.setMapper(() -> {
					if (list.isEmpty()) return Position.origin();
					IntegerRange range = range();
					if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

					double size = getTotalCellSize();
					IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
					double pixelsToFirst = rangeToFirstVisible.diff() * size;
					double visibleAmountFirst = list.getVPos() % size;

					double x = -NumberUtils.clamp(list.getHPos(), 0.0, maxHScroll());
					double y = -(pixelsToFirst + visibleAmountFirst);
					return Position.of(x, y);
				})
				.addSources(list.hPosProperty(), list.vPosProperty())
				.addSources(list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code vPos / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
				(int) Math.floor(list.getVPos() / getTotalCellSize()),
				0,
				list.size() - 1
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
				(int) Math.floor((list.getVPos() + list.getHeight()) / getTotalCellSize()),
				0,
				list.size() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(listHeight / totalCellSize)}.
		 */
		@Override
		public int visibleNum() {
			return (int) Math.ceil(list.getHeight() / getTotalCellSize());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxY - listHeight}, cannot be negative
		 */
		@Override
		public double maxVScroll() {
			return Math.max(0, getVirtualMaxY() - list.getHeight());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxX - listWidth}, cannot be negative
		 */
		@Override
		public double maxHScroll() {
			return Math.max(0, getVirtualMaxX() - list.getWidth());
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
			boolean fitToViewport = list.isFitToViewport();
			if (fitToViewport) {
				double fW = list.getWidth();
				virtualMaxX.set(fW);
				return fW;
			}
			double nW = LayoutUtils.boundWidth(node);
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
		public void layout(int absIndex, Node node) {
			double y = getTotalCellSize() * absIndex;
			double w = computeSize(node);
			double h = list.getCellSize();
			node.resizeRelocate(0, y, w, h);
		}

		@Override
		public void scrollBy(double pixels) {
			list.setVPos(list.getVPos() + pixels);
		}

		@Override
		public void scrollToPixel(double pixel) {
			list.setVPos(pixel);
		}

		@Override
		public void scrollToIndex(int index) {
			scrollToPixel(getTotalCellSize() * index);
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
	class HorizontalHelper<T, C extends Cell<T>> extends AbstractHelper<T, C> {

		public HorizontalHelper(VFXList<T, C> list) {
			super(list);
			virtualMaxX.bind(DoubleBindingBuilder.build()
				.setMapper(() -> (list.size() * getTotalCellSize() - list.getSpacing()))
				.addSources(list.sizeProperty(), list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
			range.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					if (list.getWidth() <= 0) return Utils.INVALID_RANGE;
					int needed = totalNum();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstVisible() - list.getBufferSize().val());
					int end = Math.min(list.size() - 1, start + needed - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(list.widthProperty())
				.addSources(list.bufferSizeProperty())
				.addSources(list.hPosProperty())
				.addSources(list.sizeProperty(), list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
			viewportPosition.bind(ObjectBindingBuilder.<Position>build()
				.setMapper(() -> {
					if (list.isEmpty()) return Position.origin();
					IntegerRange range = range();
					if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

					double size = getTotalCellSize();
					IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
					double pixelsToFirst = rangeToFirstVisible.diff() * size;
					double visibleAmountFirst = list.getHPos() % size;

					double x = -NumberUtils.clamp(list.getVPos(), 0.0, maxVScroll());
					double y = -(pixelsToFirst + visibleAmountFirst);
					return Position.of(x, y);
				})
				.addSources(list.hPosProperty(), list.vPosProperty())
				.addSources(list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code hPos / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
				(int) Math.floor(list.getHPos() / getTotalCellSize()),
				0,
				list.size()
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
				(int) Math.floor((list.getHPos() + list.getWidth()) / getTotalCellSize()),
				0,
				list.size()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(listWidth / totalCellSize)}.
		 */
		@Override
		public int visibleNum() {
			return (int) Math.ceil(list.getWidth() / getTotalCellSize());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxY - listHeight}, cannot be negative
		 */
		@Override
		public double maxVScroll() {
			return Math.max(0, getVirtualMaxY() - list.getHeight());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxX - listWidth}, cannot be negative
		 */
		@Override
		public double maxHScroll() {
			return Math.max(0, getVirtualMaxX() - list.getWidth());
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
			boolean fitToViewport = list.isFitToViewport();
			if (fitToViewport) {
				double fH = list.getHeight();
				virtualMaxY.set(fH);
				return fH;
			}
			double nH = LayoutUtils.boundHeight(node);
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
		public void layout(int absIndex, Node node) {
			double x = getTotalCellSize() * absIndex;
			double w = list.getCellSize();
			double h = computeSize(node);
			node.resizeRelocate(x, 0, w, h);
		}

		@Override
		public void scrollBy(double pixels) {
			list.setHPos(list.getHPos() + pixels);
		}

		@Override
		public void scrollToPixel(double pixel) {
			list.setHPos(pixel);
		}

		@Override
		public void scrollToIndex(int index) {
			scrollToPixel(getTotalCellSize() * index);
		}
	}
}
